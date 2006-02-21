/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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
import java.text.MessageFormat;
import java.util.*;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.ent.*;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.util.InternCache;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuffer;
import com.ctc.wstx.util.WordResolver;

/**
 * Reader that reads in DTD information from internal or external subset.
 *<p>
 * There are 2 main modes for DTDReader, depending on whether it is parsing
 * internal or external subset. Parsing of internal subset is somewhat
 * simpler, since no dependency checking is needed. For external subset,
 * handling of parameter entities is bit more complicated, as care has to
 * be taken to distinguish between using PEs defined in int. subset, and
 * ones defined in ext. subset itself. This determines cachability of
 * external subsets.
 *<p>
 * Reader also implements simple stand-alone functionality for flattening
 * DTD files (expanding all references to their eventual textual form);
 * this is sometimes useful when optimizing modularized DTDs
 * (which are more maintainable) into single monolithic DTDs (which in
 * general can be more performant).
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

    // Extracted wstx-specific settings:

    final boolean mCfgNormalizeLFs;

    final boolean mCfgNormAttrs;

    final boolean mCfgSupportDTDPP;

    /**
     * This flag indicates whether we should build a validating 'real'
     * validator (true, the usual case),
     * or a simpler pseudo-validator that can do all non-validation tasks
     * that are based on DTD info (entity expansion, notation references,
     * default attribute values). Latter is used in non-validating mode.
     *<p>
     */
    final boolean mCfgFullyValidating;

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

    /**
     * This flag is used to catch uses of PEs in the internal subset
     * within declarations (full declarations are ok, but not other types)
     */
    boolean mCheckForbiddenPEs = false;

    /**
     * Keyword of the declaration being currently parsed (if any). Used
     * for error reporting purposes.
     */
    String mCurrDeclaration;

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
    private FullDTDReader(WstxInputSource input, ReaderConfig cfg,
                          boolean constructFully, boolean xml11)
    {
        this(input, cfg, false, null, constructFully, xml11);
    }

    /**
     * Constructor used for reading external subset.
     */
    private FullDTDReader(WstxInputSource input, ReaderConfig cfg, 
                          DTDSubset intSubset,
                          boolean constructFully, boolean xml11)
    {
        this(input, cfg, true, intSubset, constructFully, xml11);

        // Let's make sure line/col offsets are correct...
        input.initInputLocation(this, mCurrDepth);
    }

    /**
     * Common initialization part of int/ext subset constructors.
     */
    private FullDTDReader(WstxInputSource input, ReaderConfig cfg,
                          boolean isExt, DTDSubset intSubset,
                          boolean constructFully, boolean xml11)
    {
        super(input, cfg, isExt);
        mXml11 = xml11;
        int cfgFlags = cfg.getConfigFlags();
        mConfigFlags = cfgFlags;
        mCfgNormalizeLFs = (cfgFlags & CFG_NORMALIZE_LFS) != 0;
        mCfgNormAttrs = (cfgFlags & CFG_NORMALIZE_ATTR_VALUES) != 0;
        mCfgSupportDTDPP = (cfgFlags & CFG_SUPPORT_DTDPP) != 0;
        mCfgFullyValidating = constructFully;

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
        if (not == null || not.isEmpty()) {
            mPredefdNotations = null;
        } else {
            mPredefdNotations = not;
        }
    }

    /**
     * Method called to read in the internal subset definition.
     */
    public static DTDSubset readInternalSubset(WstxInputData srcData,
                                               WstxInputSource input,
                                               ReaderConfig cfg,
                                               boolean constructFully,
                                               boolean xml11)
        throws IOException, XMLStreamException
    {
        FullDTDReader r = new FullDTDReader(input, cfg, constructFully, xml11);
        /* Need to read using same low-level reader interface:
         */
        r.copyBufferStateFrom(srcData);
        DTDSubset ss;

        try {
            ss = r.parseDTD();
        } finally {
            /* And then need to restore changes back to owner (line nrs etc);
             * effectively means that we'll stop reading external DTD subset,
             * if so.
             */
            srcData.copyBufferStateFrom(r);
        }
        return ss;
    }

    /**
     * Method called to read in the external subset definition.
     */
    public static DTDSubset readExternalSubset
        (WstxInputSource src, ReaderConfig cfg, DTDSubset intSubset, 
         boolean constructFully, boolean xml11)
        throws IOException, XMLStreamException
    {
        FullDTDReader r = new FullDTDReader(src, cfg, intSubset, constructFully, xml11);
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
        ReaderConfig cfg = ReaderConfig.createFullDefaults();
        // Need to create a non-shared copy to populate symbol table field
        cfg = cfg.createNonShared(new SymbolTable());

        /* Let's actually not normalize LFs; it's likely caller wouldn't
         * really want any such changes....
         */
        cfg.clearConfigFlag(CFG_NORMALIZE_LFS);
        cfg.clearConfigFlag(CFG_NORMALIZE_ATTR_VALUES);

        /* Let's assume xml 1.0... can be taken as an arg later on, if we
         * truly care.
         */
        FullDTDReader r = new FullDTDReader(src, cfg, null, true, false);
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
            mCheckForbiddenPEs = false; // PEs are ok at this point
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
                // PEs not allowed within declarations, in the internal subset proper
                mCheckForbiddenPEs = !mIsExternal && (mInput == mRootInput);
                if (mFlattenWriter == null) {
                    parseDirective();
                } else {
                    parseDirectiveFlattened();
                }
                continue;
            }

            if (i == ']') {
                if (mIncludeCount == 0 && !mIsExternal) { // End of internal subset
                    break;
                }
                if (mIncludeCount > 0) { // active INCLUDE block(s) open?
                    boolean suppress = (mFlattenWriter != null) && !mFlattenWriter.includeConditionals();

                    if (suppress) {
                        mFlattenWriter.flush(mInputBuffer, mInputPtr-1);
                        mFlattenWriter.disableOutput();
                    }

                    try {
                        // ]]> needs to be a token, can not come from PE:
                        char c = dtdNextFromCurr();
                        if (c == ']') {
                            c = dtdNextFromCurr();
                            if (c == '>') {
                                // Ok, fine, conditional include section ended.
                                --mIncludeCount;
                                continue;
                            }
                        }
                        throwDTDUnexpectedChar(c, "; expected ']]>' to close conditional include section");
                    } finally {
                        if (suppress) {
                            mFlattenWriter.enableOutput(mInputPtr);
                        }
                    }
                }
                // otherwise will fall through, and give an error
            }

            if (mIsExternal) {
                throwDTDUnexpectedChar(i, "; expected a '<' to start a directive");
            }
            throwDTDUnexpectedChar(i, "; expected a '<' to start a directive, or \"]>\" to end internal subset");
        }

        /* 05-Feb-2006, TSa: Not allowed to have unclosed INCLUDE/IGNORE
         *    blocks...
         */
        if (mIncludeCount > 0) { // active INCLUDE block(s) open?
            String suffix = (mIncludeCount == 1) ? "an INCLUDE block" : (""+mIncludeCount+" INCLUDE blocks");
            throwUnexpectedEOF(getErrorMsg()+"; expected closing marker for "+suffix);
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
                                                 mNotations, mElements,
                                                 mCfgFullyValidating);
        } else {
            /* Internal subsets are not cachable (no unique way to refer
             * to unique internal subsets), and there can be no references
             * to pre-defined PEs, as none were passed.
             */
            ss = DTDSubsetImpl.constructInstance(false, mGeneralEntities, null,
                                                 mParamEntities, null,
                                                 mNotations, mElements,
                                                 mCfgFullyValidating);
        }

        return ss;
    }

    protected void parseDirective()
        throws IOException, XMLStreamException
    {
        /* Hmmh. Don't think PEs are allowed to contain starting
         * '!' (or '?')... and it has to come from the same
         * input source too (no splits)
         */
        char c = dtdNextFromCurr();
        if (c == '?') { // xml decl?
            skimPI();
            return;
        }
        if (c != '!') { // nothing valid
            throwDTDUnexpectedChar(c, "; expected '!' to start a directive");
        }

        /* ignore/include, comment, or directive; we are still getting
         * token from same section though
         */
        c = dtdNextFromCurr();
        if (c == '-') { // plain comment
            c = dtdNextFromCurr();
            if (c != '-') {
                throwDTDUnexpectedChar(c, "; expected '-' for a comment");
            }
            skipComment();
        } else if (c == '[') {
            checkInclusion();
        } else if (c >= 'A' && c <= 'Z') {
            handleDeclaration(c);
        } else {
            throwDTDUnexpectedChar(c, ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
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
        char c = dtdNextFromCurr();
        if (c == '?') { // xml decl?
            mFlattenWriter.enableOutput(mInputPtr);
            mFlattenWriter.output("<?");
            skimPI();
            //throwDTDUnexpectedChar(c, " expected '!' to start a directive");
            return;
        }
        if (c != '!') { // nothing valid
            throwDTDUnexpectedChar(c, ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
        }

        // ignore/include, comment, or directive

        c = dtdNextFromCurr();
        if (c == '-') { // plain comment
            c = dtdNextFromCurr();
            if (c != '-') {
                throwDTDUnexpectedChar(c, "; expected '-' for a comment");
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
                    throwDTDUnexpectedChar(c, ErrorConsts.ERR_DTD_MAINLEVEL_KEYWORD);
                }
            }
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Overridden input handling 
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
        throws IOException, XMLStreamException
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
                throwNullParent(input);
            }
            /* 13-Feb-2006, TSa: Ok, do we violate a proper nesting constraints
             *   with this input block closure?
             */
            if (mCurrDepth != input.getScopeId()) {
                handleIncompleteEntityProblem(input);
            }

            mInput = input = parent;
            input.restoreContext(this);
            if (mFlattenWriter != null) {
                mFlattenWriter.setFlattenStart(mInputPtr);
            }
            mInputTopDepth = input.getScopeId();
            // Maybe there are leftovers from that input in buffer now?
        } while (mInputPtr >= mInputLen);

        return true;
    }

    protected boolean loadMoreFromCurrent()
        throws IOException, XMLStreamException
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

    private void loadMoreScoped(WstxInputSource currScope,
                                String entityName, Location loc)
        throws IOException, XMLStreamException
    {
        boolean check = (mInput == currScope);
        loadMore(getErrorMsg());
        // Did we get out of the scope?
        if (check && (mInput != currScope)) {
            throwParseError("Unterminated entity value for entity '"
                            +entityName+"' (definition started at "
                            +loc+")");
        }
    }
        
    /**
     * @return Next character from the current input block, if any left;
     *    NULL if end of block (entity expansion)
     */
    private char dtdNextIfAvailable()
        throws IOException, XMLStreamException
    {
        char c;
        if (mInputPtr < mInputLen) {
            c = mInputBuffer[mInputPtr++];
        } else {
            int i = peekNext();
            if (i < 0) {
                return CHAR_NULL;
            }
            ++mInputPtr;
            c = (char) i;
        }
        if (c == CHAR_NULL) {
            throwNullChar();
        }
        return c;
    }

    /**
     * @return Number of white space characters skipped
     */
    private int dtdSkipCurrWs()
        throws IOException, XMLStreamException
    {
        int count = 0;

        while (true) {
            char c;
            if (mInputPtr < mInputLen) {
                c = mInputBuffer[mInputPtr];
            } else {
                int i = peekNext();
                if (i < 0) {
                    break;
                }
                c = (char) i;
            }
            if (!isSpaceChar(c)) {
                break;
            }
            ++mInputPtr;
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c != CHAR_SPACE && c != '\t') {
                throwInvalidSpace(c);
            }

            ++count;
        }
        return count;
    }

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

    private char skipDtdWs(boolean handlePEs)
        throws IOException, XMLStreamException
    {
        while (true) {
            char c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
            if (c > CHAR_SPACE) {
                if (c == '%' && handlePEs) {
                    expandPE();
                    continue;
                }
                return c;
            }
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c != CHAR_SPACE && c != '\t') {
                throwInvalidSpace(c);
            }
        }
    }

    /**
     * Note: Apparently a parameter entity expansion does also count
     * as white space (that is, PEs outside of quoted text are considered
     * to be separated by white spaces on both sides). Fortunately this
     * can be handled by 2 little hacks: both a start of a PE, and an
     * end of input block (== end of PE expansion) count as succesful
     * spaces.
     *
     * @return Character following the obligatory boundary (white space
     *   or PE start/end)
     */
    private char skipObligatoryDtdWs()
        throws IOException, XMLStreamException
    {
        /* Ok; since we need at least one space, or a PE, or end of input
         * block, let's do this unique check first...
         */
        int i = peekNext();
        char c;

        if (i == -1) { // just means 'local' EOF (since peek only checks current)
            c = getNextChar(getErrorMsg());
            // Non-space, non PE is ok, due to end-of-block...
            if (c > CHAR_SPACE && c != '%') {
                return c;
            }
        } else {
            c = mInputBuffer[mInputPtr++]; // was peek, need to read
            if (c > CHAR_SPACE && c != '%') {
                throwDTDUnexpectedChar(c, "; expected a separating white space");
            }
        }

        // Ok, got it, now can loop...
        while (true) {
            if (c == '%') {
                expandPE();
            } else if (c > CHAR_SPACE) {
                break;
            } else {
                if (c == '\n' || c == '\r') {
                    skipCRLF(c);
                } else if (c != CHAR_SPACE && c != '\t') {
                    throwInvalidSpace(c);
                }
            }
            /* Now we got one space (or end of input block) -- no need to
             * restrict get next on current block (in case PE ends); happens
             * with xmltest/valid/not-sa/003.xml, for eaxmple.
             */
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(getErrorMsg());
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

        if (mCheckForbiddenPEs) {
            /* Ok; we hit a PE where we should not have (within the internal
             * dtd subset proper, within a declaration). This is a WF error.
             */
            throwForbiddenPE();
        }

        // 01-Jul-2004, TSa: When flattening, need to flush previous output
        if (mFlattenWriter != null) {
            // Flush up to but not including ampersand...
            mFlattenWriter.flush(mInputBuffer, mInputPtr-1);
            mFlattenWriter.disableOutput();
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : dtdNextFromCurr();
            id = readDTDName(c);
            try {
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : dtdNextFromCurr();
            } finally {
                // will ignore name and colon (or whatever was parsed)
                mFlattenWriter.enableOutput(mInputPtr);
            }
        } else {
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : dtdNextFromCurr();
            id = readDTDName(c);
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : dtdNextFromCurr();
        }
        
        // Should now get semicolon...
        if (c != ';') {
            throwDTDUnexpectedChar(c, "; expected ';' to end parameter entity name");
        }

        if (mIsExternal) {
            /* Need more checking when expanding PEs for external subsets;
             * need to see if definition was pre-defined or locally
             * defined (to know if subset will be cacheable)
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
     * has, returns null and points to char after the keyword; if not,
     * returns whatever constitutes a keyword matched, for error
     * reporting purposes.
     */
    protected String checkDTDKeyword(String exp)
        throws IOException, XMLStreamException
    {
        int i = 0;
        int len = exp.length();
        char c = ' ';

        for (; i < len; ++i) {
            if (mInputPtr < mInputLen) {
                c = mInputBuffer[mInputPtr++];
            } else {
                c = dtdNextIfAvailable();
                if (c == CHAR_NULL) { // end of block, fine
                    return exp.substring(0, i);
                }
            }
            if (c != exp.charAt(i)) {
                break;
            }
        }

        if (i == len) {
            // Got a match? Cool... except if identifier still continues...
            c = dtdNextIfAvailable();
            if (c == CHAR_NULL) { // EOB, fine
                return null;
            }
            if (!is11NameChar(c)) {
                --mInputPtr; // to push it back
                return null;
            }
        }
        StringBuffer sb = new StringBuffer(exp.substring(0, i));
        sb.append(c);
        while (true) {
            c = dtdNextIfAvailable();
            if (c == CHAR_NULL) { // EOB, fine
                break;
            }
            if (!is11NameChar(c) && c != ':') {
                --mInputPtr; // to push it back
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Method called usually to indicate an error condition; will read rest
     * of specified keyword (including characters that can be part of XML
     * identifiers), append that to passed prefix (which is optional), and
     * return resulting String.
     *
     * @param prefix Part of keyword already read in.
     */
    protected String readDTDKeyword(String prefix)
        throws IOException, XMLStreamException
    {
        StringBuffer sb = new StringBuffer(prefix);

        while (true) {
            char c;
            if (mInputPtr < mInputLen) {
                c = mInputBuffer[mInputPtr++];
            } else {
                // Don't want to cross block boundary
                c = dtdNextIfAvailable();
                if (c == CHAR_NULL) {
                    break; // end-of-block
                }
            }
            if (!is11NameChar(c) && c != ':') {
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
            if (!is11NameStartChar(c)) {
                throwDTDUnexpectedChar(c, "; expected 'PUBLIC' or 'SYSTEM' keyword");
            }
            errId = readDTDKeyword(String.valueOf(c));
        }

        throwParseError("Unrecognized keyword '"+errId+"'; expected 'PUBLIC' or 'SYSTEM'");
        return false; // never gets here
    }

    private String readDTDName(char c)
        throws IOException, XMLStreamException
    {
        // Let's just check this before trying to parse the id...
        if (!is11NameStartChar(c)) {
            throwDTDUnexpectedChar(c, "; expected an identifier");
        }
        return parseFullName(c);
    }

    private String readDTDLocalName(char c, boolean checkChar)
        throws IOException, XMLStreamException
    {
        /* Let's just check this first, to get better error msg
         * (parseLocalName() will double-check it too)
         */
        if (checkChar && !is11NameStartChar(c)) {
            throwDTDUnexpectedChar(c, "; expected an identifier");
        }
        return parseLocalName(c);
    }

    /**
     * Similar to {@link #readDTDName}, except that the rules are bit looser,
     * ie. there are no additional restrictions for the first char
     */
    private String readDTDNmtoken(char c)
        throws IOException, XMLStreamException
    {
        char[] outBuf = getNameBuffer(64);
        int outLen = outBuf.length;
        int outPtr = 0;

        while (true) {
            /* Note: colon not included in name char array, since it has
             * special meaning WRT QNames, need to add into account here:
             */
            if (!is11NameChar(c)  && c != ':') {
                // Need to get at least one char
                if (outPtr == 0) {
                    throwDTDUnexpectedChar(c, "; expected a NMTOKEN character to start a NMTOKEN");
                }
                --mInputPtr;
                break;
            }
            if (outPtr >= outLen) {
                outBuf = expandBy50Pct(outBuf);
                outLen = outBuf.length;
            }
            outBuf[outPtr++] = c;
            if (mInputPtr < mInputLen) {
                c = mInputBuffer[mInputPtr++];
            } else {
                c = dtdNextIfAvailable();
                if (c == CHAR_NULL) { // end-of-block
                    break;
                }
            }
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
            /* Hmmh. This is tricky; should only read from the current
             * scope, but it is ok to hit end-of-block if it was a PE
             * expansion...
             */
            char c = dtdNextIfAvailable();
            if (c == CHAR_NULL) { // end-of-block
                // ok, that's it...
                prefix = null;
            } else {
                if (c == ':') { // Ok, got namespace and local name
                    prefix = localName;
                    c = dtdNextFromCurr();
                    localName = parseLocalName(c);
                } else {
                    prefix = null;
                    --mInputPtr;
                }
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
         *  (ie. main XML input) file (or to be precise; they are legal
         *  in the int. subset only as complete declarations)
         */
        boolean allowPEs = mIsExternal || (mInput != mRootInput);

        TextBuffer tb = new TextBuffer(EXP_ENTITY_VALUE_LEN);
        tb.resetInitialized();

        char[] outBuf = tb.getCurrentSegment();
        int outPtr = tb.getCurrentSegmentSize();

        while (true) {
            if (mInputPtr >= mInputLen) {
                loadMoreScoped(currScope, id, loc);
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
                } else {
                    /* 11-Feb-2006, TSa: Even so, must verify that the
                     *   entity reference is well-formed.
                     */
                    boolean first = true;
                    while (true) {
                        if (outPtr >= outBuf.length) { // need more room?
                            outBuf = tb.finishCurrentSegment();
                            outPtr = 0;
                        }
                        outBuf[outPtr++] = c; // starting with '&'
                        if (mInputPtr >= mInputLen) {
                            loadMoreScoped(currScope, id, loc);
                        }
                        c = mInputBuffer[mInputPtr++];
                        if (c == ';') {
                            break;
                        }
                        if (first) {
                            first = false;
                            if (is11NameStartChar(c)) {
                                continue;
                            }
                        } else {
                            if (is11NameChar(c)) {
                                continue;
                            }
                        }
                        if (c == ':' && !mCfgNsEnabled) {
                            continue; // fine in non-ns mode
                        }
                        if (first) { // missing name
                            throwDTDUnexpectedChar(c, "; expected entity name after '&'");
                        }
                        throwDTDUnexpectedChar(c, "; expected semi-colon after entity name");
                    }
                    // we can just fall through to let semicolon be added
                }
                // Either '&' itself, or expanded char entity
            } else if (c == '%') { // param entity?
                expandPE();
                // Need to loop over, no char available yet
                continue;
            } else if (c < CHAR_SPACE) {
                if (c == '\n') {
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
                } else if (c != '\t') {
                    throwInvalidSpace(c);
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
            throwDTDUnexpectedChar(c, "; expected closing '>' after ENTITY declaration");
        }
        return tb;
    }

    /**
     * This method is similar to {@link #parseEntityValue} in some ways,
     * but has some notable differences, due to the way XML specs define
     * differences. Main differences are that parameter entities are not
     * allowed (or rather, recognized as entities), and that general
     * entities need to be verified, but NOT expanded right away.
     * Whether forward references are allowed or not is an open question
     * right now.
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
                    } else if (c != CHAR_SPACE && c != '\t') {
                        throwInvalidSpace(c);
                    }
                    if (mCfgNormAttrs) {
                        c = CHAR_SPACE;
                    }
                } else if (c == quoteChar) {
                    /* It is possible to get these via expanded entities;
                     * need to make sure this is the same input level as
                     * the one that had starting quote
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
                            int setId = expandEntity
                                (id, mPredefdGEs, mGeneralEntities, false);
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
                    throwDTDUnexpectedChar(c, SUFFIX_IN_DEF_ATTR_VALUE);
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

    /**
     * Method similar to {@link #skipPI}, but one that does basic
     * well-formedness checks.
     */
    protected void skimPI()
        throws IOException, XMLStreamException
    {
        String target = parseFullName();
        if (target.length() == 0) {
            throwParseError(ErrorConsts.ERR_WF_PI_MISSING_TARGET);
        }
        if (target.equalsIgnoreCase("xml")) {
            throwParseError(ErrorConsts.ERR_WF_PI_MISSING_TARGET, target);
        }

        char c = dtdNextFromCurr();
        // Ok, need a space between target and data nonetheless
        if (!isSpaceChar(c)) { // except if it ends right away
            if (c != '?' || dtdNextFromCurr() != '>') {
                throwUnexpectedChar(c, ErrorConsts.ERR_WF_PI_XML_MISSING_SPACE);
            }
        } else {
            /* Otherwise, not that much to check since we don't care about
             * the contents.
             */
            while (true) {
                c = (mInputPtr < mInputLen)
                    ? mInputBuffer[mInputPtr++] : dtdNextFromCurr();
                if (c == '?') {
                    do {
                        c = (mInputPtr < mInputLen)
                            ? mInputBuffer[mInputPtr++] : dtdNextFromCurr();
                    } while (c == '?');
                    if (c == '>') {
                        break;
                    }
                }
                if (c < CHAR_SPACE) {
                    if (c == '\n' || c == '\r') {
                        skipCRLF(c);
                    } else if (c != '\t') {
                        throwInvalidSpace(c);
                    }
                }
            }
        }
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
        if (!mIsExternal && mInput == mRootInput) {
            throwParseError("Internal DTD subset can not use (INCLUDE/IGNORE) directives (except via external entities)");
        }

        char c = skipDtdWs(true);
        if (c != 'I') {
            // let's obtain the keyword for error reporting purposes:
            keyword = readDTDKeyword(String.valueOf(c));
        } else {
            c = dtdNextFromCurr();
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
        throwParseError("Unrecognized directive '"+keyword+"'; expected either 'IGNORE' or 'INCLUDE'");
    }

    private void handleIncluded()
        throws IOException, XMLStreamException
    {
        char c = skipDtdWs(false);
        if (c != '[') {
            throwDTDUnexpectedChar(c, "; expected '[' to follow 'INCLUDE' directive");
        }
        ++mIncludeCount;
    }

    private void handleIgnored()
        throws IOException, XMLStreamException
    {
        char c = skipDtdWs(false);
        int count = 1; // Nesting of IGNORE/INCLUDE sections we have to match

        if (c != '[') {
            throwDTDUnexpectedChar(c, "; expected '[' to follow 'IGNORE' directive");
        }

        /* Ok; now, let's just skip until we get the closing ']]>'
         */
        String errorMsg = getErrorMsg();
        while (true) {
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(errorMsg);
            if (c < CHAR_SPACE) {
                if (c == '\n' || c == '\r') {
                    skipCRLF(c);
                } else if (c != '\t') {
                    throwInvalidSpace(c);
                }
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

    private void throwForbiddenPE()
        throws WstxException
    {
        throwParseError("Can not have parameter entities in the internal subset, except for defining complete declarations (XML 1.0, #2.8, WFC 'PEs In Internal Subset')");
    }

    private String elemDesc(Object elem) {
        return "Element <"+elem+">)";
    }

    private String attrDesc(Object elem, NameKey attrName) {
        return "Attribute '"+attrName+"' (of element <"+elem+">)";
    }

    private String entityDesc(WstxInputSource input) {
        return "Entity &"+input.getEntityId()+";";
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, main-level declaration parsing:
    /////////////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: c is known to be a letter (from 'A' to 'Z') at this poit.
     */
    private void handleDeclaration(char c)
        throws IOException, XMLStreamException
    {
        String keyw = null;
 
        /* We need to ensure that PEs do not span declaration boundaries
         * (similar to element nesting wrt. GE expansion for xml content).
         * This VC is defined in xml 1.0, section 2.8 as
         * "VC: Proper Declaration/PE Nesting"
         */
        /* We have binary depths within DTDs, for now: since the declaration
         * just started, we should now have 1 as the depth:
         */
        mCurrDepth = 1;

        try {
            do { // dummy loop, for break
                if (c == 'A') { // ATTLIST?
                    keyw = checkDTDKeyword("TTLIST");
                    if (keyw == null) {
                        mCurrDeclaration = "ATTLIST";
                        handleAttlistDecl();
                        break;
                    }
                    keyw = "A" + keyw;
                } else if (c == 'E') { // ENTITY, ELEMENT?
                    c = dtdNextFromCurr();
                    if (c == 'N') {
                        keyw = checkDTDKeyword("TITY");
                        if (keyw == null) {
                            mCurrDeclaration = "ENTITY";
                            handleEntityDecl(false);
                            break;
                        }
                        keyw = "EN" + keyw;
                    } else if (c == 'L') {
                        keyw = checkDTDKeyword("EMENT");
                        if (keyw == null) {
                            mCurrDeclaration = "ELEMENT";
                            handleElementDecl();
                            break;
                        }
                        keyw = "EL" + keyw;
                    } else {
                        keyw = readDTDKeyword("E");
                    }
                } else if (c == 'N') { // NOTATION?
                    keyw = checkDTDKeyword("OTATION");
                    if (keyw == null) {
                        mCurrDeclaration = "NOTATION";
                        handleNotationDecl();
                        break;
                    }
                    keyw = "N" + keyw;
                } else if (c == 'T' && mCfgSupportDTDPP) { // (dtd++ only) TARGETNS?
                    keyw = checkDTDKeyword("ARGETNS");
                    if (keyw == null) {
                        mCurrDeclaration = "TARGETNS";
                        handleTargetNsDecl();
                        break;
                    }
                    keyw = "T" + keyw;
                } else {
                    keyw = readDTDKeyword(String.valueOf(c));
                }

                // If we got this far, we got a problem...
                reportBadDirective(keyw);
            } while (false);
            /* Ok: now, the current input can not have been started
             * within the scope... so:
             */
            if (mInput.getScopeId() > 0) {
                handleGreedyEntityProblem(mInput);
            }

        } finally {
            // Either way, declaration has ended now...
            mCurrDepth = 0;
            mCurrDeclaration = null;
        }
    }

    /**
     * Specialized method that handles potentially suppressable entity
     * declaration. Specifically: at this point it is known that first
     * letter is 'E', that we are outputting flattened DTD info,
     * and that parameter entity declarations are to be suppressed.
     * Furthermore, flatten output is still being disabled, and needs
     * to be enabled by the method at some point.
     */
    private void handleSuppressedDeclaration()
        throws IOException, XMLStreamException
    {
        String keyw;
        char c = dtdNextFromCurr();

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

    /**
     * note: when this method is called, the keyword itself has
     * been succesfully parsed.
     */
    private void handleAttlistDecl()
        throws IOException, XMLStreamException
    {
        /* This method will handle PEs that contain the whole element
         * name. Since it's illegal to have partials, we can then proceed
         * to just use normal parsing...
         */
        char c = skipObligatoryDtdWs();
        final NameKey elemName = readDTDQName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location loc = getStartLocation();

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

                /* 26-Jan-2006, TSa: actually there are edge cases where
                 *   we may get the attribute name right away (esp.
                 *   with PEs...); so let's defer possible error for
                 *   later on. Should not allow missing spaces between
                 *   attribute declarations... ?
                 */
                /*
            } else if (c != '>') {
                throwDTDUnexpectedChar(c, "; excepted either '>' closing ATTLIST declaration, or a white space character separating individual attribute declarations");
                */
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
        char c = skipObligatoryDtdWs();
        final NameKey elemName = readDTDQName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location loc = getStartLocation();

        // Ok; name got, need some white space next
        c = skipObligatoryDtdWs();

        /* Then the content spec: either a special case (ANY, EMPTY), or
         * a parenthesis group for 'real' content spec
         */
        StructValidator val = null;
        int vldContent = XMLValidator.CONTENT_ALLOW_ANY_TEXT;

        if (c == '(') { // real content model
            c = skipDtdWs(true);
            if (c == '#') {
                val = readMixedSpec(elemName, mCfgFullyValidating);
                vldContent = XMLValidator.CONTENT_ALLOW_ANY_TEXT; // checked against DTD
            } else {
                --mInputPtr; // let's push it back...
                ContentSpec spec = readContentSpec(elemName, true, mCfgFullyValidating);
                val = spec.getSimpleValidator();
                if (val == null) {
                    val = new DFAValidator(DFAState.constructDFA(spec));
                }
                vldContent = XMLValidator.CONTENT_ALLOW_WS; // checked against DTD
            }
        } else if (is11NameStartChar(c)) {
            do { // dummy loop to allow break:
                String keyw = null;
                if (c == 'A') {
                    keyw = checkDTDKeyword("NY");
                    if (keyw == null) {
                        val = null;
                        vldContent = XMLValidator.CONTENT_ALLOW_ANY_TEXT; // no DTD checks
                        break;
                    }
                    keyw = "A"+keyw;
                } else if (c == 'E') {
                    keyw = checkDTDKeyword("MPTY");
                    if (keyw == null) {
                        val = EmptyValidator.getPcdataInstance();
                        vldContent = XMLValidator.CONTENT_ALLOW_NONE; // needed to prevent non-elements too
                        break;
                    }
                    keyw = "E"+keyw;
                } else {
                    --mInputPtr;
                    keyw = readDTDKeyword(String.valueOf(c));
                }
                throwParseError("Unrecognized DTD content spec keyword '"
                                +keyw+"' (for element <"+elemName+">); expected ANY or EMPTY");
             } while (false);
        } else {
            throwDTDUnexpectedChar(c, ": excepted '(' to start content specification for element <"+elemName+">");
        }

        // Ok, still need the trailing gt-char to close the declaration:
        c = skipDtdWs(true);
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected '>' to finish the element declaration for <"+elemName+">");
        }

        HashMap m = getElementMap();
        DTDElement oldElem = (DTDElement) m.get(elemName);
        // Ok to have it if it's not 'really' declared

        if (oldElem != null) {
            if (oldElem.isDefined()) { // oops, a problem!
                /* 03-Feb-2006, TSa: Hmmh. Apparently all other XML parsers
                 *    consider it's ok in non-validating mode. All right.
                 */
                if (mCfgFullyValidating) {
                    DTDSubsetImpl.throwElementException(oldElem, loc);
                } else {
                    // let's just ignore re-definition if not validating
                    return;
                }
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
     * This method is tricky to implement, since it can contain parameter
     * entities in multiple combinations... and yet declare one as well.
     *
     * @param suppressPEDecl If true, will need to take of enabling/disabling
     *   of flattened output.
     */
    private void handleEntityDecl(boolean suppressPEDecl)
        throws IOException, XMLStreamException
    {
        /* Hmmh. It seems that PE reference are actually accepted
         * even here... which makes distinguishing definition from
         * reference bit challenging.
         */
        char c = dtdNextFromCurr();
        boolean gotSeparator = false;
        boolean isParam = false;

        while (true) {
            if (c == '%') { // reference?
                char d = dtdNextFromCurr();
                if (isSpaceChar(d)) { // ok, PE declaration
                    isParam = true;
                    if (d == '\n' || c == '\r') {
                        skipCRLF(d);
                    }
                    break;
                }
                // Reference?
                if (!is11NameStartChar(d)) {
                    throwDTDUnexpectedChar(d, "; expected a space (for PE declaration) or PE reference name");
                }
                gotSeparator = true;
                expandPE();
                // need the next char, from the new scope... or if it gets closed, this one
                c = dtdNextChar();
            } else if (!isSpaceChar(c)) { // non-PE entity?
                break;
            } else {
                gotSeparator = true;
                c = dtdNextFromCurr();
            }
        }

        if (!gotSeparator) {
            throwDTDUnexpectedChar(c, "; expected a space separating ENTITY keyword and entity name");
        }

        /* Ok; fair enough: now must have either '%', or a name start
         * character:
         */
        if (isParam) {
            /* PE definition: at this point we already know that there must
             * have been a space... just need to skip the rest, if any
             */
            dtdSkipCurrWs();
            c = dtdNextChar();
        }

        if (suppressPEDecl) { // only if mFlattenWriter != null
            if (!isParam) {
                mFlattenWriter.enableOutput(mInputPtr);
                mFlattenWriter.output("<!ENTITY ");
                mFlattenWriter.output(c);
            }
        }

        // Need a name char, then
        String id = readDTDName(c);

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location evtLoc = getStartLocation();
        EntityDecl ent;

        try {
            c = skipObligatoryDtdWs();
            if (c == '\'' || c == '"') { // internal entity
                /* Let's get the exact location of actual content, not the
                 * opening quote. To do that, need to 'peek' next char, then
                 * push it back:
                 */
                char foo = dtdNextFromCurr();
                Location contentLoc = getLastCharLocation();
                --mInputPtr; // pushback
                TextBuffer contents = parseEntityValue(id, contentLoc, c);
                ent = new IntEntity(evtLoc, id, getSource(),
                                    contents.contentsAsArray(), contentLoc);
            } else {
                if (!is11NameStartChar(c)) {
                    throwDTDUnexpectedChar(c, "; expected either quoted value, or keyword 'PUBLIC' or 'SYSTEM'");
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
            XMLReporter rep = mConfig.getXMLReporter();
            if (rep != null) {
                EntityDecl oldED = (EntityDecl) old;
                String str = " entity '"+id+"' defined more than once: first declaration at "
                    + oldED.getLocation();
                if (isParam) {
                    str = "Parameter" + str;
                } else {
                    str = "General" + str;
                }
                reportProblem(rep, ErrorConsts.WT_ENT_DECL, str, evtLoc, oldED);
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
        char c = skipObligatoryDtdWs();
        String id = readDTDName(c);

        c = skipObligatoryDtdWs();
        boolean isPublic = checkPublicSystemKeyword(c);

        String pubId, sysId;

        c = skipObligatoryDtdWs();

        // Ok, now we can parse the reference; first public id if needed:
        if (isPublic) {
            if (c != '"' && c != '\'') {
                throwDTDUnexpectedChar(c, "; expected a quote to start the public identifier");
            }
            pubId = parsePublicId(c, mCfgNormAttrs, getErrorMsg());
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
                throwDTDUnexpectedChar(c, "; expected a quote to start the system identifier");
            }
            sysId = null;
        }

        // And then we should get the closing '>'
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected closing '>' after NOTATION declaration");
        }

        /* Ok, event needs to know its exact starting point (opening '<'
         * char), let's get that info now (note: data has been preserved
         * earlier)
         */
        Location evtLoc = getStartLocation();
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
        
        char c = skipObligatoryDtdWs();
        String name;
        
        // Explicit namespace name?
        if (is11NameStartChar(c)) {
            name = readDTDLocalName(c, false);
            c = skipObligatoryDtdWs();
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
        c = skipObligatoryDtdWs();

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
                        c = skipObligatoryDtdWs();
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
        c = skipObligatoryDtdWs();
        if (c == '#') {
            String defTypeStr = readDTDName(getNextExpanded());
            if (defTypeStr == "REQUIRED") {
                defType = DTDAttribute.DEF_REQUIRED;
            } else if (defTypeStr == "IMPLIED") {
                defType = DTDAttribute.DEF_IMPLIED;
            } else if (defTypeStr == "FIXED") {
                defType = DTDAttribute.DEF_FIXED;
                c = skipObligatoryDtdWs();
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
                // Just a VC, not WFC... so:
                if (mCfgFullyValidating) {
                    throwDTDAttrError("has type ID; can not have a default (or #FIXED) value (XML 1.0/#3.3.1)",
                                      elem, attrName);
                }
            }
        }

        DTDAttribute attr;

        /* 17-Feb-2006, TSa: Ok. So some (legacy?) DTDs do declare namespace
         *    declarations too... sometimes including default values.
         */
        if (mCfgNsEnabled && attrName.isaNsDeclaration()) { // only check in ns mode
            /* Ok: just declaring them is unnecessary, and can be safely
             * ignored. It's only the default values that matter (and yes,
             * let's not worry about #REQUIRED for now)
             */
            if (defType != DTDAttribute.DEF_DEFAULT
                && defType != DTDAttribute.DEF_FIXED) {
                return;
            }
            // But defaulting... Hmmh.

            attr = elem.addNsDefault(this, attrName, type, defType,
                                     defVal, mCfgFullyValidating);
        } else {
            attr = elem.addAttribute(this, attrName, type, defType,
                                     defVal, enumValues,
                                     mCfgFullyValidating);
        }

        // getting null means this is a dup...
        if (attr == null) {
            // anyone interested in knowing about possible problem?
            XMLReporter rep = mConfig.getXMLReporter();
            if (rep != null) {
                String msg = MessageFormat.format(ErrorConsts.W_DTD_ATTR_REDECL, new Object[] { attrName, elem });
                reportProblem(rep, ErrorConsts.WT_ATTR_DECL, msg, loc, elem);
            }
        } else {
            if (defVal != null) {
                // always normalize
                attr.normalizeDefault();
                // but only validate in validating mode:
                if (mCfgFullyValidating) {
                    attr.validateDefault(this, mCfgNormAttrs);
                }
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
                /* 03-Feb-2006, TSa: Hmmh. Apparently all other XML parsers
                 *    consider it's ok in non-validating mode. All right.
                 */
                if (mCfgFullyValidating) {
                    throwDTDAttrError("Duplicate enumeration value '"+id+"'",
                                      elem, attrName);
                }
            }
        }

        // Ok, let's construct the minimal data struct, then:
        return WordResolver.constructInstance(set);
    }

    /**
     * Method called to read a notation referency entry; done both for
     * attributes of type NOTATION, and for external unparsed entities
     * that refer to a notation. In both cases, notation referenced
     * needs to have been defined earlier; but only if we are building
     * a fully validating DTD subset object (there is the alternative
     * of a minimal DTD in DTD-aware mode, which does no validation
     * but allows attribute defaulting and normalization, as well as
     * access to entity and notation declarations).
     */
    private String readNotationEntry(char c, NameKey attrName)
        throws IOException, XMLStreamException
    {
        String id = readDTDName(c);

        if (mPredefdNotations != null) {
            NotationDecl decl = (NotationDecl) mPredefdNotations.get(id);
            if (decl != null) {
                mUsesPredefdNotations = true;
                return decl.getName();
            }
        }

        NotationDecl decl = (mNotations == null) ? null :
            (NotationDecl) mNotations.get(id);
        if (decl == null) {
            // In validating mode, this is a problem:
            if (mCfgFullyValidating) {
                String msg = "Notation '"+id+"' not defined; ";
                if (attrName == null) { // reference from entity
                    throwDTDError(msg+"can not refer to from an entity");
                }
                // reference from attribute
                throwDTDError(msg+"can not be used as value for attribute list of '"+attrName+"'");
            } else { // but in non-validating, it is not...
                return id;
            }
        }

        return decl.getName();
    }

    private String readEnumEntry(char c, HashMap sharedEnums)
        throws IOException, XMLStreamException
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
     *
     * @param construct If true, will build full object for validating content
     *   within mixed content model; if false, will just parse and discard
     *   information (done in non-validating DTD-supporting mode)
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
            } else {
                throwDTDUnexpectedChar(c, "; expected either '|' to separate elements, or ')' to close the list");
            }
            NameKey n = readDTDQName(c);
            Object old = m.put(n, TokenContentSpec.construct(' ', n));
            if (old != null) {
                /* 03-Feb-2006, TSa: Hmmh. Apparently all other XML parsers
                 *    consider it's ok in non-validating mode. All right.
                 */
                if (mCfgFullyValidating) {
                    throwDTDElemError("duplicate child element <"+n+"> in mixed content model",
                                      elemName);
                }
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
            return EmptyValidator.getPcdataInstance();
        }
        ContentSpec spec = ChoiceContentSpec.constructMixed(mCfgNsEnabled, m.values());
        StructValidator val = spec.getSimpleValidator();
        if (val == null) {
            DFAState dfa = DFAState.constructDFA(spec);
            val = new DFAValidator(dfa);
        }
        return val;
    }

    private ContentSpec readContentSpec(NameKey elemName, boolean mainLevel,
                                        boolean construct)
        throws IOException, XMLStreamException
    {
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
     * @param isParam True if this a parameter entity declaration; false
     *    if general entity declaration
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
            c = skipObligatoryDtdWs();
            if (c != '"' && c != '\'') {
                throwDTDUnexpectedChar(c, "; expected a quote to start the public identifier");
            }
            pubId = parsePublicId(c, mCfgNormAttrs, getErrorMsg());
            /* 30-Sep-2005, TSa: SGML has public ids that miss the system
             *   id. Although not legal with XML DTDs, let's give bit more
             *   meaningful error in those cases...
             */
            c = getNextExpanded();
            if (c <= CHAR_SPACE) { // good
                c = skipDtdWs(true);
            } else { // not good...
                // Let's just push it back and generate normal error then:
                if (c != '>') { // this is handled below though
                    --mInputPtr;
                    c = skipObligatoryDtdWs();
                }
            }

            /* But here let's deal with one case that we are familiar with:
             * SGML does NOT require system id after public one...
             */
            if (c == '>') {
                throwDTDError("Unexpected end of ENTITY declaration (expected a system id after public id): trying to use an SGML DTD instead of XML one?");
            }
        } else {
            // Just need some white space here
            c = skipObligatoryDtdWs();
        }
        if (c != '"' && c != '\'') {
            throwDTDUnexpectedChar(c, "; expected a quote to start the system identifier");
        }
        String sysId = parseSystemId(c, mCfgNormalizeLFs, getErrorMsg());

        // Ok; how about notation?
        String notationId = null;

        /* Ok; PEs are simpler, as they always are parsed (see production
         *   #72 in xml 1.0 specs)
         */
        if (isParam) {
            c = skipDtdWs(true);
        } else {
            /* GEs can be unparsed, too, so it's bit more complicated;
             * if we get '>', don't need space; otherwise need separating
             * space (or PE boundary). Thus, need bit more code.
             */
            int i = peekNext();
            if (i == '>') { // good
                c = '>';
                ++mInputPtr;
            } else if (i < 0) { // local EOF, ok
                c = skipDtdWs(true);
            } else if (i == '%') {
                c = getNextExpanded();
            } else {
                ++mInputPtr;
                c = (char) i;
                if (!isSpaceChar(c)) {
                    throwDTDUnexpectedChar(c, "; expected a separating space or closing '>'");
                }
                c = skipDtdWs(true);
            }

            if (c != '>') {
                if (!is11NameStartChar(c)) {
                    throwDTDUnexpectedChar(c, "; expected either NDATA keyword, or closing '>'");
                }
                String keyw = checkDTDKeyword("DATA");
                if (keyw != null) {
                    throwParseError("Unrecognized keyword '"+keyw+"'; expected NOTATION (or closing '>')");
                }
                c = skipObligatoryDtdWs();
                notationId = readNotationEntry(c, null);
                c = skipDtdWs(true);
            }
        }

        // Ok, better have '>' now:
        if (c != '>') {
            throwDTDUnexpectedChar(c, "; expected closing '>'");
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

    /*
    ///////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////
     */

    private void reportProblem(XMLReporter rep, String probType, String msg,
                               Location loc, Object extraArg)
        throws XMLStreamException
    {
        if (rep != null) {
            rep.report(msg, probType, extraArg, loc);
        }
    }

    // @Override
    /**
     * Handling of PE matching problems is actually intricate; one type
     * will be a WFC ("PE Between Declarations", which refers to PEs that
     * start from outside declarations), and another just a VC
     * ("Proper Declaration/PE Nesting", when PE is contained within
     * declaration)
     */
    protected void handleIncompleteEntityProblem(WstxInputSource closing)
        throws XMLStreamException
    {
        // Did it start outside of declaration?
        if (closing.getScopeId() == 0) { // yup
            // and being WFC, need not be validating
            throwDTDError(entityDesc(closing) + ": "
                          +"Incomplete PE: has to fully contain a declaration (as per xml 1.0.3, section 2.8, WFC 'PE Between Declarations')");
        } else {
            // whereas the other one is only sent in validating mode..
            if (mCfgFullyValidating) {
                throwDTDError(entityDesc(closing) + ": "
                              +"Incomplete PE: has to be fully contained in a declaration (as per xml 1.0.3, section 2.8, VC 'Proper Declaration/PE Nesting')");
            }
        }
    }

    protected void handleGreedyEntityProblem(WstxInputSource input)
        throws XMLStreamException
    {
        // Here it can only be of VC kind...
        if (mCfgFullyValidating) { // since it's a VC, not WFC
            throwDTDError(entityDesc(input) + ": " + 
                          "Unbalanced PE: has to be fully contained in a declaration (as per xml 1.0.3, section 2.8, VC 'Proper Declaration/PE Nesting')");
        }
    }
}

