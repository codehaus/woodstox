package com.ctc.wstx.api;

import java.net.URL;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;

import com.ctc.wstx.io.WstxInputResolver;

/**
 * Interface that defines extended Woodstox Input Factory API; methods
 * that input factories implement above and beyond default StAX API.
 * Additionally it does contain those StAX API methods that are directly
 * related to configuration settings (but not actual factory methods).
 *<p>
 * Interface-based approach was chosen because of problems with trying
 * to extend abstract StAX factory classes.
 */
public interface WstxInputFactoryConfig
{
    /*
    ////////////////////////////////////////////////
    // Methods shared with StAX XMLInputFactory
    ////////////////////////////////////////////////
     */

    /**
     * Accessor method from {@link XMLInputFactory}; included here
     * for convenience
     */
    public Object getProperty(String name);

    public boolean isPropertySupported(String name);

    public void setProperty(String propName, Object value);

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, simple accessors
    ////////////////////////////////////////////////
     */

    public boolean willCoalesceText();

    public boolean willSupportNamespaces();

    public boolean willReplaceEntityRefs();

    public boolean willSupportExternalEntities();

    public boolean willSupportDTDs();

    public boolean willValidateWithDTD();

    // Wstx-properties:

    public boolean willNormalizeLFs();

    public boolean willNormalizeAttrValues();

    public boolean willInternNsURIs();

    public boolean willReportAllTextAsCharacters();

    public boolean willReportPrologWhitespace();

    public boolean willCacheDTDs();

    public boolean willParseLazily();

    public int getInputBufferLength();

    public int getTextBufferLength();

    public int getShortestReportedTextSegment();

    public Map getCustomInternalEntities();

    public URL getBaseURL();

    public WstxInputResolver getDtdResolver();

    public WstxInputResolver getEntityResolver();

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, simple mutators
    ////////////////////////////////////////////////
     */

    public void doCoalesceText(boolean state);

    public void doSupportNamespaces(boolean state);

    public void doReplaceEntityRefs(boolean state);

    public void doSupportExternalEntities(boolean state);

    public void doSupportDTDs(boolean state);

    public void doValidateWithDTD(boolean state);

    public void setXMLReporter(XMLReporter r);

    /**
     * Note: it's preferable to use Wstx-specific {@link #setEntityResolver}
     * instead, if possible, since this just wraps passed in resolver.
     */
    public void setXMLResolver(XMLResolver r);


    // Wstx-properties:

    public void doNormalizeLFs(boolean state);

    public void doNormalizeAttrValues(boolean state);

    public void doInternNsURIs(boolean state);

    public void doReportPrologWhitespace(boolean state);

    public void doCacheDTDs(boolean state);

    public void doParseLazily(boolean state);

    public void doReportAllTextAsCharacters(boolean state);

    public void setInputBufferLength(int value);

    public void setTextBufferLength(int value);

    public void setShortestReportedTextSegment(int value);

    public void setBaseURL(URL baseURL);

    public void setCustomInternalEntities(Map in);

    public void setDtdResolver(WstxInputResolver r);

    public void setEntityResolver(WstxInputResolver r);

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, profile mutators
    ////////////////////////////////////////////////
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
     *    interning).
     * </li>
     *</ul>
     */
    public void configureForMaxConformance();

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
    public void configureForMaxConvenience();

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
    public void configureForMaxSpeed();

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
    public void configureForMinMemUsage();
    
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
     *</ul>
     */
    public void configureForRoundTripping();
}

