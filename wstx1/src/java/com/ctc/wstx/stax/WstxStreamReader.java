/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser GPL), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.stax.cfg.ErrorConsts;
import com.ctc.wstx.stax.cfg.ReaderConfig;
import com.ctc.wstx.stax.dtd.DTDId;
import com.ctc.wstx.stax.dtd.DTDSubset;
import com.ctc.wstx.stax.exc.WstxException;
import com.ctc.wstx.stax.io.BranchingReaderSource;
import com.ctc.wstx.stax.io.DefaultInputResolver;
import com.ctc.wstx.stax.io.InputBootstrapper;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.stax.ns.InputElementStack;
import com.ctc.wstx.stax.ns.VNonNsInputElementStack;
import com.ctc.wstx.stax.ns.VNsInputElementStack;
import com.ctc.wstx.stax.stream.BasicStreamReader;
import com.ctc.wstx.stax.stream.ReaderCreator;
import com.ctc.wstx.util.URLUtil;

/**
 * Implementation of {@link XMLStreamReader}; specialized at efficiency,
 * including both space and time efficiency, while trying to achieve
 * reasonable (if not 100%) compatibility with XML specs. Further,
 * where there may be good reasons to disable some strict XML processing
 * or validation (such as white space and linefeed normalization),
 * options are offered to let application decide whether to do strict
 * or loose processing.
 *<p>
 * Some notes about non-conformancy with XML specs:
 * <ul>
 *  <li>White space recognition is simplified; everything below unicode
 *     0x0020 (including 0x0020, space itself), is considered valid
 *     whitespace, except for null char.
 *     Extra linefeed chars XML 1.1 adds are not recognized.
 *   </li>
 *  <li>Content characters are not restricted to only legal XML 1.0 characters;
 *    all Unicode chars except for markup (and null char) is passed through.
 *   </li>
 * </ul>
 */
public class WstxStreamReader
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
     * entities.
     */
    DTDSubset mDTD = null;

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

    private WstxStreamReader(BranchingReaderSource input, ReaderCreator owner,
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
    public static WstxStreamReader createWstxStreamReader
        (BranchingReaderSource input, ReaderCreator owner, ReaderConfig cfg,
         InputBootstrapper bs)
      throws IOException, XMLStreamException
    {
        InputElementStack elemStack;
        if (!cfg.willValidateWithDTD()) {
            elemStack = BasicStreamReader.createElementStack(cfg);
        } else {
            if (cfg.willSupportNamespaces()) {
                elemStack = new VNsInputElementStack(16, sPrefixXml, sPrefixXmlns);
            } else {
                elemStack = new VNonNsInputElementStack(16);
            }
        }

        WstxStreamReader sr = new WstxStreamReader(input, owner, cfg, elemStack);
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
    // Extended (non-StAX) public API:
    ////////////////////////////////////////////////////
     */

    public DTDSubset getDTD() {
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
     * @param copyContents If true, will copy contents of DOCTYPE declaration
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

        // Does anyone care about stuff in DTD?
        if (!copyContents) {
            ((BranchingReaderSource) mInput).endBranch(mInputPtr);
        }

        char c = getNextChar(SUFFIX_IN_DTD);
        DTDSubset intSubset = null;

        /* Do we have an internal subset? Note that we have earlier checked
         * that it has to be either '[' or closing '>'.
         */
        if (c == '[') {
            intSubset = mConfig.getDtdReader().readInternalSubset(this, mInput, mConfig);
            // And then we need closing '>'
            c = getNextCharAfterWS(SUFFIX_IN_DTD_INTERNAL);
        }

        if (c != '>') {
            throwUnexpectedChar(c, "; expected '>' to finish DOCTYPE declaration.");
        }

        // Enough about DOCTYPE decl itself:
        if (copyContents) {
            ((BranchingReaderSource) mInput).endBranch(mInputPtr);
        }

        /* But, then, we also may need to read the external subset, if
         * one was defined:
         */
        DTDSubset extSubset = (mDtdPublicId != null || mDtdSystemId != null) ?
            findDtdExtSubset(mDtdPublicId, mDtdSystemId, intSubset) : null;
        DTDSubset combo;
        
        if (intSubset == null) {
            combo = extSubset;
        } else if (extSubset == null) {
            combo = intSubset;
        } else {
            combo = intSubset.combineWithExternalSubset(extSubset, mConfig.getXMLReporter());
        }
        
        mDTD = combo;
        mGeneralEntities = (combo == null) ? null : combo.getGeneralEntityMap();
        if (hasConfigFlags(CFG_VALIDATE_AGAINST_DTD)) {
            mElementStack.setElementSpecs(combo.getElementMap());
        }
    }

    private DTDSubset findDtdExtSubset(String pubId, String sysId,
                                       DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        boolean cache = hasConfigFlags(CFG_CACHE_DTDS);
        URL sysRef = null;
        DTDId dtdId = null;
        DTDSubset extSubset = null;

        if (cache) {
            /* Following settings will change what gets stored as DTD, so
             * they need to separate cached instances too:
             */
            int significantFlags = mConfigFlags &
                (CFG_NAMESPACE_AWARE
                 | CFG_NORMALIZE_LFS | CFG_NORMALIZE_ATTR_VALUES
                 );

            if (pubId != null) {
                dtdId = DTDId.constructFromPublicId(pubId, significantFlags);
            } else {
                sysRef = resolveExtSubsetPath(sysId);
                dtdId = DTDId.constructFromSystemId(sysRef, significantFlags);
            }
            extSubset = mOwner.findCachedDTD(dtdId);
            /* Ok, now; can use the cached copy iff it does not refer to
             * any parameter entities internal subset (if one exists)
             * defines:
             */
            if (extSubset != null) {
                if (intSubset == null || extSubset.isReusableWith(intSubset)) {
                    return extSubset;
                }
            }
        }

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
        
        WstxInputSource src;
        WstxInputResolver res = mConfig.getDtdResolver();
        if (res != null) {
            // null, since it's not an entity expansion
            src = res.resolveReference
                (mInput, null, mDtdPublicId, mDtdSystemId, sysRef);
        } else {
            src = null;
        }
        
        if (src == null) {
            // null, since it's not an entity expansion
            src = DefaultInputResolver.getInstance().resolveReference
                (mInput, null, mDtdPublicId, mDtdSystemId, sysRef);
        }
        extSubset = mConfig.getDtdReader().readExternalSubset(this, src, mConfig, intSubset);
        
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

    /**
     * Method called to resolve path to external DTD subset, given
     * system identifier.
     */
    private URL resolveExtSubsetPath(String systemId)
        throws IOException
    {
        // Do we have a context to use for resolving?
        URL ctxt = mInput.getSource();

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
