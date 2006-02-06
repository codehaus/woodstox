/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.sr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.codehaus.stax2.validation.DTDValidationSchema;
import org.codehaus.stax2.validation.ValidationContext;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.dtd.DTDValidator;
import com.ctc.wstx.dtd.FullDTDReader;
import com.ctc.wstx.util.ExceptionUtil;
import com.ctc.wstx.util.URLUtil;

/**
 * Implementation of {@link XMLStreamReader} that builds on
 * {@link BasicStreamReader}, but adds full DTD-handling, including
 * DTD validation
 */
public class ValidatingStreamReader
    extends BasicStreamReader
{
    /*
    ////////////////////////////////////////////////
    // Constants for standard StAX properties:
    ////////////////////////////////////////////////
    */

    final static String STAX_PROP_ENTITIES = "javax.xml.stream.entities";

    final static String STAX_PROP_NOTATIONS = "javax.xml.stream.notations";

    /*
    ////////////////////////////////////////////////////
    // DTD information (entities, ...)
    ////////////////////////////////////////////////////
     */

    // // // Note: some members that logically belong here, are actually
    // // // part of superclass

    /**
     * Combined DTD set, constructed from parsed internal and external
     * entities (which may have been set via override DTD functionality).
     */
    DTDValidationSchema mDTD = null;

    /**
     * Flag to indicate that the DOCTYPE declaration is to be
     * overridden by value of {@link #mDTD}. Mostly needed to signal
     * "significant null" value, which means "just discard DOCTYPE if one
     * gotten from the document".
     */
    boolean mDTDOverridden = false;

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    private ValidatingStreamReader(BranchingReaderSource input, ReaderCreator owner,
                                   ReaderConfig cfg, InputElementStack elemStack,
                                   boolean forER)
        throws IOException, XMLStreamException
    {
        super(input, owner, cfg, elemStack, forER);
    }

    /**
     * Factory method for constructing readers.
     *
     * @param owner "Owner" of this reader, factory that created the reader;
     *   needed for returning updated symbol table information after parsing.
     * @param input Input source used to read the XML document.
     * @param cfg Object that contains reader configuration info.
     * @param bs Bootstrapper to use, for reading xml declaration etc.
     * @param forER True if this reader is to be (configured to be) used by
     *   an event reader. Will cause some changes to default settings, as
     *   required by contracts Woodstox XMLEventReader implementation has
     *   (with respect to lazy parsing, short text segments etc)
     */
    public static ValidatingStreamReader createValidatingStreamReader
        (BranchingReaderSource input, ReaderCreator owner,
         ReaderConfig cfg, InputBootstrapper bs, boolean forER)
        throws IOException, XMLStreamException
    {
        ValidatingStreamReader sr = new ValidatingStreamReader
            (input, owner, cfg, createElementStack(cfg), forER);
        sr.initProlog(bs);
        return sr;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        // DTD-specific properties...
        if (name.equals(STAX_PROP_ENTITIES)) {
            safeEnsureFinishToken();
            if (mDTD == null || !(mDTD instanceof DTDSubset)) {
                return null;
            }
            return ((DTDSubset) mDTD).getGeneralEntityList();
        }
        if (name.equals(STAX_PROP_NOTATIONS)) {
            safeEnsureFinishToken();
            if (mDTD == null || !(mDTD instanceof DTDSubset)) {
                return null;
            }
            return ((DTDSubset) mDTD).getNotationList();
        }
        return super.getProperty(name);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader2 (StAX2) implementation
    ////////////////////////////////////////////////////
     */

    // // // StAX2, per-reader configuration

    // no additional readable features
    //public Object getFeature(String name)

    public void setFeature(String name, Object value)
    {
        // Referring to DTD-related features?
        if (name.equals(FEATURE_DTD_OVERRIDE)) {
            mDTDOverridden = true;
            // null is ok, basically means "never use a DTD"...
            if (value != null && !(value instanceof XMLValidationSchema)) {
                throw new IllegalArgumentException("Value to set for feature "+name+" not of type XMLValidationSchema");
            }
            mDTD = (DTDValidationSchema) value;
        } else {
            super.setFeature(name, value);
        }
    }

    // Nothing to override from the base class, for these methods:
    /*
    public boolean isPropertySupported(String name)
    {
    }

    public void setProperty(String name, Object value)
    {
    }
    */

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    public Object getProcessedDTD() {
        return mDTD;
    }

    public DTDValidationSchema getProcessedDTDSchema() {
        return mDTD;
    }

    /*
    ////////////////////////////////////////////////////
    // Private methods, DOCTYPE handling
    ////////////////////////////////////////////////////
     */

    /**
     * This method gets called to handle remainder of DOCTYPE declaration,
     * essentially the optional internal subset. Internal subset, if such
     * exists, is always read, but whether its contents are added to the
     * read buffer depend on passed-in argument.
     *<p>
     * NOTE: Since this method overrides the default implementation, make
     * sure you do NOT change the method signature.
     *
     * @param copyContents If true, will copy contents of the internal
     *   subset of DOCTYPE declaration
     *   in the text buffer (in addition to parsing it for actual use); if
     *   false, will only do parsing.
     */
    protected void finishDTD(boolean copyContents)
        throws IOException, XMLStreamException
    {
        if (!hasConfigFlags(CFG_SUPPORT_DTD)) {
            super.finishDTD(copyContents);
            return;
        }

        /* We know there are no spaces, as this char was read and pushed
         * back earlier...
         */
        char c = getNextChar(SUFFIX_IN_DTD);
        DTDSubset intSubset = null;

        /* Do we have an internal subset? Note that we have earlier checked
         * that it has to be either '[' or closing '>'.
         */
        if (c == '[') {
            // Do we need to copy the contents of int. subset in the buffer?
            if (copyContents) {
                ((BranchingReaderSource) mInput).startBranch(mTextBuffer, mInputPtr, mCfgNormalizeLFs);
            }

            try {
                intSubset = FullDTDReader.readInternalSubset(this, mInput, mConfig,
                                                             hasConfigFlags(CFG_VALIDATE_AGAINST_DTD));
            } finally {
                /* Let's close branching in any and every case (may allow
                 * graceful recovery in error cases in future
                 */
                if (copyContents) {
                    /* Need to "push back" ']' got in the succesful case
                     * (that's -1 part below);
                     * in error case it'll just be whatever last char was.
                     */
                    ((BranchingReaderSource) mInput).endBranch(mInputPtr-1);
                }
            }

            // And then we need closing '>'
            c = getNextCharAfterWS(SUFFIX_IN_DTD_INTERNAL);
        }

        if (c != '>') {
            throwUnexpectedChar(c, "; expected '>' to finish DOCTYPE declaration.");
        }

        /* But, then, we also may need to read the external subset, if
         * one was defined:
         */

        /* 19-Sep-2004, TSa: That does not need to be done, however, if
         *    there's a DTD override set.
         */
        if (mDTDOverridden) {
            // We have earlier override that's already parsed
        } else {
            // Nope, no override
            DTDSubset extSubset = (mDtdPublicId != null || mDtdSystemId != null) ?
                findDtdExtSubset(mDtdPublicId, mDtdSystemId, intSubset) : null;
            
            if (intSubset == null) {
                mDTD = extSubset;
            } else if (extSubset == null) {
                mDTD = intSubset;
            } else {
                mDTD = intSubset.combineWithExternalSubset(this, extSubset);
            }
        }

        if (mDTD == null) { // only if specifically overridden not to have any
            mGeneralEntities = null;
        } else {
            if (mDTD instanceof DTDSubset) {
                mGeneralEntities = ((DTDSubset) mDTD).getGeneralEntityMap();
            } else {
                /* Also, let's warn if using non-native DTD implementation,
                 * since entities and notations can not be accessed
                 */
                doReportProblem(mConfig.getXMLReporter(), ErrorConsts.WT_DT_DECL,
                                "Value to set for feature "+FEATURE_DTD_OVERRIDE+" not a native Woodstox DTD implementation (but "+mDTD.getClass()+"): can not access full entity or notation information", null);
            }
            /* 16-Jan-2006, TSa: Actually, we have both fully-validating mode,
             *   and non-validating-but-DTD-aware mode. In latter case, we'll
             *   still need to add a validator, but just to get type info
             *   and to add attribute default values if necessary.
             */
            //if (hasConfigFlags(CFG_VALIDATE_AGAINST_DTD))
            XMLValidator vld = mDTD.createValidator(/*(ValidationContext)*/ mElementStack);
            if (vld instanceof DTDValidator) {
                ((DTDValidator) vld).setAttrValueNormalization(mCfgNormalizeAttrs);
            }
            mElementStack.setValidator(vld);
        }
    }

    /**
     * Method called right before handling the root element, by the base
     * class. This allows for some initialization and checks to be done
     * (not including ones that need access to actual element name)
     */
    protected void initValidation()
        throws XMLStreamException
    {
        if (hasConfigFlags(CFG_VALIDATE_AGAINST_DTD)
            && !mElementStack.hasDTDValidator()) {
            /* It's ok to miss it, but it may not be what caller wants. Either
             * way, let's pass the info and continue
             */
            reportProblem(ErrorConsts.WT_DT_DECL, ErrorConsts.W_MISSING_DTD);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Private methods, external subset access
    ////////////////////////////////////////////////////
     */

    /**
     * Method called by <code>finishDTD</code>, to locate the specified
     * external DTD subset. Subset may be obtained from a cache, if cached
     * copy exists and is compatible; if not, it will be read from the
     * source identified by the public and/or system identifier passed.
     */
    private DTDSubset findDtdExtSubset(String pubId, String sysId,
                                       DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        boolean cache = hasConfigFlags(CFG_CACHE_DTDS);
        DTDId dtdId = constructDtdId(pubId, sysId);

        if (cache) {
            DTDSubset extSubset = findCachedSubset(dtdId, intSubset);
            if (extSubset != null) {
                return extSubset;
            }
        }

        // No useful cached copy? Need to read it then.
        /* For now, we do require system identifier; otherwise we don't
         * know how to resolve DTDs by public id. In future should
         * probably also have some simple catalog resolving facility?
         */
        if (sysId == null) {
            throwParseError("Can not resolve DTD with public id '"
                            +mDtdPublicId+"'; missing system identifier.");
        }
        WstxInputSource src = null;

        try {
            /* null -> not an entity expansion, no name.
             * Note, too, that we can NOT just pass mEntityResolver, since
             * that's the one used for general entities, whereas ext subset
             * should be resolved by the param entity resolver.
             */
            String xmlVersion = mDocXmlVersion;
            /* 05-Feb-2006, TSa: If xmlVersion not explicitly known, it defaults
             *    to 1.0
             */
            if (xmlVersion == null) {
                xmlVersion = XmlConsts.XML_V_10;
            }
            src = DefaultInputResolver.resolveEntity
                (mInput, null, pubId, sysId, mConfig.getDtdResolver(),
                 mConfig.getXMLReporter(), xmlVersion);
        } catch (FileNotFoundException fex) {
            /* Let's catch and rethrow this just so we get more meaningful
             * description (with input source position etc)
             */
            throwParseError("(was "+fex.getClass().getName()+") "+fex.getMessage());
        }

        DTDSubset extSubset = FullDTDReader.readExternalSubset(src, mConfig, intSubset,
                                                               hasConfigFlags(CFG_VALIDATE_AGAINST_DTD));
        
        if (cache) {
            /* Ok; can be cached, but only if it does NOT refer to
             * parameter entities defined in the internal subset (if
             * it does, there's no easy/efficient to check if it could
             * be used later on, plus it's unlikely it could be)
             */
            if (extSubset.isCachable()) {
                mOwner.addCachedDTD(dtdId, extSubset);
            }
        }

        return extSubset;
    }

    private DTDSubset findCachedSubset(DTDId id, DTDSubset intSubset)
        throws XMLStreamException
    {
        DTDSubset extSubset = mOwner.findCachedDTD(id);
        /* Ok, now; can use the cached copy iff it does not refer to
         * any parameter entities internal subset (if one exists)
         * defines:
         */
        if (extSubset != null) {
            if (intSubset == null || extSubset.isReusableWith(intSubset)) {
                return extSubset;
            }
        }
        return null;
    }

    /**
     * Method called to resolve path to external DTD subset, given
     * system identifier.
     */
    private URL resolveExtSubsetPath(String systemId)
        throws IOException
    {
        // Do we have a context to use for resolving?
        URL ctxt = (mInput == null) ? null : mInput.getSource();

        /* Ok, either got a context or not; let's create the URL based on
         * the id, and optional context:
         */
        if (ctxt == null) {
            /* Call will try to figure out if system id has the protocol
             * in it; if not, create a relative file, if it does, try to
             * resolve it.
             */
            return URLUtil.urlFromSystemId(systemId);
        }
        return URLUtil.urlFromSystemId(systemId, ctxt);
    }

    protected DTDId constructDtdId(String pubId, String sysId)
        throws IOException
    {
        /* Following settings will change what gets stored as DTD, so
         * they need to separate cached instances too:
         */
        int significantFlags = mConfigFlags &
            (CFG_NAMESPACE_AWARE
             | CFG_NORMALIZE_LFS | CFG_NORMALIZE_ATTR_VALUES
             /* Let's optimize non-validating case; DTD info we need
              * is less if so (no need to store content specs for one)...
              * plus, eventual functionality may be different too.
              */
             | CFG_VALIDATE_AGAINST_DTD
             /* Also, whether we support dtd++ or not may change construction
              * of settings... (currently does not, but could)
              */
             | CFG_SUPPORT_DTDPP
             );
        URL sysRef = (sysId == null || sysId.length() == 0) ? null :
            resolveExtSubsetPath(sysId);
        
        if (pubId != null && pubId.length() > 0) {
            return DTDId.construct(pubId, sysRef, significantFlags);
        }
        if (sysRef == null) {
            return null;
        }
        return DTDId.constructFromSystemId(sysRef, significantFlags);
    }

    protected DTDId constructDtdId(URL sysId)
        throws IOException
    {
        int significantFlags = mConfigFlags &
            (CFG_NAMESPACE_AWARE
             | CFG_NORMALIZE_LFS | CFG_NORMALIZE_ATTR_VALUES
             /* Let's optimize non-validating case; DTD info we need
              * is less if so (no need to store content specs for one)
              */
             | CFG_VALIDATE_AGAINST_DTD
             /* Also, whether we support dtd++ or not may change construction
              * of settings... (currently does not, but could)
              */
             | CFG_SUPPORT_DTDPP
             );
        return DTDId.constructFromSystemId(sysId, significantFlags);
    }

    /*
    ////////////////////////////////////////////////////
    // Private methods, DTD validation support
    ////////////////////////////////////////////////////
     */

    /**
     * Method called by lower-level parsing code when invalid content
     * (anything inside element with 'empty' content spec; text inside
     * non-mixed element etc) is found during basic scanning. Note
     * that actual DTD element structure problems are not reported
     * through this method.
     */
    protected void reportInvalidContent(int evtType)
        throws XMLStreamException
    {
        switch (mVldContent) {
        case XMLValidator.CONTENT_ALLOW_NONE:
            reportValidationProblem(ErrorConsts.ERR_VLD_EMPTY,
                                    mElementStack.getTopElementDesc(),
                                    ErrorConsts.tokenTypeDesc(evtType));
            break;
        case XMLValidator.CONTENT_ALLOW_WS:
            reportValidationProblem(ErrorConsts.ERR_VLD_NON_MIXED,
                                    mElementStack.getTopElementDesc());
            break;
        case XMLValidator.CONTENT_ALLOW_VALIDATABLE_TEXT:
        case XMLValidator.CONTENT_ALLOW_ANY_TEXT:
            /* Not 100% sure if this should ever happen... depends on
             * interpretation of 'any' content model?
             */
            reportValidationProblem(ErrorConsts.ERR_VLD_ANY,
                                    mElementStack.getTopElementDesc(),
                                    ErrorConsts.tokenTypeDesc(evtType));
            break;
        default: // should never occur:
            throwParseError("Internal error: trying to report invalid content for "+evtType);
        }
    }
}
