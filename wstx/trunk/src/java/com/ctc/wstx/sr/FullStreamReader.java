/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
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

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.util.ExceptionUtil;
import com.ctc.wstx.util.URLUtil;

/**
 * Implementation of {@link XMLStreamReader} that builds on
 * {@link WstxStreamReader}, but adds full DTD-handling, including
 * DTD validation
 */
public class FullStreamReader
    extends WstxStreamReader
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
    DTDSubset mDTD = null;

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

    private FullStreamReader(BranchingReaderSource input, ReaderCreator owner,
                             ReaderConfig cfg, InputElementStack elemStack)
        throws IOException, XMLStreamException
    {
        super(input, owner, cfg, elemStack);
    }

    /**
     * Factory method for constructing readers.
     *
     * @param owner "Owner" of this reader, factory that created the reader;
     *   needed for returning updated symbol table information after parsing.
     * @param input Input source used to read the XML document.
     * @param cfg Object that contains reader configuration info.
     */
    public static FullStreamReader createFullStreamReader
        (BranchingReaderSource input, ReaderCreator owner,
         ReaderConfig cfg, InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
        InputElementStack elemStack;
        if (!cfg.willValidateWithDTD()) {
            elemStack = WstxStreamReader.createElementStack(cfg);
        } else {
            boolean normAttrs = cfg.willNormalizeAttrValues();
            if (cfg.willSupportNamespaces()) {
                elemStack = new VNsInputElementStack(16, sPrefixXml, sPrefixXmlns, normAttrs);
            } else {
                elemStack = new VNonNsInputElementStack(16, normAttrs);
            }
        }

        FullStreamReader sr = new FullStreamReader(input, owner, cfg, elemStack);
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
        // Need to have full info...
        if (mStTokenUnfinished) {
            try { finishToken(); } catch (Exception ie) {
                throwLazyError(ie);
            }
        }

        // DTD-specific properties...
        if (mCurrToken == DTD && mDTD != null) {
            if (name.equals(STAX_PROP_ENTITIES)) {
                return mDTD.getGeneralEntityList();
            }
            if (name.equals(STAX_PROP_NOTATIONS)) {
                return mDTD.getNotationList();
            }
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
            IllegalArgumentException iae;
            try {
                mDTD = setFeatureDTDOverride(value);
                mDTDOverridden = true;
            } catch (Throwable t) { // io/wstx exceptions
                ExceptionUtil.throwAsIllegalArgument(t);
            }
        } else {
            super.setFeature(name, value);
        }
    }

    /**
     * Actual method for setting override for DOCTYPE declaration override.
     */
    protected final DTDSubset setFeatureDTDOverride(Object value)
        throws IOException, XMLStreamException
    {
        // First, null indicates "ignore possible DOCTYPE declaration"
        if (value == null) {
            return null;
        }
        
        boolean cache = hasConfigFlags(CFG_CACHE_DTDS);
        DTDId id = null;
        
        /* Then did we get a StreamSource or URL? These types can
         * be used with cachable DTDs...
         */
        if (cache) {
            if (value instanceof StreamSource) {
                StreamSource ss = (StreamSource) value;
                id = constructDtdId(ss.getPublicId(), ss.getSystemId());
            } else if (value instanceof URL) {
                id = constructDtdId((URL) value);
            }
            // If there's a suitable id, we may be able to find it from cache...
            if (id != null) {
                DTDSubset ss = findCachedSubset(id, null);
                if (ss != null) {
                    return ss;
                }
            }
        }

        // Ok, no usable cached subset found, need to (try to) read it:
        WstxInputSource src = DefaultInputResolver.sourceFrom(mInput, null, value, mReporter);
        return mConfig.getDtdReader().readExternalSubset(this, src, mConfig, null);
    }

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    public Object getProcessedDTD() {
        return mDTD;
    }

    /*
    ////////////////////////////////////////////////////
    // Extended (non-StAX) public API:
    ////////////////////////////////////////////////////
     */

    /*
    public void setDTDOverride(String pubId, String sysId)
        throws IOException, XMLStreamException
    {
        mDTD = findDtdExtSubset(pubId, sysId, null);
    }
    */

    /*
    public void setDTDOverride(String pubId, URL source)
        throws IOException, XMLStreamException
    {
        setDTDOverride(pubId, source.toExternalForm());
    }
    */

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
                intSubset = mConfig.getDtdReader().readInternalSubset(this, mInput, mConfig);
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
        DTDSubset combo;

        /* 19-Sep-2004, TSa: That does not need to be done, however, if
         *    there's a DTD override set.
         */
        if (mDTDOverridden) {
            // We have earlier override that's already parsed
            combo = mDTD;
        } else {
            // Nope, no override
            DTDSubset extSubset = (mDtdPublicId != null || mDtdSystemId != null) ?
                findDtdExtSubset(mDtdPublicId, mDtdSystemId, intSubset) : null;
            
            if (intSubset == null) {
                combo = extSubset;
            } else if (extSubset == null) {
                combo = intSubset;
            } else {
                combo = intSubset.combineWithExternalSubset(this, extSubset);
            }
            
            mDTD = combo;
        }

        if (combo == null) { // only if specifically overridden not to have any
            mGeneralEntities = null;
        } else {
            mGeneralEntities = combo.getGeneralEntityMap();
            if (hasConfigFlags(CFG_VALIDATE_AGAINST_DTD)) {
                mElementStack.setElementSpecs(combo.getElementMap(), mSymbols,
                                              mCfgNormalizeAttrs, mGeneralEntities);
            }
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

        URL sysRef = dtdId.getSystemId();

        // No useful cached copy? Need to read it then:
            
        /* For now, we do require system identifier; otherwise we don't
         * know how to resolve DTDs by public id. In future should
         * probably also have some simple catalog resolving facility?
         */
        if (sysId == null) {
            throwParseError("Can not resolve DTD with public id '"
                            +mDtdPublicId+"'; missing system identifier.");
        }
        
        if (sysRef == null) {
            sysRef = resolveExtSubsetPath(sysId);
        }

        WstxInputSource src = null;

        try {
            /* null -> not an entity expansion, no name.
             * Note, too, that we can NOT just pass mEntityResolver, since
             * that's the one used for general entities, whereas ext subset
             * should be resolved by the param entity resolver.
             */
            src = DefaultInputResolver.resolveEntity
                (mInput, null, pubId, sysId, mConfig.getDtdResolver(),
                 mReporter);
        } catch (FileNotFoundException fex) {
            /* Let's catch and rethrow this just so we get more meaningful
             * description (with input source position etc)
             */
            throwParseError("(was "+fex.getClass().getName()+") "+fex.getMessage());
        }

        DTDSubset extSubset = mConfig.getDtdReader().readExternalSubset(this, src, mConfig, intSubset);
        
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
              * is less if so (no need to store content specs for one)
              */
             | CFG_VALIDATE_AGAINST_DTD
             /* Also, whether we support dtd++ or not may change construction
              * of settings... (currently does not, but could)
              */
             | CFG_SUPPORT_DTDPP
             );
        URL sysRef = (sysId == null || pubId.length() == 0) ? null :
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
        throws WstxException
    {
        switch (mVldContent) {
        case CONTENT_ALLOW_NONE:
            throwParseError(ErrorConsts.ERR_VLD_EMPTY,
                            mElementStack.getTopElementDesc(),
                            ErrorConsts.tokenTypeDesc(evtType));
            break;
        case CONTENT_ALLOW_NON_MIXED:
            throwParseError(ErrorConsts.ERR_VLD_NON_MIXED,
                            mElementStack.getTopElementDesc());
            break;
        case CONTENT_ALLOW_DTD_ANY:
            /* Not 100% sure if this should ever happen... depends on
             * interpretation of 'any' content model?
             */
            throwParseError(ErrorConsts.ERR_VLD_ANY,
                            mElementStack.getTopElementDesc(),
                            ErrorConsts.tokenTypeDesc(evtType));
            break;
        default: // should never occur:
            throwParseError("Internal error: trying to report invalid content for "+evtType);
        }
    }
}
