/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.dtd;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.*;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.ent.*;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.DefaultInputResolver;
import com.ctc.wstx.io.InputSourceFactory;
import com.ctc.wstx.io.WstxInputResolver;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.sr.StreamScanner;
import com.ctc.wstx.util.InternCache;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuffer;
import com.ctc.wstx.util.WordResolver;

/**
 * Reader that reads in DTD information from internal or external subset.
 * It also implements simple stand-alone functionality for flattening
 * DTD files; this is sometimes useful when optimizing modularized DTDs
 * (which are more maintainable) into single monolithic DTDs (which in
 * general can be more performant).
 *<p>
 * There are 2 main modes for DTDReader, depending on whether it is parsing
 * internal or external subset. Parsing of internal subset is somewhat
 * simpler, since no dependency checking is needed. For external subset,
 * handling of parameter entities is bit more complicated, as care has to
 * be taken to distinguish between using PEs defined in int. subset, and
 * ones defined in ext. subset itself. This determines cachability of
 * external subsets.
 */

public class FullDTDReader
    extends MinimalDTDReader
{
    /**
     * Flag that can be changed to enable or disable interning of shared
     * names; shared names are used for enumerated values to reduce
     * memory usage.
     */
    final static boolean INTERN_SHARED_NAMES = false;

    /**
     * Expected maximum length of internal entities; balanced to reduce
     * likelihood of having to grow the array, versus allocating too
     * big chunks.
     */
    final static int EXP_ENTITY_VALUE_LEN = 500;

    /*
    //////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////
     */

    final int mConfigFlags;

    final XMLReporter mReporter;

    // Extracted wstx-specific settings:

    final boolean mCfgNormalizeLFs;

    final boolean mCfgNormAttrs;

    final boolean mCfgValidate;

    final boolean mCfgSupportDTDPP;

    /*
    //////////////////////////////////////////////////
    // Entity handling, parameter entities (PEs)
    //////////////////////////////////////////////////
     */

    /**
     * Set of parameter entities defined so far in the currently parsed
     * subset. Note: the first definition sticks, entities can not be
     * redefined.
     *<p>
     * Keys are entity name Strings; values are instances of EntityDecl
     */
    HashMap mParamEntities;

    /**
     * Set of parameter entities already defined for the subset being
     * parsed; namely, PEs defined in the internal subset passed when
     * parsing matching external subset. Null when parsing internal
     * subset.
     */
    final HashMap mPredefdPEs;

    /**
     * Set of parameter entities (ids) that have been referenced by this
     * DTD; only maintained for external subsets, and only as long as
     * no pre-defined PE has been referenced.
     */
    Set mRefdPEs;

    /*
    //////////////////////////////////////////////////
    // Entity handling, general entities (GEs)
    //////////////////////////////////////////////////
     */

    /**
     * Set of generic entities defined so far in this subset.
     * As with parameter entities, the first definition sticks.
     *<p>
     * Keys are entity name Strings; values are instances of EntityDecl
     *<p>
     * Note: this Map only contains entities declared and defined in the
     * subset being parsed; no previously defined values are passed.
     */
    HashMap mGeneralEntities;

    /**
     * Set of general entities already defined for the subset being
     * parsed; namely, PEs defined in the internal subset passed when
     * parsing matching external subset. Null when parsing internal
     * subset. Such entities are only needed directly for one purpose;
     * to be expanded when reading attribute default value definitions.
     */
    final HashMap mPredefdGEs;

    /**
     * Set of general entities (ids) that have been referenced by this
     * DTD; only maintained for external subsets, and only as long as
     * no pre-defined GEs have been referenced.
     */
    Set mRefdGEs;

    /*
    //////////////////////////////////////////////////
    // Entity handling, both PEs and GEs
    //////////////////////////////////////////////////
     */

    /**
     * Flag used to keep track of whether current (external) subset
     * has referenced at least one PE that was pre-defined.
     */
    boolean mUsesPredefdEntities = false;

    /*
    //////////////////////////////////////////////////
    // Notation settings
    //////////////////////////////////////////////////
     */

    /**
     * Set of notations defined so far. Since it's illegal to (try to)
     * redefine notations, there's no specific precedence.
     *<p>
     * Keys are entity name Strings; values are instances of
     * NotationDecl objects
     */
    HashMap mNotations;

    /**
     * Notations already parsed before current subset; that is,
     * notations from the internal subset if we are currently
     * parsing matching external subset.
     */
    final HashMap mPredefdNotations;

    /**
     * Flag used to keep track of whether current (external) subset
     * has referenced at least one notation that was defined in internal
     * subset. If so, can not cache the external subset
     */
    boolean mUsesPredefdNotations = false;

    /*
    //////////////////////////////////////////////////
    // Element specifications
    //////////////////////////////////////////////////
     */

    /**
     * Map used to shared NameKey instances, to reduce memory usage
     * of (qualified) element and attribute names
     */
    HashMap mSharedNames = null;

    /**
     * Contains definition of elements and matching content specifications.
     * Also contains temporary placeholders for elements that are indirectly
     * "created" by ATTLIST declarations that precede actual declaration
     * for the ELEMENT referred to.
     */
    HashMap mElements;

    /**
     * Map used for sharing legal enumeration values; used since oftentimes
     * same enumeration values are used with multiple attributes
     */
    HashMap mSharedEnumValues = null;

    /*
    //////////////////////////////////////////////////
    // Reader state
    //////////////////////////////////////////////////
     */

    /**
     * Nesting count for conditionally included sections; 0 means that
     * we are not inside such a section. Note that condition ignore is
     * handled separately.
     */
    int mIncludeCount = 0;

    /*
    //////////////////////////////////////////////////
    // DTD++ support information
    //////////////////////////////////////////////////
     */

    /**
     * Flag that indicates if any DTD++ features have been encountered
     * (in DTD++-supporting mode).
     */
    boolean mAnyDTDppFeatures = false;

    /**
     * Currently active default namespace URI.
     */
    String mDefaultNsURI = "";

    /**
     * Prefix-to-NsURI mappings for this DTD, if any: lazily
     * constructed when needed
     */
    HashMap mNamespaces = null;

    /*
    //////////////////////////////////////////////////
    // Additional support for creating expanded output
    // of processed DTD.
    //////////////////////////////////////////////////
     */

    DTDWriter mFlattenWriter = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    /**
     * Constructor used for reading/skipping internal subset.
     */
    private FullDTDReader(StreamScanner master, WstxInputSource input,
                          ReaderConfig cfg)
    {
        this(input, cfg, master, false, null);
    }

    /**
     * Constructor used for reading external subset.
     */
    private FullDTDReader(StreamScanner master, WstxInputSource input,
                          ReaderConfig cfg,  DTDSubset intSubset)
    {
        this(input, cfg, master, true, intSubset);

        // Let's make sure line/col offsets are correct...
        input.initInputLocation(this);
    }

    /**
     * Common initialization part of int/ext subset constructors.
     */
    private FullDTDReader(WstxInputSource input, ReaderConfig cfg,
                          StreamScanner master, boolean isExt,
                          DTDSubset intSubset)
    {
        super(input, cfg, master, isExt);
        mReporter = cfg.getXMLReporter();
        int cfgFlags = cfg.getConfigFlags();
        mConfigFlags = cfgFlags;
        mCfgNormalizeLFs = (cfgFlags & CFG_NORMALIZE_LFS) != 0;
        mCfgNormAttrs = (cfgFlags & CFG_NORMALIZE_ATTR_VALUES) != 0;
        mCfgValidate = (cfgFlags & CFG_VALIDATE_AGAINST_DTD) != 0;
        mCfgSupportDTDPP = (cfgFlags & CFG_SUPPORT_DTDPP) != 0;
        mUsesPredefdEntities = false;
        mParamEntities = null;
        mRefdPEs = null;
        mRefdGEs = null;
        mGeneralEntities = null;

        // Did we get any existing parameter entities?
        HashMap pes = (intSubset == null) ?
            null : intSubset.getParameterEntityMap();
        if (pes == null || pes.isEmpty()) {
            mPredefdPEs = null;
        } else {
            mPredefdPEs = pes;
        }

        // How about general entities (needed only for attr. def. values)
        HashMap ges = (intSubset == null) ?
            null : intSubset.getGeneralEntityMap();
        if (ges == null || ges.isEmpty()) {
            mPredefdGEs = null;
        } else {
            mPredefdGEs = ges;
        }

        // And finally, notations
        HashMap not = (intSubset == null) ?
            null : intSubset.getNotationMap();
        if (not == null || ges.isEmpty()) {
            mPredefdNotations = null;
        } else {
            mPredefdNotations = not;
        }
    }

    /**
     * Method called to read in the internal subset definition.
     */
    public static DTDSubset readInternalSubset(StreamScanner master, WstxInputSource input,
                                               ReaderConfig cfg)
        throws IOException, XMLStreamException
    {
        FullDTDReader r = new FullDTDReader(master, input, cfg);
        // Parser should reuse master's input buffers:
        r.copyBufferStateFrom(master);
        DTDSubset ss;

        try {
            ss = r.parseDTD();
        } finally {
            /* And then need to restore changes back to master (line nrs etc);
             * effectively means that we'll stop reading external DTD subset,
             * if so.
             */
            master.copyBufferStateFrom(r);
        }
        return ss;
    }

    /**
     * Method called to read in the external subset definition.
     */
    public static DTDSubset readExternalSubset
        (StreamScanner master, WstxInputSource src, ReaderConfig cfg,
         DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        FullDTDReader r = new FullDTDReader(master, src, cfg, intSubset);
        return r.parseDTD();
    }

    /**
     * Method that will parse, process and output contents of an external
     * DTD subset. It will do processing similar to
     * {@link #readExternalSubset}, but additionally will copy its processed
     * ("flattened") input to specified writer.
     *
     * @param src Input source used to read the main external subset
     * @param flattenWriter Writer to output processed DTD content to
     * @param inclComments If true, will pass comments to the writer; if false,
     *   will strip comments out
     * @param inclConditionals If true, will include conditional block markers,
     *   as well as intervening content; if false, will strip out both markers
     *   and ignorable sections.
     * @param inclPEs If true, will output parameter entity declarations; if
     *   false will parse and use them, but not output.
     */
    public static DTDSubset flattenExternalSubset(WstxInputSource src, Writer flattenWriter,
                                                  boolean inclComments, boolean inclConditionals,
                                                  boolean inclPEs)
        throws IOException, XMLStreamException
    {
        int configFlags = -1; // let's start with all options set, first

        /* null -> DTDReaderProxy to use -- since we are not using stream
         *    reader,  need not pass valid value.
         */
        ReaderConfig cfg = ReaderConfig.createFullDefaults(new SymbolTable(), null);
        /* Let's actually not normalize LFs; it's likely caller wouldn't
         * really want any such changes....
         */
        cfg.clearConfigFlag(CFG_NORMALIZE_LFS);
        cfg.clearConfigFlag(CFG_NORMALIZE_ATTR_VALUES);

        // null -> no master
        FullDTDReader r = new FullDTDReader(null, src, cfg, null);
        r.setFlattenWriter(flattenWriter, inclComments, inclConditionals,
                           inclPEs);
        DTDSubset ss = r.parseDTD();
        r.flushFlattenWriter();
        flattenWriter.flush();
        return ss;
    }

    /*
    //////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////
     */

    /**
     * Method that will set specified Writer as the 'flattening writer';
     * writer used to output flattened version of DTD read in. This is
     * similar to running a C-preprocessor on C-sources, except that
     * defining writer will not prevent normal parsing of DTD itself.
     */
    public void setFlattenWriter(Writer w, boolean inclComments,
                                 boolean inclConditionals, boolean inclPEs)
    {
        mFlattenWriter = new DTDWriter(w, inclComments, inclConditionals,
                                       inclPEs);
    }

    private void flushFlattenWriter()
        throws IOException
    {
        mFlattenWriter.flush(mInputBuffer, mInputPtr);
    }

    /*
    //////////////////////////////////////////////////
    // Internal API
    //////////////////////////////////////////////////
     */

    /**
     * Method that may need to be called by attribute default value
     * validation code, during parsing....
     *<p>
     * Note: see base class for some additional remarks about this
     * method.
     */
    public EntityDecl findEntity(String entName)
    {
        if (mPredefdGEs != null) {
            EntityDecl decl = (EntityDecl) mPredefdGEs.get(entName);
            if (decl != null) {
                return decl;
            }
        }
        return (EntityDecl) mGeneralEntities.get(entName);
    }

    /*
    //////////////////////////////////////////////////
    // Main-level parsing methods
    //////////////////////////////////////////////////
     */

    protected DTDSubset parseDTD()
        throws IOException, XMLStreamException
    {
        while (true) {
            int i = getNextAfterWS();
            if (i < 0) {
                if (mIsExternal) { // ok for external DTDs
                    break;
                }
                // Error for internal subset
                throwUnexpectedEOF(SUFFIX_IN_DTD_INTERNAL);
            }

            if (i == '%') { // parameter entity
                expandPE();
                continue;
            }

            /* First, let's keep track of start of the directive; needed for
             * entity and notation declaration events.
             */
            mTokenInputTotal = mCurrInputProcessed + mInputPtr;
            mTokenInputRow = mCurrInputRow;
            mTokenInputCol = mInputPtr - mCurrInputRowStart;

            if (i == '<') {
                if (mFlattenWriter == null) {
                    parseDirective();
                } else {
                    parseDirectiveFlattened();
                }
                continue;
            }

            if (i == ']') {
                if (mIncludeCount > 0) { // active INCLUDE block(s) open?
                    boolean suppress = (mFlattenWriter != null) && !mFlattenWriter.includeConditionals();

                    if (suppress) {
                        mFlattenWriter.flush(mInputBuffer, mInputPtr-1);
                        mFlattenWriter.disableOutput();
                    }

                    try {
                        char c = getNextExpanded();
                        if (c != ']' 
                            || (c = getNextExpanded()) != '>') {
                            throwDTDUnexpectedChar(c, "; expected ']]>' to close conditional include section.");
                        }
                    } finally {
                        if (suppress) {
                            mFlattenWriter.enableOutput(mInputPtr);
                        }
                    }

                    // Ok, fine, conditional include section ended.
                    --mIncludeCount;
                    continue;
                }
                if (!mIsExternal) {
                    // End of internal subset
                    break;
                }
            }

            if (mIsExternal) {
                throwDTDUnexpectedChar(i, SUFFIX_IN_DTD_EXTERNAL+"; expected a '<' to start a directive.");
            }
            throwDTDUnexpectedChar(i, SUFFIX_IN_DTD_INTERNAL+"; expected a '<' to start a directive, or \"]>\" to end internal subset.");
        }

        // Ok; time to construct and return DTD data object.
        DTDSubset ss;

        // There are more settings for ext. subsets:
        if (mIsExternal) {
            /* External subsets are cachable if they did not refer to any
             * PEs or GEs defined in internal subset passed in (if any),
             * nor to any notations.
             * We don't care about PEs it defined itself, but need to pass
             * in Set of PEs it refers to, to check if cached copy can be
             * used with different int. subsets.
             * We need not worry about notations referred, since they are
             * not allowed to be re-defined.
             */
            boolean cachable = !mUsesPredefdEntities && !mUsesPredefdNotations;
            ss = DTDSubsetImpl.constructInstance(cachable,
                                                 mGeneralEntities, mRefdGEs,
                                                 null, mRefdPEs,
                                                 mNotations, mElements);
        } else {
            /* Internal subsets are not cachable (no unique way to refer
             * to unique internal subsets), and there can be no references
             * to pre-defined PEs, as none were passed.
             */
            ss = DTDSubsetImpl.constructInstance(false, mGeneralEntities, null,
                                                 mParamEntities, null,
                                                 mNotations, mElements);
        }

        return ss;
    }

    protected void parseDirective()
        throws IOException, XMLStreamException
    {
        /* Let's determine type here, and call appropriate skip/parse
         * methods.
         */
        char c = getNextExpanded();
        /* 11-Jul-2004, TSa: DTDs are not defined to have processing
         *   instructions... but let's allow them, for now?
         */
        if (c == '?') { // xml decl?
            skipPI();
            //throwDTDUnexpectedChar(c, " expected '!' to start a directive.");
            return;
        }
        if (c != '!') { // nothing valid
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected '!' to start a directive");
        }

        // ignore/include, comment, or directive

        c = getNextExpanded();
        if (c == '-') { // plain comment
            c = getNextExpanded();
            if (c != '-') {
                throwDTDUnexpectedChar(c, getErrorMsg()+"; expected '-' for a comment.");
            }
            skipComment();
        } else if (c == '[') {
            checkInclusion();
        } else if (c >= 'A' && c <= 'Z') {
            handleDeclaration(c);
        } else {
            throwDTDUnexpectedChar(c, getErrorMsg()+ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
        }
    }

    /**
     * Method similar to {@link #parseDirective}, but one that takes care
     * to properly output dtd contents via {@link com.ctc.wstx.dtd.DTDWriter}
     * as necessary.
     * Separated to simplify both methods; otherwise would end up with
     * 'if (... flatten...) ... else ...' spaghetti code.
     */
    protected void parseDirectiveFlattened()
        throws IOException, XMLStreamException
    {
        /* First, need to flush any flattened output there may be, at
         * this point (except for opening lt char): and then need to
         * temporarily disable more output until we know the type and
         * whether it should be output or not:
         */
        mFlattenWriter.flush(mInputBuffer, mInputPtr-1);
        mFlattenWriter.disableOutput();

        /* Let's determine type here, and call appropriate skip/parse
         * methods.
         */
        char c = getNextExpanded();
        /* 11-Jul-2004, TSa: DTDs are not defined to have processing
         *   instructions... but let's allow them, for now?
         */
        if (c == '?') { // xml decl?
            mFlattenWriter.enableOutput(mInputPtr);
            mFlattenWriter.output("<?");
            skipPI();
            //throwDTDUnexpectedChar(c, " expected '!' to start a directive.");
            return;
        }
        if (c != '!') { // nothing valid
            throwDTDUnexpectedChar(c, getErrorMsg()+ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
        }

        // ignore/include, comment, or directive

        c = getNextExpanded();
        if (c == '-') { // plain comment
            c = getNextExpanded();
            if (c != '-') {
                throwDTDUnexpectedChar(c, getErrorMsg()+"; expected '-' for a comment.");
            }
            boolean comm = mFlattenWriter.includeComments();
            if (comm) {
                mFlattenWriter.enableOutput(mInputPtr);
                mFlattenWriter.output("<!--");
            }

            try {
                skipComment();
            } finally {
                if (!comm) {
                    mFlattenWriter.enableOutput(mInputPtr);
                }
            }
        } else {
            if (c == '[') {
                boolean cond = mFlattenWriter.includeConditionals();
                if (cond) {
                    mFlattenWriter.enableOutput(mInputPtr);
                    mFlattenWriter.output("<![");
                }
                try {
                    checkInclusion();
                } finally {
                    if (!cond) {
                        mFlattenWriter.enableOutput(mInputPtr);
                    }
                }
            } else {
                /* 12-Jul-2004, TSa: Do we need to see if we have to suppress
                 *    a PE declaration?
                 */
                boolean filterPEs = (c == 'E') && !mFlattenWriter.includeParamEntities();
                if (filterPEs) {
                    handleSuppressedDeclaration();
                } else if (c >= 'A' && c <= 'Z') {
                    mFlattenWriter.enableOutput(mInputPtr);
                    mFlattenWriter.output("<!");
                    mFlattenWriter.output(c);
                    handleDeclaration(c);
                } else {
                    throwDTDUnexpectedChar(c, getErrorMsg()+ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
                }
            }
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Overrided input handling 
    ////////////////////////////////////////////////////
     */

    protected void initInputSource(WstxInputSource newInput, boolean isExt)
        throws IOException, XMLStreamException
    {
        if (mFlattenWriter != null) {
            // Anything to flush from previous buffer contents?
            mFlattenWriter.flush(mInputBuffer, mInputPtr);
            mFlattenWriter.disableOutput();
            try {
                /* Then let's let base class do the 'real' input source setup;
                 * this includes skipping of optional XML declaration that we
                 * do NOT want to output
                 */
                super.initInputSource(newInput, isExt);
            } finally {
                // This will effectively skip declaration
                mFlattenWriter.enableOutput(mInputPtr);
            }
        } else {
            super.initInputSource(newInput, isExt);
        }
    }

    /**
     * Need to override this method, to check couple of things: first,
     * that nested input sources are balanced, when expanding parameter
     * entities inside entity value definitions (as per XML specs), and
     * secondly, to handle (optional) flattening output.
     */
    protected boolean loadMore()
        throws IOException, WstxException
    {
        WstxInputSource input = mInput;

        // Any flattened not-yet-output input to flush?
        if (mFlattenWriter != null) {
            /* Note: can not trust mInputPtr; may not be correct. End of
             * input should be, though.
             */
            mFlattenWriter.flush(mInputBuffer, mInputLen);
        }

        do {
            /* Need to make sure offsets are properly updated for error
             * reporting purposes, and do this now while previous amounts
             * are still known.
             */
            mCurrInputProcessed += mInputLen;
            mCurrInputRowStart -= mInputLen;
            int count = input.readInto(this);
            if (count > 0) {
                if (mFlattenWriter != null) {
                    mFlattenWriter.setFlattenStart(mInputPtr);
                }
                return true;
            }
            input.close();
            if (input == mRootInput) {
                return false;
            }
            WstxInputSource parent = input.getParent();
            if (parent == null) { // sanity check!
                throw new Error("Internal error: null parent for input source '"
                                +input+"'; should never occur (should have stopped at root input '"+mRootInput+"'.");
            }

            mInput = input = parent;
            input.restoreContext(this);
            if (mFlattenWriter != null) {
                mFlattenWriter.setFlattenStart(mInputPtr);
            }
            // Maybe there are leftovers from that input in buffer now?
        } while (mInputPtr >= mInputLen);

        return true;
    }

    protected boolean loadMoreFromCurrent()
        throws IOException, WstxException
    {
        // Any flattened not-yet-output input to flush?
        if (mFlattenWriter != null) {
            mFlattenWriter.flush(mInputBuffer, mInputLen);
        }

        // Need to update offsets properly
        mCurrInputProcessed += mInputLen;
        mCurrInputRowStart -= mInputLen;
        int count = mInput.readInto(this);
        if (count > 0) {
            if (mFlattenWriter != null) {
                mFlattenWriter.setFlattenStart(mInputPtr);
            }
            return true;
        }
        return false;
    }

    protected boolean ensureInput(int minAmount)
        throws IOException
    {
        int currAmount = mInputLen - mInputPtr;
        if (currAmount >= minAmount) {
            return true;
        }
        // Any flattened not-yet-output input to flush?
        if (mFlattenWriter != null) {
            mFlattenWriter.flush(mInputBuffer, mInputLen);
        }
        if (mInput.readMore(this, minAmount)) {
            if (mFlattenWriter != null) {
                //mFlattenWriter.setFlattenStart(mInputPtr);
                mFlattenWriter.setFlattenStart(currAmount);
            }
            return true;
        }
        return false;
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, input access:
    //////////////////////////////////////////////////
     */

    /**
     * Method that will get next character, and either return it as is (for
     * normal chars), or expand parameter entity that starts with next
     * character (which has to be '%').
     */
    private char getNextExpanded()
        throws IOException, XMLStreamException
    {
        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c != '%') {
                return c;
            }
            expandPE();
        }
    }

    /**
     * Similar to {@link #getNextExpanded}, but additionally allows for inlined
     * '--' comments inside declaration (entity, attlist, notation), as per
     * SGML (which _maybe_ is also legal in DTDs?)
     */
    private char getNextExpandedInDecl()
        throws IOException, XMLStreamException
    {
        main_loop:
        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '-') {
              char d = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
              // Inline comment?
              if (d == '-') {
                skipCommentContent();
                continue main_loop;
              }
                // Nah, let's push second one back, and return hyphen:
                --mInputPtr; // pushback
                return c;
              }

            if (c != '%') {
                return c;
            }
            expandPE();
        }
    }

    private char skipDtdWs(boolean allowComments)
        throws IOException, XMLStreamException
    {
        while (true) {
            char c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '%') {
                expandPE();
                continue;
            }
            if (c > CHAR_SPACE) {
              if (c == '-' && allowComments) {
                c = (mInputPtr < mInputLen)
                  ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                if (c == '-') {
                  skipCommentContent();
                  continue;
                }
                // Nah, need to return the hyphen, let's fall through:
                --mInputPtr;
              }
              return c;
            }
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            }
        }
    }

    private char skipObligatoryDtdWs(boolean allowComments)
        throws IOException, XMLStreamException
    {
        char c = CHAR_NULL;
        int count = 0;
        
        while (true) {
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == '%') {
                expandPE();
                continue;
            }
            if (c > CHAR_SPACE) {
                if (c == '-' && allowComments) {
                    c = (mInputPtr < mInputLen)
                        ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                    if (c == '-') {
                        skipCommentContent();
                        continue;
                    }
                    // Nah, need to return the hyphen, let's fall through:
                    --mInputPtr;
                }
                break;
            }
            ++count;
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            }
        }
        
        if (count == 0) {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a separating white space.");
        }
        
        return c;
    }

    /**
     * Method called to handle expansion of parameter entities. When called,
     * '%' character has been encountered as a reference indicator, and
     * now we should get parameter entity name.
     */
    private void expandPE()
        throws IOException, XMLStreamException
    {

        String id;
        char c;

        // 01-Jul-2004, TSa: When flattening, need to flush previous output
        if (mFlattenWriter != null) {
            // Flush up to but not including ampersand...
            mFlattenWriter.flush(mInputBuffer, mInputPtr-1);
            mFlattenWriter.disableOutput();
            id = readDTDName();
            try {
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            } finally {
                // will ignore name and colon (or whatever was parsed)
                mFlattenWriter.enableOutput(mInputPtr);
            }
        } else {
            id = readDTDName();
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
        }
        
        // Should now get semicolon...
        if (c != ';') {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected ';' to end parameter entity name.");
        }

        if (mIsExternal) {
            /* Need more checking when expanding PEs for external subsets;
             * need to see if definition was pre-defined or locally
             * defined.
             */
            int setId = expandEntity(id, mPredefdPEs, mParamEntities, true);
            if (setId == 1) { // came from internal subset...
                mUsesPredefdEntities = true;
                /* No need to further keep track of internal references,
                 * since this subset can not be cached, so let's just free
                 * up Map if it has been created
                 */
                mRefdPEs = null;
            } else {
                // Ok, just need to mark reference, if we still care:
                if (!mUsesPredefdEntities) {
                    // Let's also mark down the fact we referenced the entity:
                    Set used = mRefdPEs;
                    if (used == null) {
                        mRefdPEs = used = new HashSet();
                    }
                    used.add(id);
                }
            }
        } else {
            expandEntity(id, mParamEntities, null, true);
        }
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, low-level parsing:
    //////////////////////////////////////////////////
     */

    /**
     * Method called to verify whether input has specified keyword; if it
     * has, returns null and points to char after the keyword; if not, returns
     * whatever constitutes a keyword matched, for error reporting purposes.
     */
    protected String checkDTDKeyword(String exp)
        throws IOException, XMLStreamException
    {
        int i = 0;
        int len = exp.length();
        char c = ' ';

        for (; i < len; ++i) {
            while (true) { // inlined getNextExpanded()
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                if (c != '%') {
                    break;
                }
                expandPE();
            }
            if (c != exp.charAt(i)) {
                break;
            }
        }

        if (i == len) {
            /* Got a match? Cool... except if identifier still continues...
             */
            c = getNextExpanded();
            --mInputPtr; // ie. we just peek it...
            if (!isNameChar(c)) {
                // Yup, that's fine!
                return null;
            }
            // Nope, need to just fall down to get full 'wrong' keyword
        }

        // Let's first add previous parts of the keyword:
        StringBuffer sb = new StringBuffer(exp.substring(0, i));
        sb.append(c);
        while (true) {
            c = getNextExpanded();
            if (!isNameChar(c) && c != ':') {
                --mInputPtr;
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Method called to verify whether input has specified keyword; if it
     * has, returns null and points to char after the keyword; if not, returns
     * whatever constitutes a keyword matched, for error reporting purposes.
     */
    protected void checkDTDKeyword(String exp, char firstChar, String extraError)
        throws IOException, XMLStreamException
    {
        // First thing, is the first char ok?
        if (firstChar != exp.charAt(0)) {
            if (isNameStartChar(firstChar)) {
                ; // Ok, fine, let's fall to code that gets the identifier
            } else {
                throwDTDUnexpectedChar(firstChar, getErrorMsg()+extraError);
            }
        }

        int i = 1;
        int len = exp.length();

        for (; i < len; ++i) {
            char c;
            while (true) { // inlined getNextExpanded()
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                if (c != '%') {
                    break;
                }
                expandPE();
            }
            if (c != exp.charAt(i)) {
                break;
            }
        }

        if (i == len) {
            // Got a match? Cool... except if identifier still continues...
            char c = getNextExpanded();
            --mInputPtr; // ie. we just peek it...
            if (!isNameChar(c)) {
                // Yup, that's fine!
                return;
            }
            // Nope, need to just fall down to get full 'wrong' keyword
        }
      
        // Let's first add previous parts of the keyword:
        StringBuffer sb = new StringBuffer(exp.substring(0, i));
        while (true) {
            char c = getNextExpanded();
            if (!isNameChar(c) && c != ':') {
                --mInputPtr;
                break;
            }
            sb.append(c);
        }
        throwParseError(getErrorMsg()+extraError);
    }

    /**
     * Method called usually to indicate an error condition; will read rest
     * of specified keyword (including characters that can be part of XML
     * identifiers), append that to passed prefix (which is optional), and
     * return resulting String.
     *
     * @param prefix Part of keyword already read in, if any; may be null
     *    if keyword is just starting.
     */
    protected String readDTDKeyword(String prefix)
        throws IOException, XMLStreamException
    {
        boolean gotPrefix = prefix != null && prefix.length() > 0;
        StringBuffer sb = gotPrefix ?
            new StringBuffer(prefix) : new StringBuffer();

        if (!gotPrefix) {
            char c = getNextExpanded();
            if (!isNameStartChar(c)) { // should never happen...
                --mInputPtr;
                return "";
            }
            sb.append(c);
        }

        while (true) {
            char c = getNextExpanded();
            if (!isNameChar(c) && c != ':') {
                --mInputPtr;
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * @return True, if input contains 'PUBLIC' keyword; false if it
     *   contains 'SYSTEM'; otherwise throws an exception.
     */
    private boolean checkPublicSystemKeyword(char c) 
        throws IOException, XMLStreamException
    {
        String errId;

        if (c == 'P') {
            errId = checkDTDKeyword("UBLIC");
            if (errId == null) {
                return true;
            }
            errId = "P" + errId;
        } else if (c == 'S') {
            errId = checkDTDKeyword("YSTEM");
            if (errId == null) {
                return false;
            }
            errId = "S" + errId;
        } else {
            if (!isNameStartChar(c)) {
                throwDTDUnexpectedChar(c, "; expected 'PUBLIC' or 'SYSTEM' keyword.");
            }
            --mInputPtr;
            errId = readDTDKeyword(null);
        }

        throwParseError("Unrecognized keyword '"+errId+"'; expected 'PUBLIC' or 'SYSTEM'");
        return false; // never gets here
    }

    private String readDTDName()
        throws IOException, WstxException
    {
        return readDTDName(getNextChar(getErrorMsg()));
    }

    private String readDTDName(char c)
        throws IOException, WstxException
    {
        // Let's just check this before trying to parse the id...
        if (!isNameStartChar(c)) {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected an identifier");
        }
        return parseFullName(c);
    }

    private String readDTDLocalName(char c, boolean checkChar)
        throws IOException, WstxException
    {
        /* Let's just check this first, to get better error msg
	 * (parseLocalName() will double-check it too)
	 */
        if (checkChar && !isNameStartChar(c)) {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected an identifier");
        }
        return parseLocalName(c);
    }

    /**
     * Similar to {@link #readDTDName}, except that the rules are bit looser,
     * ie. there are no additional restrictions for the first char
     */
    private String readDTDNmtoken(char c)
        throws IOException, WstxException
    {
        char[] outBuf = getNameBuffer(64);
        int outLen = outBuf.length;
        int outPtr = 0;

        while (true) {
            if (!isNameChar(c)) {
                // Need to get at least one char
                if (outPtr == 0) {
                    throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a NMTOKEN character to start a NMTOKEN");
                }
                --mInputPtr;
                break;
            }
            if (outPtr >= outLen) {
                outBuf = expandBy50Pct(outBuf);
                outLen = outBuf.length;
            }
            outBuf[outPtr++] = c;
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextChar(SUFFIX_IN_NAME);
        }

        /* Nmtokens need not be canonicalized; they will be processed
         * as necessary later on:
         */
        return new String(outBuf, 0, outPtr);
    }

    /**
     * Method that will read an element or attribute name from DTD; depending
     * on namespace mode, it can have prefix as well.
     *<p>
     * Note: returned {@link NameKey} instances are canonicalized so that
     * all instances read during parsing of a single DTD subset so that
     * identity comparison can be used instead of calling <code>equals()</code>
     * method (but only within a single subset!). This also reduces memory
     * usage to some extent.
     */
    private NameKey readDTDQName(char firstChar)
        throws IOException, XMLStreamException
    {
        String prefix, localName;

        if (!mCfgNsEnabled) {
            prefix = null;
            localName = parseFullName(firstChar);
        } else {
            localName = parseLocalName(firstChar);
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c == ':') { // Ok, got namespace and local name
                prefix = localName;
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
                localName = parseLocalName(c);
            } else {
                --mInputPtr; // pushback
                prefix = null;
            }
        }

        return findSharedName(prefix, localName);
    }

    private char readArity()
        throws IOException, XMLStreamException
    {
        char c = (mInputPtr < mInputLen) ?
            mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
        if (c == '?' || c == '*' || c == '+') {
            return c;
        }
        // Hmmh, not recognized, let's put it back:
        --mInputPtr;

        // Default is 'just one'
        return ' ';
    }

    /**
     * Method that reads and pre-processes replacement text for an internal
     * entity (parameter or generic).
     */
    private TextBuffer parseEntityValue(String id, Location loc, char quoteChar)
        throws IOException, XMLStreamException
    {
        /* 25-Jun-2004, TSa: Let's first mark current input source as the
         *   scope, so we can both make sure it ends in this input
         *   context (file), and that embedded single/double quotes
         *   in potentially expanded entities do not end the value
         *   definition (as per XML 1.0/3, 4.4.5)
         */
        WstxInputSource currScope = mInput;

        /* 18-Jul-2004, TSa: Also, let's see if parameter entities are
         *  allowed; they are only legal outside of main internal subset
         *  (ie. main XML input) file.
         */
        boolean allowPEs = mIsExternal || (mInput != mRootInput);

        TextBuffer tb = new TextBuffer(EXP_ENTITY_VALUE_LEN);
        tb.resetInitialized();

        char[] outBuf = tb.getCurrentSegment();
        int outPtr = tb.getCurrentSegmentSize();

        while (true) {
            if (mInputPtr >= mInputLen) {
                boolean check = (mInput == currScope);
                loadMore(getErrorMsg());
                // Did we get out of the scope?
                if (check && (mInput != currScope)) {
                    throwParseError("Unterminated entity value for entity '"
                                    +id+"' (definition started at "
                                    +loc+")");
                }
            }
            char c = mInputBuffer[mInputPtr++];

            // Let's get most normal chars 'skipped' first
            if (c >= CHAR_FIRST_PURE_TEXT) {
                ;
            } else if (c == quoteChar) {
                // Only end if we are in correct scope:
                if (mInput == currScope) {
                    break;
                }
            } else if (c == '&') { // char entity that needs to be replaced?
                /* 06-Sep-2004, TSa: We can NOT expand char entities, as
                 *   XML specs consider them 'real' (non-char) entities.
                 *   And expanding them would cause problems with entities
                 *   that have such entities.
                 */
                char d = resolveCharOnlyEntity(false);
                // Did we get a real char entity?
                if (d != CHAR_NULL) {
                    c = d;
                }
                // Either '&' itself, or expanded char entity
            } else if (c == '%') { // param entity?
                if (!allowPEs) {
                    throwParseError("Can not have parameter entities in entity value defined at the main level of internal subset (XML 1.1, #2.8).");
                }
                expandPE();
                // Need to loop over, no char available yet
                continue;
            } else if (c == '\n') {
                markLF();
            } else if (c == '\r') {
                if (skipCRLF(c)) {
                    if (mCfgNormalizeLFs) {
                        c = '\n';
                    } else {
                        // Special handling, to output 2 chars at a time:
                        outBuf[outPtr++] = c;
                        if (outPtr >= outBuf.length) { // need more room?
                            outBuf = tb.finishCurrentSegment();
                            outPtr = 0;
                        }
                        outBuf[outPtr++] = '\n';
                        if (outPtr >= outBuf.length) {
                            outBuf = tb.finishCurrentSegment();
                            outPtr = 0;
                        }
                        // No need to use default output
                        continue;
                    }
                } else if (mCfgNormalizeLFs) {
                    c = '\n'; // For Mac text
                }
            }
                
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = tb.finishCurrentSegment();
                outPtr = 0;
            }
        }
        tb.setCurrentLength(outPtr);

        // Ok, now need the closing '>':
        char c = skipDtdWs(true);
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected closing '>' after ENTITY declaration.");
        }
        return tb;
    }

    /**
     * This method is similar to {@link #parseEntityValue} in some ways,
     * but has some notable differences, due to the way XML specs define
     * differences. Main differences are that parameter entities are not
     * allowed (or rather, recognized as entities), and that general
     * entities are expanded right away; latter meaning that it is not
     * possible to do forward references to GEs.
     */
    private String parseAttrDefaultValue(char quoteChar, NameKey attrName,
                                         Location loc, boolean gotFixed)
        throws IOException, XMLStreamException
    {
        if (quoteChar != '"' && quoteChar != '\'') { // caller doesn't test it
            String msg = "; expected a single or double quote to enclose the default value";
            if (!gotFixed) {
                msg += ", or one of keywords (#REQUIRED, #IMPLIED, #FIXED)";
            }
            msg += " (for attribute '"+attrName+"')";
            throwDTDUnexpectedChar(quoteChar, msg);
        }

        /* Let's mark the current input source as the scope, so we can both
         * make sure it ends in this input context (DTD subset), and that
         * embedded single/double quotes in potentially expanded entities do
         * not end the value definition (as per XML 1.0/3, 4.4.5)
         */
        WstxInputSource currScope = mInput;

        TextBuffer tb = new TextBuffer(200);
        tb.resetInitialized();

        int outPtr = 0;
        char[] outBuf = tb.getCurrentSegment();
        int outLen = outBuf.length;

        /* One more note: this is mostly cut'n pasted from stream reader's
         * parseNormalizedAttrValue...
         */
        main_loop:

        while (true) {
            if (mInputPtr >= mInputLen) {
                boolean check = (mInput == currScope);
                loadMore(getErrorMsg());
                // Did we get out of the scope?
                if (check && (mInput != currScope)) {
                    throwParseError("Unterminated attribute default value for attribute '"
                                    +attrName+"' (definition started at "
                                    +loc+")");
                }
            }
            char c = mInputBuffer[mInputPtr++];

            // Let's do a quick for most attribute content chars:
            if (c < CHAR_FIRST_PURE_TEXT) {
                if (c <= CHAR_SPACE) {
                    if (c == '\n') {
                        markLF();
                    } else if (c == '\r') {
                        c = getNextChar(SUFFIX_IN_DEF_ATTR_VALUE);
                        if (c != '\n') { // nope, not 2-char lf (Mac?)
                            --mInputPtr;
                            c = mCfgNormalizeLFs ? '\n' : '\r';
                        } else {
                            // Fine if we are to normalize lfs
                            if (!mCfgNormalizeLFs) {
                                // Ok, except need to add leading '\r' first
                                if (!mCfgNormAttrs) {
                                    if (outPtr >= outLen) { // need more room?
                                        outBuf = mTextBuffer.finishCurrentSegment();
                                        outPtr = 0;
                                        outLen = outBuf.length;
                                    }
                                outBuf[outPtr++] = '\r';
                                }
                                // c is fine to continue
                            }
                        }
                        markLF();
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    }
                    if (mCfgNormAttrs) {
                        c = CHAR_SPACE;
                    }
                } else if (c == quoteChar) {
                    /* It is possible to get these via expanded entities;
                     * need to make sure this is the main input level:
                     */
                    if (mInput == currScope) {
                        break;
                    }
                } else if (c == '&') { // an entity of some sort...
                    if (inputInBuffer() >= 3) {
                        c = resolveSimpleEntity(true);
                    } else {
                        /* 06-Sep-2004, TSa: Unlike with entity values, here
                         *   we DO NEED TO expand standard pre-defined
                         *   entities too...
                         */
                        c = resolveCharOnlyEntity(true);
                    }
                    // Only get null if it's a 'real' external entity...
                    if (c == CHAR_NULL) {
                        c = getNextChar(SUFFIX_IN_ENTITY_REF);
                        String id = parseEntityName(c);
                        
                        /* This is only complicated for external subsets, since
                         * they may 'inherit' entity definitions from preceding
                         * internal subset...
                         */
                        if (mIsExternal) {
                            int setId = expandEntity(id, mPredefdGEs,
                                                     mGeneralEntities, false);
                            if (setId == 1) { // came from internal subset...
                                mUsesPredefdEntities = true;
                                /* No need to further keep track of references,
                                 * as this means this subset is not cachable...
                                 * so let's just free up Map if it has been created
                                 */
                                mRefdGEs = null;
                            } else {
                                // Ok, just need to mark reference, if we still care:
                                if (!mUsesPredefdEntities) {
                                    // Let's also mark down the fact we referenced the entity:
                                    if (mRefdGEs == null) {
                                        mRefdGEs = new HashSet();
                                    }
                                    mRefdGEs.add(id);
                                }
                            }
                        } else { // internal subset, let's just expand it
                            expandEntity(id, null, mGeneralEntities, false);
                        }
                        // Ok, should have updated the input source by now
                        continue main_loop;
                    }
                } else if (c == '<') {
                    throwParseError("Unexpected '<' "+SUFFIX_IN_DEF_ATTR_VALUE);
                }
            } // if (c < CHAR_FIRST_PURE_TEXT)
                
            // Ok, let's just add char in, whatever it was
            if (outPtr >= outLen) { // need more room?
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
                outLen = outBuf.length;
            }
            outBuf[outPtr++] = c;
        }

        // Fine; let's tell TextBuild we're done:
        tb.setCurrentLength(outPtr);
        return tb.contentsAsString();
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, conditional blocks:
    //////////////////////////////////////////////////
     */

    private void checkInclusion()
        throws IOException, XMLStreamException
    {
        String keyword;

        // INCLUDE/IGNORE not allowed in internal subset...
        /* 18-Jul-2004, TSa: Except if it's in an expanded parsed external
         *   entity...
         */
        if (!mIsExternal) {
            if (mInput == mRootInput) {
                throwParseError("Internal DTD subset can not use (INCLUDE/IGNORE) directives (except via external entities).");
            }
        }

        if (skipDtdWs(false) != 'I') {
            // let's obtain the keyword for error reporting purposes:
            keyword = readDTDKeyword("I");
        } else {
            char c = getNextExpanded();
            if (c == 'G') {
                keyword = checkDTDKeyword("NORE");
                if (keyword == null) {
                    handleIgnored();
                    return;
                }
                keyword = "IG"+keyword;
            } else if (c == 'N') {
                keyword = checkDTDKeyword("CLUDE");
                if (keyword == null) {
                    handleIncluded();
                    return;
                }
                keyword = "IN"+keyword;
            } else {
                --mInputPtr;
                keyword = readDTDKeyword("I");
            }
        }

        // If we get here, it was an error...
        throwParseError("Unrecognized directive '"+keyword+"'; expected either 'IGNORE' or 'INCLUDE'.");
    }

    private void handleIncluded()
        throws IOException, XMLStreamException
    {
        char c = skipDtdWs(false);
        if (c != '[') {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected '[' to follow 'INCLUDE' directive.");
        }
        ++mIncludeCount;
    }

    private void handleIgnored()
        throws IOException, XMLStreamException
    {
        char c = skipDtdWs(false);
        int count = 1; // Nesting of IGNORE/INCLUDE sections we have to match

        if (c != '[') {
            throwDTDUnexpectedChar(c, "; expected '[' to follow 'IGNORE' directive.");
        }

        /* Ok; now, let's just skip until we get the closing ']]>'
         */
        String errorMsg = getErrorMsg();
        while (true) {
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(errorMsg);
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == ']') { // closing?
                if (getNextChar(errorMsg) == ']'
                    && getNextChar(errorMsg) == '>') {
                    if (--count < 1) { // done!
                        return;
                    }
                    // nested ignores, let's just continue
                } else {
                    --mInputPtr; // need to push one char back, may be '<'
                }
            } else if (c == '<') {
                if (getNextChar(errorMsg) == '!'
                    && getNextChar(errorMsg) == '[') {
                    // Further nesting, sweet
                    ++count;
                } else {
                    --mInputPtr; // need to push one char back, may be '<'
                }
            }
        }
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods, validation, exceptions:
    //////////////////////////////////////////////////
     */

    private void reportBadDirective(String dir)
        throws WstxException
    {
	String msg = "Unrecognized DTD directive '<!"+dir+" >'; expected ATTLIST, ELEMENT, ENTITY or NOTATION";
	if (mCfgSupportDTDPP) {
	    msg += " (or, for DTD++, TARGETNS)";
	}
        throwDTDError(msg);
    }

    private void throwDTDError(String msg)
        throws WstxException
    {
        // !!! TBI: separate type for these exceptions?
        throwParseError(msg);
    }

    private void throwDTDElemError(String msg, Object elem)
        throws WstxException
    {
        throwDTDError(elemDesc(elem) + ": " + msg);
    }

    private void throwDTDAttrError(String msg, DTDElement elem, NameKey attrName)
        throws WstxException
    {
        throwDTDError(attrDesc(elem, attrName) + ": " + msg);
    }

    private void throwDTDUnexpectedChar(int i, String extraMsg)
        throws WstxException
    {
        if (extraMsg == null) {
            throwUnexpectedChar(i, getErrorMsg());
        }
        throwUnexpectedChar(i, getErrorMsg()+extraMsg);
    }

    private String elemDesc(Object elem) {
        return "Element <"+elem+">)";
    }

    private String attrDesc(Object elem, NameKey attrName) {
        return "Attribute '"+attrName+"' (of element <"+elem+">)";
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, main-level declaration parsing:
    /////////////////////////////////////////////////////
     */

    private void handleDeclaration(char c)
        throws IOException, XMLStreamException
    {
        String keyw = null;
 
        if (c == 'A') { // ATTLIST?
            keyw = checkDTDKeyword("TTLIST");
            if (keyw == null) {
                handleAttlistDecl();
                return;
            }
            keyw = "A" + keyw;
        } else if (c == 'E') { // ENTITY, ELEMENT?
            c = getNextExpanded();
            if (c == 'N') {
                keyw = checkDTDKeyword("TITY");
                if (keyw == null) {
                    handleEntityDecl(false);
                    return;
                }
                keyw = "EN" + keyw;
            } else if (c == 'L') {
                keyw = checkDTDKeyword("EMENT");
                if (keyw == null) {
                    handleElementDecl();
                    return;
                }
                keyw = "EL" + keyw;
            } else {
                keyw = readDTDKeyword("E");
            }
        } else if (c == 'N') { // NOTATION?
            keyw = checkDTDKeyword("OTATION");
            if (keyw == null) {
                handleNotationDecl();
                return;
            }
            keyw = "N" + keyw;
        } else if (c == 'T' && mCfgSupportDTDPP) { // (dtd++ only) TARGETNS?
            keyw = checkDTDKeyword("ARGETNS");
            if (keyw == null) {
                handleTargetNsDecl();
                return;
            }
            keyw = "T" + keyw;
        } else {
            --mInputPtr;
            keyw = readDTDKeyword(null);
        }
        reportBadDirective(keyw);
    }

    /**
     * Specialized method that handles potentially suppressable entity
     * declaration. Specifically: at this point it is know that first
     * letter is 'E', that we are outputting flattened DTD info,
     * and that parameter entity declarations are to be suppressed.
     * Furthermore, flatten output is still being disabled, and needs
     * to be enabled by the method at some point.
     */
    private void handleSuppressedDeclaration()
        throws IOException, XMLStreamException
    {
        String keyw;
        char c = getNextExpanded();

        if (c == 'N') {
            keyw = checkDTDKeyword("TITY");
            if (keyw == null) {
                handleEntityDecl(true);
                return;
            }
            keyw = "EN" + keyw;
            mFlattenWriter.enableOutput(mInputPtr); // error condition...
        } else {
            mFlattenWriter.enableOutput(mInputPtr);
            mFlattenWriter.output("<!E");
            mFlattenWriter.output(c);

            if (c == 'L') {
                keyw = checkDTDKeyword("EMENT");
                if (keyw == null) {
                    handleElementDecl();
                    return;
                }
                keyw = "EL" + keyw;
            } else {
                keyw = readDTDKeyword("E");
            }
        }
        reportBadDirective(keyw);
    }

    private void handleAttlistDecl()
        throws IOException, XMLStreamException
    {
        /* This method will handle PEs that contain the whole element
         * name. Since it's illegal to have partials, we can then proceed
         * to just use normal parsing...
         */
        char c = skipObligatoryDtdWs(true);
        final NameKey elemName = readDTDQName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location loc = getLocation();

        // Ok, where's our element?
        HashMap m = getElementMap();
        DTDElement elem = (DTDElement) m.get(elemName);

        if (elem == null) { // ok, need a placeholder
            // Let's add ATTLIST location as the temporary location too
            elem = DTDElement.createPlaceholder(loc, elemName);
            m.put(elemName, elem);
        }

        // Ok, need to loop to get all attribute defs:
        int index = 0;

        while (true) {
            /* White space is optional, if we get the closing '>' char;
             * otherwise it's obligatory.
             */
            c = getNextExpanded();
            if (isSpaceChar(c)) {
                // Let's push it back in case it's LF, to be handled properly
                --mInputPtr;
                c = skipDtdWs(true);
            } else if (c != '>') {
                throwDTDUnexpectedChar(c, "; excepted either '>' closing ATTLIST declaration, or a white space character separating individual attribute declarations");
            }
            if (c == '>') {
                break;
            }
            handleAttrDecl(elem, c, index, loc);
            ++index;
        }
    }

    private void handleElementDecl()
        throws IOException, XMLStreamException
    {
        /* This method will handle PEs that contain the whole element
         * name. Since it's illegal to have partials, we can then proceed
         * to just use normal parsing...
         */
        char c = skipObligatoryDtdWs(true);
        final NameKey elemName = readDTDQName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location loc = getLocation();

        // Ok; name got, need some white space next
        c = skipObligatoryDtdWs(true);

        /* Then the content spec: either a special case (ANY, EMPTY), or
         * a parenthesis group for 'real' content spec
         */
        StructValidator val = null;
        int vldContent = CONTENT_ALLOW_MIXED;

        if (c == '(') { // real content model
            c = skipDtdWs(true); // I guess we can allow comments...
            if (c == '#') {
                val = readMixedSpec(elemName, mCfgValidate);
                vldContent = CONTENT_ALLOW_MIXED; // checked against DTD
            } else {
                --mInputPtr; // let's push it back...
                ContentSpec spec = readContentSpec(elemName, true, mCfgValidate);
                val = spec.getValidator();
                if (val == null) {
                    val = new DFAValidator(DFAState.constructDFA(spec));
                }
                vldContent = CONTENT_ALLOW_NON_MIXED; // checked against DTD
            }
        } else if (isNameStartChar(c)) {
            do { // dummy loop to allow break:
                String keyw = null;
                if (c == 'A') {
                    keyw = checkDTDKeyword("NY");
                    if (keyw == null) {
                        val = null;
                        vldContent = CONTENT_ALLOW_DTD_ANY; // no DTD checks
                        break;
                    }
                    keyw = "A"+keyw;
                } else if (c == 'E') {
                    keyw = checkDTDKeyword("MPTY");
                    if (keyw == null) {
                        val = null; // could also use the empty validator
                        vldContent = CONTENT_ALLOW_NONE; // specific checks
                        break;
                    }
                    keyw = "E"+keyw;
                } else {
                    --mInputPtr;
                    keyw = readDTDKeyword(null);
                }
                throwParseError("Unrecognized DTD content spec keyword '"
                                +keyw+"'; expected ANY or EMPTY");
             } while (false);
        } else {
            throwDTDUnexpectedChar(c, getErrorMsg()+": excepted '(' to start content specification");
        }

        // Ok, still need the trailing gt-char to close the declaration:
        c = skipDtdWs(true);
        if (c != '>') {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected '>' to finish the ENTITY declaration");
        }

        HashMap m = getElementMap();
        DTDElement oldElem = (DTDElement) m.get(elemName);
        // Ok to have it if it's not 'really' declared

        if (oldElem != null) {
            if (oldElem.isDefined()) { // oops, a problem!
                DTDSubsetImpl.throwElementException(oldElem, loc);
            }

            /* 09-Sep-2004, TSa: Need to transfer existing attribute
             *   definitions, however...
             */
            oldElem = oldElem.define(loc, val, vldContent);
        } else {
            // Sweet, let's then add the definition:
            oldElem = DTDElement.createDefined(loc, elemName, val, vldContent);
        }
        m.put(elemName, oldElem);
    }

    /**
     * @param suppressPEDecl If true, will need to take of enabling/disabling
     *   of flattened output.
     */
    private void handleEntityDecl(boolean suppressPEDecl)
        throws IOException, XMLStreamException
    {
        String emsg = getErrorMsg();
        /* Hmmh. We won't allow parameter entities at this point, so let's
         * not use 'skipDtdWs'. We won't allow comments then, either; could
         * be improved if that seems necessary?
         */
        char c = getNextCharAfterWS(emsg);
        boolean isParam = (c == '%');

        if (suppressPEDecl) {
            if (!isParam) {
                mFlattenWriter.enableOutput(mInputPtr);
                mFlattenWriter.output("<!ENTITY ");
                mFlattenWriter.output(c);
            }
        }

        if (isParam) {
            c = skipObligatoryDtdWs(true);
        }

        String id = readDTDName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location evtLoc = getLocation();
        EntityDecl ent;

        try {
            c = skipDtdWs(true);
            if (c == '\'' || c == '"') { // internal entity
                /* Let's get the exact location of actual content, not the
                 * opening quote. To do that, need to 'peek' next char, then
                 * push it back:
                 */
                char foo = getNextChar(emsg);
                Location contentLoc = getLastCharLocation();
                --mInputPtr; // pushback
                TextBuffer contents = parseEntityValue(id, contentLoc, c);
                ent = new IntEntity(evtLoc, id, getSource(),
                                    contents.contentsAsArray(), contentLoc);
            } else {
                if (!isNameStartChar(c)) {
                    throwDTDUnexpectedChar(c, getErrorMsg()+"; expected either quoted value, or keyword 'PUBLIC' or 'SYSTEM'.");
                }
                ent = handleExternalEntityDecl(isParam, id, c, evtLoc);
            }
        } finally {
            /* Ok; one way or the other, entity declaration contents have now
             * been read in.
             */
            if (suppressPEDecl && isParam) {
                mFlattenWriter.enableOutput(mInputPtr);
            }
        }

        // Ok, got it!
        HashMap m;
        if (isParam) {
            m = mParamEntities;
            if (m == null) {
                mParamEntities = m = new HashMap();
            }
        } else {
            m = mGeneralEntities;
            if (m == null) {
                /* Let's try to get insert-ordered Map, to be able to
                 * report redefinition problems when validating subset
                 * compatibility
                 */
                mGeneralEntities = m = JdkFeatures.getInstance().getInsertOrderedMap();
            }
        }

        // First definition sticks...
        Object old;
        if (m.size() > 0 && (old = m.get(id)) != null) {
            // Application may want to know about the problem...
            if (mReporter != null) {
                EntityDecl oldED = (EntityDecl) old;
                String str = " entity '"+id+"' defined more than once: first declaration at "
                    + oldED.getLocation();
                if (isParam) {
                    str = "Parameter" + str;
                } else {
                    str = "General" + str;
                }
                try { // Doh.... this is silly.. but shouldn't really happen:
                    mReporter.report(str, ErrorConsts.WT_ENT_DECL, oldED, evtLoc);
                } catch (XMLStreamException strex) {
                    throwFromStrE(strex);
                }
            }
        } else {
            m.put(id, ent);
        }
    }

    /**
     * Method called to handle <!NOTATION ... > declaration.
     */
    private void handleNotationDecl()
        throws IOException, XMLStreamException
    {
        char c = skipObligatoryDtdWs(true); // comments are ok
        String id = readDTDName(c);

        c = skipObligatoryDtdWs(true); // comments are ok
        boolean isPublic = checkPublicSystemKeyword(c);

        String pubId, sysId;

        c = skipObligatoryDtdWs(true); // comments should be ok

        // Ok, now we can parse the reference; first public id if needed:
        if (isPublic) {
            if (c != '"' && c != '\'') {
                throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a quote to start the public identifier.");
            }
            pubId = parsePublicId(c, mCfgNormalizeLFs, getErrorMsg());
            c = skipDtdWs(true);
        } else {
            pubId = null;
        }

        /* And then we may need the system id; one NOTATION oddity, if
         * there's public id, system one is optional.
         */
        if (c == '"' || c == '\'') {
            sysId = parseSystemId(c, mCfgNormalizeLFs, getErrorMsg());
            c = skipDtdWs(true);
        } else {
            if (!isPublic) {
                throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a quote to start the system identifier.");
            }
            sysId = null;
        }

        // And then we should get the closing '>'
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected closing '>' after NOTATION declaration.");
        }

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location evtLoc = getLocation();
        NotationDecl nd = new NotationDecl(evtLoc, id, pubId, sysId);

        // Any definitions from the internal subset?
        if (mPredefdNotations != null) {
            NotationDecl oldDecl = (NotationDecl) mPredefdNotations.get(id);
            if (oldDecl != null) { // oops, a problem!
                DTDSubsetImpl.throwNotationException(oldDecl, nd);
            }
        }

        HashMap m = mNotations;
        if (m == null) {
            /* Let's try to get insert-ordered Map, to be able to
             * report redefinition problems in proper order when validating
             * subset compatibility
             */
            mNotations = m = JdkFeatures.getInstance().getInsertOrderedMap();
        } else {
            NotationDecl oldDecl = (NotationDecl) m.get(id);
            if (oldDecl != null) { // oops, a problem!
                DTDSubsetImpl.throwNotationException(oldDecl, nd);
            }
        }
        m.put(id, nd);
    }

    /**
     * Method called to handle <!TARGETNS ... > declaration (the only
     * new declaration type for DTD++)
     *<p>
     * Note: only valid for DTD++, in 'plain DTD' mode shouldn't get
     * called.
     */
    private void handleTargetNsDecl()
        throws IOException, XMLStreamException
    {
        mAnyDTDppFeatures = true;
        
        char c = skipObligatoryDtdWs(true); // comments are ok
        String name;
        
        // Explicit namespace name?
        if (isNameStartChar(c)) {
            name = readDTDLocalName(c, false);
            c = skipObligatoryDtdWs(true);
        } else { // no, default namespace (or error)
            name = null;
        }
        
        // Either way, should now get a quote:
        if (c != '"' && c != '\'') {
            if (c == '>') { // slightly more accurate error
                throwDTDError("Missing namespace URI for TARGETNS directive");
            }
            throwDTDUnexpectedChar(c, "; expected a single or double quote to enclose the namespace URI");
        }
        
        /* !!! 07-Nov-2004, TSa: what's the exact value we should get
         *   here? Ns declarations can have any attr value...
         */
        String uri = parseSystemId(c, false, "in namespace URI");
        
        // Do we need to normalize the URI?
        if ((mConfigFlags & CFG_INTERN_NS_URIS) != 0) {
            uri = InternCache.getInstance().intern(uri);
        }
        
        // Ok, and then the closing '>':
        c = skipDtdWs(true);
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected '>' to end TARGETNS directive");
        }
        
        if (name == null) { // default NS URI
            mDefaultNsURI = uri;
        } else {
            if (mNamespaces == null) {
                mNamespaces = new HashMap();
            }
            mNamespaces.put(name, uri);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, secondary decl parsing methods
    /////////////////////////////////////////////////////
     */

    /**
     * @param elem Element that contains this attribute
     * @param c First character of what should be the attribute name
     * @param index Sequential index number of this attribute as children
     *    of the element; used for creating bit masks later on.
     * @param loc Location of the element name in attribute list declaration
     */
    private void handleAttrDecl(DTDElement elem, char c, int index,
                                Location loc)
        throws IOException, XMLStreamException
    {
        // First attribute name
        NameKey attrName = readDTDQName(c);
        
        // then type:
        c = skipObligatoryDtdWs(true);

        int type = 0;
        WordResolver enumValues = null;
        
        if (c == '(') { // enumerated type
            enumValues = parseEnumerated(elem, attrName, false);
            type = DTDAttribute.TYPE_ENUMERATED;
        } else {
            String typeStr = readDTDName(c);
            
            dummy:
            do { // dummy loop
                switch (typeStr.charAt(0)) {
                case 'C': // CDATA
                    if (typeStr == "CDATA") {
                        type = DTDAttribute.TYPE_CDATA;
                        break dummy;
                    }
                    break;
                case 'I': // ID, IDREF, IDREFS
                    if (typeStr == "ID") {
                        type = DTDAttribute.TYPE_ID;
                        break dummy;
                    } else if (typeStr == "IDREF") {
                        type = DTDAttribute.TYPE_IDREF;
                        break dummy;
                    } else if (typeStr == "IDREFS") {
                        type = DTDAttribute.TYPE_IDREFS;
                        break dummy;
                    }
                    break;
                case 'E': // ENTITY, ENTITIES
                    if (typeStr == "ENTITY") {
                        type = DTDAttribute.TYPE_ENTITY;
                        break dummy;
                    } else if (typeStr == "ENTITIES") {
                        type = DTDAttribute.TYPE_ENTITIES;
                        break dummy;
                    }
                    break;
                case 'N': // NOTATION, NMTOKEN, NMTOKENS
                    if (typeStr == "NOTATION") {
                        type = DTDAttribute.TYPE_NOTATION;
                        /* Special case; is followed by a list of
                         * enumerated ids...
                         */
                        c = skipObligatoryDtdWs(true);
                        if (c != '(') {
                            throwDTDUnexpectedChar(c, "Excepted '(' to start the list of NOTATION ids");
                        }
                        enumValues = parseEnumerated(elem, attrName, true);
                        break dummy;
                    } else if (typeStr == "NMTOKEN") {
                        type = DTDAttribute.TYPE_NMTOKEN;
                        break dummy;
                    } else if (typeStr == "NMTOKENS") {
                        type = DTDAttribute.TYPE_NMTOKENS;
                        break dummy;
                    }
                    break;
                }

                // Problem:
                throwDTDAttrError("Unrecognized attribute type '"+typeStr+"'"
                                   +ErrorConsts.ERR_DTD_ATTR_TYPE,
                                   elem, attrName);
            } while (false);
        }

        int defType = DTDAttribute.DEF_DEFAULT;
        String defVal = null;

        // Ok, and how about the default declaration?
        c = skipDtdWs(true);
        if (c == '#') {
            String defTypeStr = readDTDName();
            if (defTypeStr == "REQUIRED") {
                defType = DTDAttribute.DEF_REQUIRED;
            } else if (defTypeStr == "IMPLIED") {
                defType = DTDAttribute.DEF_IMPLIED;
            } else if (defTypeStr == "FIXED") {
                defType = DTDAttribute.DEF_FIXED;
                c = skipObligatoryDtdWs(true);
                defVal = parseAttrDefaultValue(c, attrName, loc, true);
            } else {
                throwDTDAttrError("Unrecognized attribute default value directive #"+defTypeStr
                                   +ErrorConsts.ERR_DTD_DEFAULT_TYPE,
                                   elem, attrName);
            }
        } else {
            defVal = parseAttrDefaultValue(c, attrName, loc, false);
        }

        /* There are some checks that can/need to be done now, such as:
         *
         * - [#3.3.1/VC: ID Attribute default] def. value type can not
         *   be #FIXED
         */
        if (type == DTDAttribute.TYPE_ID) {
            if (defType == DTDAttribute.DEF_DEFAULT
                || defType == DTDAttribute.DEF_FIXED) {
                throwDTDAttrError("has type ID; can not have a default (or #FIXED) value (XML 1.0/#3.3.1)",
                                  elem, attrName);
            }
        }

        // getting null means this is a dup...
        DTDAttribute attr = elem.addAttribute(this, attrName, type, defType,
                                              defVal, enumValues);
        if (attr == null) {
            // anyone interested in knowing about possible problem?
            if (mReporter != null) {
                try { // Doh.... this is silly.. but shouldn't really happen:
                    mReporter.report("Attribute '"+attrName+"' already declared for element <"+elem+">; ignoring re-declaration", ErrorConsts.WT_ATTR_DECL, elem, loc);
                } catch (XMLStreamException strex) {
                    throwFromStrE(strex);
                }
            }
        } else {
            if (defVal != null) {
                attr.validateDefault(this, mCfgNormAttrs);
            }
        }
    }

    /**
     * Parsing method that reads a list of one or more space-separated
     * tokens (nmtoken or name, depending on 'isNotation' argument)
     */
    private WordResolver parseEnumerated(DTDElement elem, NameKey attrName,
                                         boolean isNotation)
        throws IOException, XMLStreamException
    {
        /* Need to use tree set to be able to construct the data
         * structs we need later on...
         */
        TreeSet set = new TreeSet();

        char c = skipDtdWs(true);
        if (c == ')') { // just to give more meaningful error msgs
            throwDTDUnexpectedChar(c, " (empty list; missing identifier(s))?");
        }

        HashMap sharedEnums;

        if (isNotation) {
            sharedEnums = null;
        } else {
            sharedEnums = mSharedEnumValues;
            if (sharedEnums == null && !isNotation) {
                mSharedEnumValues = sharedEnums = new HashMap();
            }
        }

        String id = isNotation ? readNotationEntry(c, attrName)
            : readEnumEntry(c, sharedEnums);
        set.add(id);
        
        while (true) {
            c = skipDtdWs(true);
            if (c == ')') {
                break;
            }
            if (c != '|') {
                throwDTDUnexpectedChar(c, "; missing '|' separator?");
            }
            c = skipDtdWs(true);
            id = isNotation ? readNotationEntry(c, attrName)
                : readEnumEntry(c, sharedEnums);
            if (!set.add(id)) {
                throwDTDAttrError("Duplicate enumeration value '"+id+"'",
                                   elem, attrName);
            }
        }

        // Ok, let's construct the minimal data struct, then:
        return WordResolver.constructInstance(set);
    }

    /**
     * Method called to read a notation referency entry; done both for
     * attributes of type NOTATION, and for external unparsed entities
     * that refere to a notation. In both cases, notation referenced
     * needs to have been defined earlier.
     */
    private String readNotationEntry(char c, NameKey attrName)
        throws IOException, WstxException
    {
        String id = readDTDName(c);

        /* 05-Oct-2004, TSa: Need to check that notation has been declared
         *    somewhere... and if that somewhere is int. subset, when parsing
         *    external subset, also need to note that:
         */
        if (mPredefdNotations != null) {
            NotationDecl decl = (NotationDecl) mPredefdNotations.get(id);
            if (decl != null) {
                mUsesPredefdNotations = true;
            }
            return decl.getName();
        }

        NotationDecl decl = (mNotations == null) ? null :
            (NotationDecl) mNotations.get(id);
        if (decl == null) {
            String msg = "Notation '"+id+"' not defined; ";
            if (attrName == null) { // reference from entity
                throwDTDError(msg+"can not refer to from an entity");
            }
            // reference from attribute
            throwDTDError(msg+"can not be used as value for attribute list of '"+attrName+"'");
        }

        return decl.getName();
    }

    private String readEnumEntry(char c, HashMap sharedEnums)
        throws IOException, WstxException
    {
        String id = readDTDNmtoken(c);

        /* Let's make sure it's shared for this DTD subset; saves memory
         * both for DTDs and resulting docs. Could also intern Strings?
         */
        String sid = (String) sharedEnums.get(id);
        if (sid == null) {
            sid = id;
            if (INTERN_SHARED_NAMES) {
                /* 19-Nov-2004, TSa: Let's not use intern cache here...
                 *   shouldn't be performance critical (DTDs themselves
                 *   cached), and would add more entries to cache.
                 */
                sid = sid.intern();
            }
            sharedEnums.put(sid, sid);
        }
        return sid;
    }


    /**
     * Method called to parse what seems like a mixed content specification.
     */
    private StructValidator readMixedSpec(NameKey elemName, boolean construct)
        throws IOException, XMLStreamException
    {
        String keyw = checkDTDKeyword("PCDATA");
        if (keyw != null) {
            throwParseError("Unrecognized directive #"+keyw+"'; expected #PCDATA (or element name)");
        }

        HashMap m = JdkFeatures.getInstance().getInsertOrderedMap();
        while (true) {
            char c = skipDtdWs(true);
            if (c == ')') {
                break;
            }
            if (c == '|') {
                c = skipDtdWs(true);
            } else if (c == ',') {
                throwDTDUnexpectedChar(c, " (sequences not allowed within mixed content)");
            } else if (c == '(') {
                throwDTDUnexpectedChar(c, " (sub-content specs not allowed within mixed content)");
            }
            NameKey n = readDTDQName(c);
            Object old = m.put(n, TokenContentSpec.construct(' ', n));
            if (old != null) {
                throwDTDElemError("duplicate child element <"+n+"> in mixed content model",
                                  elemName);
            }
        }

        /* One more check: can have a trailing asterisk; in fact, have
         * to have one if there were any elements.
         */
        char c = (mInputPtr < mInputLen) ?
            mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
        if (c != '*') {
            if (m.size() > 0) {
                throwParseError("Missing trailing '*' after a non-empty mixed content specification");
            }
            --mInputPtr; // need to push it back
        }
        if (!construct) { // no one cares?
            return null;
        }

        /* Without elements, it's considered "pure" PCDATA, which can use a
         * specific 'empty' validator:
         */
        if (m.isEmpty()) {
            return EmptyValidator.getInstance();
        }
        ContentSpec spec = ChoiceContentSpec.constructMixed(mCfgNsEnabled, m.values());
        StructValidator val = spec.getValidator();
        if (val == null) {
            DFAState dfa = DFAState.constructDFA(spec);
            val = new DFAValidator(dfa);
        }
        return val;
    }

    private int mTokenIndex = 1;

    private ContentSpec readContentSpec(NameKey elemName, boolean mainLevel,
                                        boolean construct)
        throws IOException, XMLStreamException
    {
        if (mainLevel) {
            mTokenIndex = 1;
        }
        
        ArrayList subSpecs = new ArrayList();
        boolean isChoice = false; // default to sequence
        boolean choiceSet = false;

        while (true) {
            char c = skipDtdWs(true);
            if (c == ')') {
                // Need to have had at least one entry...
                if (subSpecs.isEmpty()) {
                    throwParseError("Empty content specification for '"+elemName+"' (need at least one entry)");
                }
                break;
            }
            if (c == '|' || c == ',') { // choice/seq indicator
                boolean newChoice = (c == '|');
                if (!choiceSet) {
                    isChoice = newChoice;
                    choiceSet = true;
                } else {
                    if (isChoice != newChoice) {
                        throwParseError("Can not mix content spec separators ('|' and ','); need to use parenthesis groups");
                    }
                }
                c = skipDtdWs(true);
            } else {
                // Need separator between subspecs...
                if (!subSpecs.isEmpty()) {
                    throwDTDUnexpectedChar(c, " (missing separator '|' or ','?)");
                }
            }
            if (c == '(') {
                ContentSpec cs = readContentSpec(elemName, false, construct);
                subSpecs.add(cs);
                continue;
            }

            // Just to get better error messages:
            if (c == '|' || c == ',') {
                throwDTDUnexpectedChar(c, " (missing element name?)");
            }
            NameKey thisName = readDTDQName(c);

            /* Now... it's also legal to directly tag arity marker to a
             * single element name, too...
             */
            char arity = readArity();
            ContentSpec cs = construct ?
                TokenContentSpec.construct(arity, thisName)
                : TokenContentSpec.getDummySpec();
            subSpecs.add(cs);
        }
        
        char arity = readArity();

        /* Not really interested in constructing anything? Let's just
         * return the dummy placeholder.
         */
        if (!construct) {
            return TokenContentSpec.getDummySpec();
        }

        // Just one entry? Can just return it as is, combining arities
        if (subSpecs.size() == 1) {
            ContentSpec cs = (ContentSpec) subSpecs.get(0);
            char otherArity = cs.getArity();
            if (arity != otherArity) {
                cs.setArity(combineArities(arity, otherArity));
            }
            return cs;
        }

        if (isChoice) {
            return ChoiceContentSpec.constructChoice(mCfgNsEnabled, arity, subSpecs);
        }
        return SeqContentSpec.construct(mCfgNsEnabled, arity, subSpecs);
    }

    private static char combineArities(char arity1, char arity2)
    {
        if (arity1 == arity2) {
            return arity1;
        }

        // no modifier doesn't matter:
        if (arity1 == ' ') {
            return arity2;
        }
        if (arity2 == ' ') {
            return arity1;
        }
        // Asterisk is most liberal, supercedes others:
        if (arity1 == '*' || arity2 == '*') {
            return '*';
        }

        /* Ok, can only have '+' and '?'; which combine to
         * '*'
         */
        return '*';
    }

    /**
     * Method that handles rest of external entity declaration, after
     * it's been figured out entity is not internal (does not continue
     * with a quote).
     * 
     * @param evtLoc Location where entity declaration directive started;
     *   needed when construction event Objects for declarations.
     */
    private EntityDecl handleExternalEntityDecl(boolean isParam, String id,
                                                char c, Location evtLoc)
        throws IOException, XMLStreamException
    {
        String errId = null;
        boolean isPublic = checkPublicSystemKeyword(c);

        String pubId = null;

        // Ok, now we can parse the reference; first public id if needed:
        if (isPublic) {
            c = skipObligatoryDtdWs(true); // comments should be ok
            if (c != '"' && c != '\'') {
                throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a quote to start the public identifier.");
            }
            pubId = parsePublicId(c, mCfgNormalizeLFs, getErrorMsg());
        }

        // And then we need the system id:
        c = skipObligatoryDtdWs(true); // comments should be ok
        if (c != '"' && c != '\'') {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected a quote to start the system identifier.");
        }
        String sysId = parseSystemId(c, mCfgNormalizeLFs, getErrorMsg());

        // Ok; how about notation?
        String notationId = null;

        c = skipDtdWs(true);
        if (c != '>') {
            checkDTDKeyword("NDATA", c, "; expected either NDATA keyword, or closing '>'.");
            c = skipObligatoryDtdWs(true); // comments should be ok
            notationId = readNotationEntry(c, null);
            c = skipDtdWs(true);
        }

        // Ok, better have '>' now:
        if (c != '>') {
            throwDTDUnexpectedChar(c, getErrorMsg()+"; expected closing '>'.");
        }

        if (notationId == null) { // parsed entity:
            return new ParsedExtEntity(evtLoc, id, getSource(),
                                       pubId, sysId);
        }

        // unparsed entity
        return new UnparsedExtEntity(evtLoc, id, getSource(),
                                     pubId, sysId, notationId);
    }

    /*
    ///////////////////////////////////////////////////////
    // Data struct access
    ///////////////////////////////////////////////////////
     */

    private HashMap getElementMap() {
        HashMap m = mElements;
        if (m == null) {
            /* Let's try to get insert-ordered Map, to be able to
             * report redefinition problems in proper order when validating
             * subset compatibility
             */
            mElements = m = JdkFeatures.getInstance().getInsertOrderedMap();
        }
        return m;
    }

    final NameKey mAccessKey = new NameKey(null, null);

    /**
     * Method used to 'intern()' qualified names; main benefit is reduced
     * memory usage as the name objects are shared. May also slightly
     * speed up Map access, as more often identity comparisons catch
     * matches.
     *<p>
     * Note: it is assumed at this point that access is only from a single
     * thread, and non-recursive -- generally valid assumption as readers are
     * not shared. Restriction is needed since the method is not re-entrant:
     * it uses mAccessKey during the method call.
     */
    private NameKey findSharedName(String prefix, String localName)
    {
        HashMap m = mSharedNames;

        if (mSharedNames == null) {
            mSharedNames = m = new HashMap();
        } else {
            // Maybe we already have a shared instance... ?
            NameKey key = mAccessKey;
            key.reset(prefix, localName);
            key = (NameKey) m.get(key);
            if (key != null) { // gotcha
                return key;
            }
        }

        // Not found; let's create, cache and return it:
        NameKey result = new NameKey(prefix, localName);
        m.put(result, result);
        return result;
    }
}

