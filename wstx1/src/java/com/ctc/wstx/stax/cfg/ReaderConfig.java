package com.ctc.wstx.stax.cfg;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.*;

import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.util.ArgUtil;
import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.stax.WstxInputProperties;
import com.ctc.wstx.stax.dtd.DTDReaderProxy;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.XMLResolverWrapper;

// !!! Should remove this dependency to event API
import com.ctc.wstx.stax.evt.WEntityDeclInt;

/**
 * Simple configuration container class; passed by reader factory to reader
 * instance created.
 */
public final class ReaderConfig
    implements InputConfigFlags
{
    /*
    ////////////////////////////////////////////////
    // Constants for reader properties:
    ////////////////////////////////////////////////
    */

    // // First, standard StAX properties:

    // Simple flags:
    public final static int PROP_COALESCE_TEXT = 1;
    public final static int PROP_NAMESPACE_AWARE = 2;
    public final static int PROP_REPLACE_ENTITY_REFS = 3;
    public final static int PROP_SUPPORT_EXTERNAL_ENTITIES = 4;
    public final static int PROP_VALIDATE_AGAINST_DTD = 5;
    public final static int PROP_SUPPORT_DTD = 6;

    // Object type properties
    public final static int PROP_EVENT_ALLOCATOR = 7;
    public final static int PROP_WARNING_REPORTER = 8;
    public final static int PROP_XML_RESOLVER = 9;

    // // // Constants for additional Wstx properties:

    // Simple flags:

    public final static int PROP_NORMALIZE_LFS = 20;
    public final static int PROP_NORMALIZE_ATTR_VALUES = 21;
    public final static int PROP_INTERN_NS_URIS = 22;
    public final static int PROP_REPORT_ALL_TEXT_AS_CHARACTERS = 23;
    public final static int PROP_REPORT_PROLOG_WS = 24;
    public final static int PROP_CACHE_DTDS = 25;
    public final static int PROP_LAZY_PARSING = 26;

    // Object type properties

    public final static int PROP_INPUT_BUFFER_LENGTH = 40;
    public final static int PROP_TEXT_BUFFER_LENGTH = 41;
    public final static int PROP_MIN_TEXT_SEGMENT = 42;
    public final static int PROP_CUSTOM_INTERNAL_ENTITIES = 43;
    public final static int PROP_DTD_RESOLVER = 44;
    public final static int PROP_ENTITY_RESOLVER = 45;
    public final static int PROP_BASE_URL = 46;

    /*
    ////////////////////////////////////////////////
    // Limits for numeric properties
    ////////////////////////////////////////////////
    */

    /**
     * Need to set a minimum size, since there are some limitations to
     * smallest consequtive block that can be used.
     */
    final static int MIN_INPUT_BUFFER_LENGTH = 8; // 16 bytes

    /**
     * Let's just avoid worst performance bottlenecks... not a big deal,
     * since it gets dynamically increased
     */
    final static int MIN_TEXT_BUFFER_LENGTH = 32; // 64 bytes

    /**
     * Let's allow caching of few dozens of DTDs... shouldn't really
     * matter, how many DTDs does one really use?
     */
    final static int DTD_CACHE_SIZE_J2SE = 30;

    final static int DTD_CACHE_SIZE_J2ME = 10;

    /*
    ////////////////////////////////////////////////
    // Default values for custom properties:
    ////////////////////////////////////////////////
    */

    /**
     * By default, let's require minimum of 64 chars to be delivered
     * as shortest partial (piece of) text (CDATA, text) segment;
     * same for both J2ME subset and full readers. Prevents tiniest
     * runts from getting passed
     */
    public final static int DEFAULT_SHORTEST_TEXT_SEGMENT = 64;

    /**
     * Default config flags are converted from individual settings,
     * to conform to StAX 1.0 specifications.
     */
    final static int DEFAULT_FLAGS_FULL =
        // First, default settings StAX specs dictate:
        CFG_COALESCE_TEXT
        | CFG_NAMESPACE_AWARE
        | CFG_REPLACE_ENTITY_REFS
        | CFG_SUPPORT_EXTERNAL_ENTITIES
        | CFG_SUPPORT_DTD

        // and then custom setting defaults:

        // default is to turn on content normalization
        | CFG_NORMALIZE_ATTR_VALUES
        | CFG_NORMALIZE_LFS

        // and namespace URI interning
        | CFG_INTERN_NS_URIS

        // but NOT reporting of ignorable white space in prolog/epilog:
        // | CFG_REPORT_PROLOG_WS

        /* but enable DTD caching (if they are handled):
         * (... maybe J2ME subset shouldn't do it?)
         */
        | CFG_CACHE_DTDS

        /* by default, let's also allow lazy parsing, since it tends
         * to improve performance
         */
        | CFG_LAZY_PARSING
        ;

    /**
     * For now defaults for J2ME flags can be identical to 'full' set;
     * differences are in buffer sizes.
     */
    final static int DEFAULT_FLAGS_J2ME = DEFAULT_FLAGS_FULL;

    // // //

    /**
     * Map to use for converting from String property ids to ints
     * described above; useful to allow use of switch later on.
     */
    final static HashMap sProperties = new HashMap(24); // 9 + 7, currently, 75% fill rate
    static {
        // Standard ones; support for features
        sProperties.put(XMLInputFactory.IS_COALESCING,
                        new Integer(PROP_COALESCE_TEXT));
        sProperties.put(XMLInputFactory.IS_NAMESPACE_AWARE,
                        new Integer(PROP_NAMESPACE_AWARE));
        sProperties.put(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                    new Integer(PROP_REPLACE_ENTITY_REFS));
        sProperties.put(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                    new Integer(PROP_SUPPORT_EXTERNAL_ENTITIES));
        sProperties.put(XMLInputFactory.IS_VALIDATING,
                        new Integer(PROP_VALIDATE_AGAINST_DTD));
        sProperties.put(XMLInputFactory.SUPPORT_DTD,
                        new Integer(PROP_SUPPORT_DTD));

        // Standard ones; pluggable components
        sProperties.put(XMLInputFactory.ALLOCATOR,
                        new Integer(PROP_EVENT_ALLOCATOR));
        sProperties.put(XMLInputFactory.REPORTER,
                        new Integer(PROP_WARNING_REPORTER));
        sProperties.put(XMLInputFactory.RESOLVER,
                        new Integer(PROP_XML_RESOLVER));

        // Non-standard ones, flags:

        sProperties.put(WstxInputProperties.P_NORMALIZE_LFS,
                        new Integer(PROP_NORMALIZE_LFS));
        sProperties.put(WstxInputProperties.P_NORMALIZE_ATTR_VALUES,
                        new Integer(PROP_NORMALIZE_ATTR_VALUES));
        sProperties.put(WstxInputProperties.P_INTERN_NS_URIS,
                        new Integer(PROP_INTERN_NS_URIS));
        sProperties.put(WstxInputProperties.P_REPORT_ALL_TEXT_AS_CHARACTERS,
                        new Integer(PROP_REPORT_ALL_TEXT_AS_CHARACTERS));
        sProperties.put(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE,
                        new Integer(PROP_REPORT_PROLOG_WS));
        sProperties.put(WstxInputProperties.P_CACHE_DTDS,
                        new Integer(PROP_CACHE_DTDS));
        sProperties.put(WstxInputProperties.P_LAZY_PARSING,
                        new Integer(PROP_LAZY_PARSING));

        // Non-standard ones, non-flags:

        sProperties.put(WstxInputProperties.P_INPUT_BUFFER_LENGTH,
                        new Integer(PROP_INPUT_BUFFER_LENGTH));
        sProperties.put(WstxInputProperties.P_TEXT_BUFFER_LENGTH,
                        new Integer(PROP_TEXT_BUFFER_LENGTH));
        sProperties.put(WstxInputProperties.P_MIN_TEXT_SEGMENT,
                        new Integer(PROP_MIN_TEXT_SEGMENT));
        sProperties.put(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES,
                        new Integer(PROP_CUSTOM_INTERNAL_ENTITIES));
        sProperties.put(WstxInputProperties.P_DTD_RESOLVER,
                        new Integer(PROP_DTD_RESOLVER));
        sProperties.put(WstxInputProperties.P_ENTITY_RESOLVER,
                        new Integer(PROP_ENTITY_RESOLVER));
        sProperties.put(WstxInputProperties.P_BASE_URL,
                        new Integer(PROP_BASE_URL));
    }

    /*
    //////////////////////////////////////////////////////////
    // Current config state:
    //////////////////////////////////////////////////////////
     */

    final boolean mIsJ2MESubset;

    final SymbolTable mSymbols;
    final DTDReaderProxy mDtdReader;

    int mConfigFlags;

    int mInputBufferLen;
    int mTextBufferLen;
    int mMinTextSegmentLen;

    Map mCustomEntities;

    XMLReporter mReporter;

    XMLResolver mXmlResolver = null;
    WstxInputResolver mDtdResolver = null;

    WstxInputResolver mEntityResolver = null;

    /**
     * Base URL to use as the resolution context for relative entity
     * references
     */
    URL mBaseURL = null;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private ReaderConfig(boolean j2meSubset,
                         SymbolTable symbols, DTDReaderProxy dtdReader,
                         int configFlags,
                         int inputBufLen, int textBufLen,
                         int minTextSegmentLen)
    {
        mIsJ2MESubset = j2meSubset;

        mSymbols = symbols;
        mDtdReader = dtdReader;

        mConfigFlags = configFlags;

        mInputBufferLen = inputBufLen;
        mTextBufferLen = textBufLen;
        mMinTextSegmentLen = minTextSegmentLen;
    }

    public static ReaderConfig createJ2MEDefaults(SymbolTable symbols,
                                                  DTDReaderProxy dtdReader)
    {
        /* For J2ME we'll use slightly smaller buffer sizes by
         * default, on assumption lower memory usage is desireable:
         */
        ReaderConfig rc = new ReaderConfig
            (true, symbols, dtdReader, DEFAULT_FLAGS_J2ME,
             // 4k input buffer (2000 chars):
             2000,
             /* 2k initial temp buffer for storing text; defines the
              * optimal non-coalesced text segment length
              */
             1000,
             DEFAULT_SHORTEST_TEXT_SEGMENT);
        return rc;
    }

    public static ReaderConfig createFullDefaults(SymbolTable symbols,
                                                  DTDReaderProxy dtdReader)
    {
        /* For full version, can use bit larger buffers to achieve better
         * overall performance.
         */
        ReaderConfig rc = new ReaderConfig
            (false, symbols, dtdReader, DEFAULT_FLAGS_FULL,
             // 8k input buffer (4000 chars):
             4000,
             /* 4k initial temp buffer for storing text; defines the
              * optimal non-coalesced text segment length
              */
             2000,
             DEFAULT_SHORTEST_TEXT_SEGMENT);
        return rc;
    }

    public ReaderConfig createNonShared()
    {
        ReaderConfig rc = new ReaderConfig(mIsJ2MESubset,
                                           mSymbols, mDtdReader,
                                           mConfigFlags,
                                           mInputBufferLen, mTextBufferLen,
                                           mMinTextSegmentLen);
        rc.mCustomEntities = mCustomEntities;
        rc.mReporter = mReporter;
        rc.mBaseURL = mBaseURL;

        return rc;
    }

    /*
    //////////////////////////////////////////////////////////
    // Property name to property id translation...
    //////////////////////////////////////////////////////////
     */

    public int getPropertyId(String id) {
        Integer I = (Integer) sProperties.get(id);
        if (I == null) {
            throw new IllegalArgumentException("Unrecognized property '"+id+"'");
        }
        return I.intValue();
    }

    public boolean isPropertySupported(String name) {
        boolean b = sProperties.containsKey(name);

        /* ??? Less support in J2ME subset? Or is that support not really
         * needed here, but in the matching input factory?
         */

        return b;
    }

    public Object getProperty(int id)
    {
        /* Properties NOT supported here:
             PROP_EVENT_ALLOCATOR
        */

        switch (id) {
            // First, standard properties:

        case PROP_COALESCE_TEXT:
            return willCoalesceText() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_NAMESPACE_AWARE:
            return willSupportNamespaces() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_REPLACE_ENTITY_REFS:
            return willReplaceEntityRefs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_SUPPORT_EXTERNAL_ENTITIES:
            return willSupportExternalEntities() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_AGAINST_DTD:
            return willValidateWithDTD() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_SUPPORT_DTD:
            return willSupportDTDs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_WARNING_REPORTER:
            return getXMLReporter();
        case PROP_XML_RESOLVER:
            return getXMLResolver();


            // // // Then custom ones; flags:

        case PROP_NORMALIZE_LFS:
            return willNormalizeLFs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_NORMALIZE_ATTR_VALUES:
            return willNormalizeAttrValues() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_INTERN_NS_URIS:
            return willInternNsURIs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_REPORT_ALL_TEXT_AS_CHARACTERS:
            return willReportAllTextAsCharacters() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_REPORT_PROLOG_WS:
            return willReportPrologWhitespace() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_CACHE_DTDS:
            return willCacheDTDs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_LAZY_PARSING:
            return willParseLazily() ? Boolean.TRUE : Boolean.FALSE;


            // // // Custom ones; Object properties:

        case PROP_INPUT_BUFFER_LENGTH:
            return new Integer(getInputBufferLength());
        case PROP_TEXT_BUFFER_LENGTH:
            return new Integer(getTextBufferLength());
        case PROP_MIN_TEXT_SEGMENT:
            return new Integer(getShortestReportedTextSegment());
        case PROP_CUSTOM_INTERNAL_ENTITIES:
            return getCustomInternalEntities();
        case PROP_DTD_RESOLVER:
            return getDtdResolver();
        case PROP_ENTITY_RESOLVER:
            return getEntityResolver();
        case PROP_BASE_URL:
            return getBaseURL();

        default:
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }
    }

    public void setProperty(String propName, int id, Object value)
    {
        /* Properties NOT supported here:
             PROP_EVENT_ALLOCATOR
        */

        switch (id) {
            // First, standard properties:

        case PROP_COALESCE_TEXT:
            doCoalesceText(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_NAMESPACE_AWARE:
            doSupportNamespaces(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_REPLACE_ENTITY_REFS:
            doReplaceEntityRefs(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_SUPPORT_EXTERNAL_ENTITIES:
            doSupportExternalEntities(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_SUPPORT_DTD:
            doSupportDTDs(ArgUtil.convertToBoolean(propName, value));
            break;
            
            // // // Then ones that can be dispatched:

        case PROP_VALIDATE_AGAINST_DTD:
            doValidateWithDTD(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_WARNING_REPORTER:
            setXMLReporter((XMLReporter) value);
            break;

        case PROP_XML_RESOLVER:
            setXMLResolver((XMLResolver) value);
            break;

            // // // Custom settings, flags:

        case PROP_NORMALIZE_LFS:
            doNormalizeLFs(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_NORMALIZE_ATTR_VALUES:
            doNormalizeAttrValues(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_INTERN_NS_URIS:
            doInternNsURIs(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_REPORT_PROLOG_WS:
            doReportPrologWhitespace(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_CACHE_DTDS:
            doCacheDTDs(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_LAZY_PARSING:
            doParseLazily(ArgUtil.convertToBoolean(propName, value));
            break;

        case PROP_REPORT_ALL_TEXT_AS_CHARACTERS:
            doReportAllTextAsCharacters(ArgUtil.convertToBoolean(propName, value));
            break;

            // // // Custom settings, Object properties:

        case PROP_INPUT_BUFFER_LENGTH:
            setInputBufferLength(ArgUtil.convertToInt(propName, value, 1));
            break;
        
        case PROP_TEXT_BUFFER_LENGTH:
            setTextBufferLength(ArgUtil.convertToInt(propName, value, 1));
            break;
            
        case PROP_MIN_TEXT_SEGMENT:
            setShortestReportedTextSegment(ArgUtil.convertToInt(propName, value, 1));
            break;

        case PROP_CUSTOM_INTERNAL_ENTITIES:
            setCustomInternalEntities((Map) value);
            break;

        case PROP_DTD_RESOLVER:
            setDtdResolver((WstxInputResolver) value);
            break;

        case PROP_ENTITY_RESOLVER:
            setEntityResolver((WstxInputResolver) value);
            break;

        case PROP_BASE_URL:
            setBaseURL((URL) value);
            break;

        default:
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }
    }
 
    /*
    //////////////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////////////
     */

    // // // Accessors for immutable configuration:

    public SymbolTable getSymbols() { return mSymbols; }

    public DTDReaderProxy getDtdReader() { return mDtdReader; }

    /**
     * In future this property could/should be made configurable?
     */

    public int getDtdCacheSize() {
        return mIsJ2MESubset ? DTD_CACHE_SIZE_J2ME : DTD_CACHE_SIZE_J2SE;
    }

    // // // "Raw" accessors for on/off properties:

    public int getConfigFlags() { return mConfigFlags; }
    public boolean hasConfigFlags(int flags) {
        return (mConfigFlags & flags) == flags;
    }

    // // // Standard StAX on/off property accessors

    public boolean willCoalesceText() {
        return hasConfigFlags(CFG_COALESCE_TEXT);
    }

    public boolean willSupportNamespaces() {
        return hasConfigFlags(CFG_NAMESPACE_AWARE);
    }

    public boolean willReplaceEntityRefs() {
        return hasConfigFlags(CFG_REPLACE_ENTITY_REFS);
    }

    public boolean willSupportExternalEntities() {
        return hasConfigFlags(CFG_SUPPORT_EXTERNAL_ENTITIES);
    }

    public boolean willSupportDTDs() {
        return hasConfigFlags(CFG_SUPPORT_DTD);
    }

    public boolean willValidateWithDTD() {
        return hasConfigFlags(CFG_VALIDATE_AGAINST_DTD);
    }

    // // // Woodstox on/off property accessors
    public boolean willNormalizeLFs() {
        return hasConfigFlags(CFG_NORMALIZE_LFS);
    }

    public boolean willNormalizeAttrValues() {
        return hasConfigFlags(CFG_NORMALIZE_ATTR_VALUES);
    }

    public boolean willInternNsURIs() {
        return hasConfigFlags(CFG_INTERN_NS_URIS);
    }

    public boolean willReportAllTextAsCharacters() {
        return hasConfigFlags(CFG_REPORT_ALL_TEXT_AS_CHARACTERS);
    }

    public boolean willReportPrologWhitespace() {
        return hasConfigFlags(CFG_REPORT_PROLOG_WS);
    }

    public boolean willCacheDTDs() {
        return hasConfigFlags(CFG_CACHE_DTDS);
    }

    public boolean willParseLazily() {
        return hasConfigFlags(CFG_LAZY_PARSING);
    }

    public int getInputBufferLength() { return mInputBufferLen; }
    public int getTextBufferLength() { return mTextBufferLen; }

    public int getShortestReportedTextSegment() { return mMinTextSegmentLen; }

    public Map getCustomInternalEntities() {
        if (mCustomEntities == null) {
            return JdkFeatures.getInstance().getEmptyMap();
        }
        /* Better be defensive and just return a copy...
         */
        int len = mCustomEntities.size();
        HashMap m = new HashMap(len + (len >> 2), 0.81f);
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            m.put(me.getKey(), new String((char[]) me.getValue()));
        }
        return m;
    }

    public XMLReporter getXMLReporter() { return mReporter; }

    public XMLResolver getXMLResolver() { return mXmlResolver; }

    public URL getBaseURL() { return mBaseURL; }
    public WstxInputResolver getDtdResolver() { return mDtdResolver; }
    public WstxInputResolver getEntityResolver() { return mEntityResolver; }

    /*
    //////////////////////////////////////////////////////////
    // Simple mutators
    //////////////////////////////////////////////////////////
     */

    private void setConfigFlag(int flag, boolean state) {
        if (state) {
            mConfigFlags |= flag;
        } else {
            mConfigFlags &= ~flag;
        }
    }

    public void setConfigFlags(int flags) {
        mConfigFlags = flags;
    }

    public void setConfigFlag(int flag) {
        mConfigFlags |= flag;
    }

    public void clearConfigFlag(int flag) {
        mConfigFlags &= ~flag;
    }

    // // // Mutators for standard StAX properties

    public void doCoalesceText(boolean state) {
        setConfigFlag(CFG_COALESCE_TEXT, state);
    }

    public void doSupportNamespaces(boolean state) {
        setConfigFlag(CFG_NAMESPACE_AWARE, state);
    }

    public void doReplaceEntityRefs(boolean state) {
        setConfigFlag(CFG_REPLACE_ENTITY_REFS, state);
    }

    public void doSupportExternalEntities(boolean state) {
        setConfigFlag(CFG_SUPPORT_EXTERNAL_ENTITIES, state);
    }

    public void doSupportDTDs(boolean state) {
        setConfigFlag(CFG_SUPPORT_DTD, state);
    }

    public void doValidateWithDTD(boolean state) {
        setConfigFlag(CFG_VALIDATE_AGAINST_DTD, state);
    }

    // // // Mutators for Woodstox-specific properties

    public void doNormalizeLFs(boolean state) {
        setConfigFlag(CFG_NORMALIZE_LFS, state);
    }

    public void doNormalizeAttrValues(boolean state) {
        setConfigFlag(CFG_NORMALIZE_ATTR_VALUES, state);
    }

    public void doInternNsURIs(boolean state) {
        setConfigFlag(CFG_INTERN_NS_URIS, state);
    }

    public void doReportPrologWhitespace(boolean state) {
        setConfigFlag(CFG_REPORT_PROLOG_WS, state);
    }

    public void doCacheDTDs(boolean state) {
        setConfigFlag(CFG_CACHE_DTDS, state);
    }

    public void doParseLazily(boolean state) {
        setConfigFlag(CFG_LAZY_PARSING, state);
    }

    public void doReportAllTextAsCharacters(boolean state) {
        setConfigFlag(CFG_REPORT_ALL_TEXT_AS_CHARACTERS, state);
    }

    public void setInputBufferLength(int value)
    {
        /* Let's enforce minimum here; necessary to allow longest
         * consequtive text span to be available (xml decl, etc)
         */
        if (value < MIN_INPUT_BUFFER_LENGTH) {
            value = MIN_INPUT_BUFFER_LENGTH;
        }
        mInputBufferLen = value;
    }

    public void setTextBufferLength(int value)
    {
        // Let's just do sanity checks, to avoid worst performance
        if (value < MIN_TEXT_BUFFER_LENGTH) {
            value = MIN_TEXT_BUFFER_LENGTH;
        }
        mTextBufferLen = value;
    }

    public void setShortestReportedTextSegment(int value) {
        mMinTextSegmentLen = value;
    }

    public void setCustomInternalEntities(Map m) {
        Map entMap;
        if (m == null || m.size() < 1) {
            entMap = JdkFeatures.getInstance().getEmptyMap();
        } else {
            int len = m.size();
            entMap = new HashMap(len + (len >> 1), 0.75f);
            Iterator it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                Object val = me.getValue();
                char[] ch;
                if (val == null) {
                    ch = EmptyIterator.getEmptyCharArray();
                } else if (val instanceof char[]) {
                    ch = (char[]) val;
                } else {
                    // Probably String, but let's just ensure that
                    String str = val.toString();
                    ch = str.toCharArray();
                }
                String name = (String) me.getKey();
                entMap.put(name, WEntityDeclInt.create(name, ch));
            }
        }
        mCustomEntities = entMap;
    }

    public void setXMLReporter(XMLReporter r) {
        mReporter = r;
    }

    /**
     * Note: it's preferable to use Wstx-specific {@link #setEntityResolver}
     * instead, if possible, since this just wraps passed in resolver.
     */
    public void setXMLResolver(XMLResolver r) {
        if (r == null) {
            mXmlResolver = null;
            mEntityResolver = null;
        } else {
            mXmlResolver = r;
            mEntityResolver = new XMLResolverWrapper(r);
        }
    }

    public void setDtdResolver(WstxInputResolver r) { mDtdResolver = r; }
    public void setEntityResolver(WstxInputResolver r) { mEntityResolver = r; }

    public void setBaseURL(URL baseURL) { mBaseURL = baseURL; }

    /*
    /////////////////////////////////////////////////////
    // "Profile" mutators
    /////////////////////////////////////////////////////
     */

    public void configureForMaxConformance()
    {
        doNormalizeLFs(true);
        doNormalizeAttrValues(true);
    }

    public void configureForMaxConvenience()
    {
        doCoalesceText(true);
        doReportAllTextAsCharacters(true);
        doReplaceEntityRefs(true);
        doReportPrologWhitespace(false);

        /* Also, we can make errors to be reporting in timely manner:
         * (once again, at potential expense of performance)
         */
        doParseLazily(false);
    }

    public void configureForMaxSpeed() {
        doCoalesceText(false);
        /* If we let Reader decide sizes of text segments, it should be
         * able to optimize it better, thus low min value. This value
         * is only used in cases where text is at buffer boundary, or
         * where entity prevents using consequtive chars from input buffer:
         */
        setShortestReportedTextSegment(8);
        setInputBufferLength(8000); // 16k input buffer
        // Text buffer need not be huge, as we do not coalesce
        setTextBufferLength(4000); // 8K
        doNormalizeLFs(false);
        doNormalizeAttrValues(false);

        // these can also improve speed:
        doInternNsURIs(true);
        doCacheDTDs(true);
        doParseLazily(true);
    }

    public void configureForMinMemUsage()
    {
        doCoalesceText(false);
        setShortestReportedTextSegment(ReaderConfig.DEFAULT_SHORTEST_TEXT_SEGMENT);
        setInputBufferLength(512); // 1k input buffer
        // Text buffer need not be huge, as we do not coalesce
        setTextBufferLength(512); // 1k, to match input buffer size
        doCacheDTDs(false);

        // This can reduce temporary memory usage:
        doParseLazily(true);
    }
    
    public void configureForRoundTripping()
    {
        doReportPrologWhitespace(true);
        doNormalizeLFs(false);
        doNormalizeAttrValues(false);
        doCoalesceText(false);
        // effectively prevents from reporting partial segments:
        setShortestReportedTextSegment(Integer.MAX_VALUE);
        doReplaceEntityRefs(false);
        doReportAllTextAsCharacters(false);
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////////
     */
}
