package com.ctc.wstx.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLInputFactory2; // for property consts

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.dtd.DTDReaderProxy;
import com.ctc.wstx.ent.IntEntity;
import com.ctc.wstx.util.ArgUtil;
import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.SymbolTable;

/**
 * Simple configuration container class; passed by reader factory to reader
 * instance created.
 *<p>
 * TODO:
 *<ul>
 * <li>Enable/disable event support
 * <li>Enable/disable DTD support
 *</ul>
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

    // // Then StAX2 standard properties:

    // Simple flags:
    public final static int PROP_INTERN_NS_URIS = 20;
    public final static int PROP_INTERN_NAMES = 21;
    public final static int PROP_REPORT_ALL_TEXT_AS_CHARACTERS = 22;
    public final static int PROP_REPORT_PROLOG_WS = 23;
    public final static int PROP_PRESERVE_LOCATION = 24;

    // // // Constants for additional Wstx properties:

    // Simple flags:

    public final static int PROP_NORMALIZE_LFS = 40;
    public final static int PROP_NORMALIZE_ATTR_VALUES = 41;
    public final static int PROP_CACHE_DTDS = 42;
    public final static int PROP_LAZY_PARSING = 43;
    public final static int PROP_SUPPORT_DTDPP = 44;

    // Object type properties:

    public final static int PROP_INPUT_BUFFER_LENGTH = 50;
    public final static int PROP_TEXT_BUFFER_LENGTH = 51;
    public final static int PROP_MIN_TEXT_SEGMENT = 52;
    public final static int PROP_CUSTOM_INTERNAL_ENTITIES = 53;
    public final static int PROP_DTD_RESOLVER = 54;
    public final static int PROP_ENTITY_RESOLVER = 55;
    public final static int PROP_BASE_URL = 56;

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

        /* and also make Event objects preserve location info...
         * can be turned off for maximum performance
         */
        | CFG_PRESERVE_LOCATION
        /* Also, let's enable dtd++ support (shouldn't hurt with non-dtd++
         * dtds)
         */
        | CFG_SUPPORT_DTDPP
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

	// StAX2-introduced flags:
        sProperties.put(XMLInputFactory2.P_INTERN_NAMES,
                        new Integer(PROP_INTERN_NAMES));
        sProperties.put(XMLInputFactory2.P_INTERN_NS_URIS,
                        new Integer(PROP_INTERN_NS_URIS));
        sProperties.put(XMLInputFactory2.P_REPORT_ALL_TEXT_AS_CHARACTERS,
                        new Integer(PROP_REPORT_ALL_TEXT_AS_CHARACTERS));
        sProperties.put(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE,
                        new Integer(PROP_REPORT_PROLOG_WS));
        sProperties.put(XMLInputFactory2.P_PRESERVE_LOCATION,
                        new Integer(PROP_PRESERVE_LOCATION));

        // Non-standard ones, flags:

        sProperties.put(WstxInputProperties.P_NORMALIZE_LFS,
                        new Integer(PROP_NORMALIZE_LFS));
        sProperties.put(WstxInputProperties.P_NORMALIZE_ATTR_VALUES,
                        new Integer(PROP_NORMALIZE_ATTR_VALUES)); 
        sProperties.put(WstxInputProperties.P_CACHE_DTDS,
                        new Integer(PROP_CACHE_DTDS));
        sProperties.put(WstxInputProperties.P_LAZY_PARSING,
                        new Integer(PROP_LAZY_PARSING));
        sProperties.put(WstxInputProperties.P_SUPPORT_DTDPP,
                        new Integer(PROP_SUPPORT_DTDPP));

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

    XMLResolver mDtdResolver = null;
    XMLResolver mEntityResolver = null;

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

    public static ReaderConfig createJ2MEDefaults(SymbolTable sym, DTDReaderProxy dtdReader)
    {
        /* For J2ME we'll use slightly smaller buffer sizes by
         * default, on assumption lower memory usage is desireable:
         */
        ReaderConfig rc = new ReaderConfig
            (true, sym, dtdReader, DEFAULT_FLAGS_J2ME,
             // 4k input buffer (2000 chars):
             2000,
             /* 2k initial temp buffer for storing text; defines the
              * optimal non-coalesced text segment length
              */
             1000,
             DEFAULT_SHORTEST_TEXT_SEGMENT);
        return rc;
    }

    public static ReaderConfig createFullDefaults(SymbolTable sym, DTDReaderProxy dtdReader)
    {
        /* For full version, can use bit larger buffers to achieve better
         * overall performance.
         */
        ReaderConfig rc = new ReaderConfig
            (false, sym, dtdReader, DEFAULT_FLAGS_FULL,
             // 8k input buffer (4000 chars):
             4000,
             /* 4k initial temp buffer for storing text; defines the
              * optimal non-coalesced text segment length
              */
             2000,
             DEFAULT_SHORTEST_TEXT_SEGMENT);
        return rc;
    }

    public ReaderConfig createNonShared(SymbolTable sym)
    {
        ReaderConfig rc = new ReaderConfig(mIsJ2MESubset,
                                           (sym == null) ? mSymbols : sym,
                                           mDtdReader,
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
    // Public API, generic StAX config methods
    //////////////////////////////////////////////////////////
     */

    public Object getProperty(String propName)
    {
        return getProperty(getPropertyId(propName));
    }

    public boolean isPropertySupported(String name) {
        boolean b = sProperties.containsKey(name);

        /* ??? Less support in J2ME subset? Or is that support not really
         * needed here, but in the matching input factory?
         */

        return b;
    }

    public void setProperty(String propName, Object value)
    {
        setProperty(propName, getPropertyId(propName), value);
    }
 
    /*
    //////////////////////////////////////////////////////////
    // Public API, accessors
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

    public boolean willInternNames() {
	// 17-Apr-2005, TSa: NOP, we'll always intern them...
        return true;
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

    public boolean willPreserveLocation() {
        return hasConfigFlags(CFG_PRESERVE_LOCATION);
    }

    public boolean willSupportDTDPP() {
        return hasConfigFlags(CFG_SUPPORT_DTDPP);
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

    public XMLResolver getXMLResolver() { return mEntityResolver; }

    public URL getBaseURL() { return mBaseURL; }
    public XMLResolver getDtdResolver() { return mDtdResolver; }
    public XMLResolver getEntityResolver() { return mEntityResolver; }

    /*
    //////////////////////////////////////////////////////////
    // Simple mutators
    //////////////////////////////////////////////////////////
     */

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

    public void doInternNames(boolean state) {
	// 17-Apr-2005, TSa: NOP, we'll always intern them...
	;
    }

    public void doInternNsURIs(boolean state) {
        setConfigFlag(CFG_INTERN_NS_URIS, state);
    }

    public void doReportPrologWhitespace(boolean state) {
        setConfigFlag(CFG_REPORT_PROLOG_WS, state);
    }

    public void doReportAllTextAsCharacters(boolean state) {
        setConfigFlag(CFG_REPORT_ALL_TEXT_AS_CHARACTERS, state);
    }

    public void doCacheDTDs(boolean state) {
        setConfigFlag(CFG_CACHE_DTDS, state);
    }

    public void doParseLazily(boolean state) {
        setConfigFlag(CFG_LAZY_PARSING, state);
    }

    public void doPreserveLocation(boolean state) {
        setConfigFlag(CFG_PRESERVE_LOCATION, state);
    }

    public void doSupportDTDPP(boolean state) {
        setConfigFlag(CFG_SUPPORT_DTDPP, state);
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
                entMap.put(name, IntEntity.create(name, ch));
            }
        }
        mCustomEntities = entMap;
    }

    public void setXMLReporter(XMLReporter r) {
        mReporter = r;
    }

    /**
     * Note: for better granularity, you should call {@link #setEntityResolver}
     * and {@link #setDtdResolver} instead.
     */
    public void setXMLResolver(XMLResolver r) {
        mEntityResolver = r;
        mDtdResolver = r;
    }

    public void setDtdResolver(XMLResolver r) {
        mDtdResolver = r;
    }

    public void setEntityResolver(XMLResolver r) {
        mEntityResolver = r;
    }

    public void setBaseURL(URL baseURL) { mBaseURL = baseURL; }

    /*
    /////////////////////////////////////////////////////
    // "Profile" mutators
    /////////////////////////////////////////////////////
     */

    /**
     * Method to call to make Reader created conform as closely to XML
     * standard as possible, doing all checks and transformations mandated
     * (linefeed conversions, attr value normalizations). Note that this
     * does NOT include enabling DTD validation.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables all XML mandated transformations: will convert linefeeds
     *   to canonical LF, will convert white space in attributes as per
     *   specs
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>'performance' settings (buffer sizes, DTD caching, coalescing,
     *    interning, accurate location info).
     * </li>
     *</ul>
     */
    public void configureForMaxConformance()
    {
        doNormalizeLFs(true);
        doNormalizeAttrValues(true);
    }

    /**
     * Method to call to make Reader created be as "convenient" to use
     * as possible; ie try to avoid having to deal with some of things
     * like segmented text chunks. This may incure some slight performance
     * penalties, but shouldn't affect conformance.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables text coalescing.
     *  <li>
     * <li>Forces all non-ignorable text events (Text, CDATA) to be reported
     *    as CHARACTERS event.
     *  <li>
     * <li>Enables automatic entity reference replacement.
     *  <li>
     * <li>Disables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>Text normalization (whether it's more convenient that linefeeds
     *    are converted or not is an open question).
     *   </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>other 'performance' settings (buffer sizes, interning, DTD caching)
     *    than coalescing
     * </li>
     *</ul>
     */
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
        /* Ditto for event API...
         */
        doPreserveLocation(true);
    }

    /**
     * Method to call to make Reader created be as fast as possible reading
     * documents, especially for long-running processes where caching is
     * likely to help. 
     * Potential trade-offs are somewhat increased memory usage
     * (full-sized input buffers), and reduced XML conformance (will not
     * do some of transformations).
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Disables text coalescing, sets lowish value for min. reported
     *    text segment length.
     *  </li>
     * <li>Increases input buffer length a bit from default.
     *  </li>
     * <li>Disables text normalization (linefeeds, attribute values)
     *  </li>
     * <li>Enables all interning (ns URIs)
     *  </li>
     * <li>Enables DTD caching
     *  </li>
     *</ul>
     *
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>Entity replacement settings (automatic, support external)
     * </li>
     *</ul>
     */
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
        doPreserveLocation(false);
    }

    /**
     * Method to call to make Reader created minimize its memory usage.
     * This generally incurs some performance penalties, due to using
     * smaller input buffers.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Turns off coalescing, to prevent having to store long text
     *   segments in consequtive buffer; resets min. reported text segment
     *   to the default value.
     *  <li>
     * <li>Reduces buffer sizes from default
     *  <li>
     * <li>Turns off DTD-caching
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>Normalization (linefeed, attribute value)
     * </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>Interning settings (may or may not affect mem usage)
     * </li>
     *</ul>
     */
    public void configureForMinMemUsage()
    {
        doCoalesceText(false);
        setShortestReportedTextSegment(ReaderConfig.DEFAULT_SHORTEST_TEXT_SEGMENT);
        setInputBufferLength(512); // 1k input buffer
        // Text buffer need not be huge, as we do not coalesce
        setTextBufferLength(512); // 1k, to match input buffer size
        doCacheDTDs(false);

        // These can reduce temporary memory usage:
        doParseLazily(true);
        doPreserveLocation(false);
    }
    
    /**
     * Method to call to make Reader try to preserve as much of input
     * formatting as possible, so that round-tripping would be as lossless
     * as possible, ie that matching writer could produce output as closely
     * matching input format as possible.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     * <li>Disables XML mandated transformations (linefeed, attribute values),
     *   to preserve most of original formatting.
     *  <li>
     * <li>Disables coalescing, to prevent CDATA and Text segments from getting
     *   combined.
     *  <li>
     * <li>Increases minimum report text segment length so that all original
     *    text segment chunks are reported fully
     *  <li>
     * <li>Disables automatic entity replacement, to allow for preserving
     *    such references.
     *  <li>
     * <li>Disables automatic conversion of CDATA to Text events.
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings (enable/disable)
     * </li>
     *  <li>Some perfomance settings: interning settings, DTD caching
     * </li>
     *  <li>Whether to preserve additional information not relevant
     *    to outputting (like CFG_PRESERVE_LOCATION).
     * </li>
     *</ul>
     */
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

    private void setConfigFlag(int flag, boolean state) {
        if (state) {
            mConfigFlags |= flag;
        } else {
            mConfigFlags &= ~flag;
        }
    }

    public int getPropertyId(String id) {
        Integer I = (Integer) sProperties.get(id);
        if (I == null) {
            throw new IllegalArgumentException("Unrecognized property '"+id+"'");
        }
        return I.intValue();
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
        case PROP_INTERN_NAMES:
            return willInternNames() ? Boolean.TRUE : Boolean.FALSE;
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
        case PROP_PRESERVE_LOCATION:
            return willPreserveLocation() ? Boolean.TRUE : Boolean.FALSE;


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

        default: // sanity check, should never happen
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

        case PROP_INTERN_NAMES:
            doInternNames(ArgUtil.convertToBoolean(propName, value));
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

        case PROP_PRESERVE_LOCATION:
            doPreserveLocation(ArgUtil.convertToBoolean(propName, value));
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
            setDtdResolver((XMLResolver) value);
            break;

        case PROP_ENTITY_RESOLVER:
            setEntityResolver((XMLResolver) value);
            break;

        case PROP_BASE_URL:
            setBaseURL((URL) value);
            break;

        default: // sanity check, should never happen
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }
    }
}
