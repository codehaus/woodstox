package com.ctc.wstx.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.*;

import org.codehaus.stax2.EscapingWriterFactory;
import org.codehaus.stax2.XMLOutputFactory2; // for property consts

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.util.ArgUtil;

/**
 * Simple configuration container class; passed by reader factory to reader
 * instance created.
 */
public final class WriterConfig
    implements OutputConfigFlags
{
    // // // Constants for standard StAX properties:

    protected final static String DEFAULT_AUTOMATIC_NS_PREFIX = "wstxns";

    // // // First, standard StAX writer properties

    final static int PROP_AUTOMATIC_NS = 1; // standard property ("repairing")

    // // // And then additional StAX2 properties:

    // General output settings
    final static int PROP_AUTOMATIC_EMPTY_ELEMS = 2;
    // Namespace settings:
    final static int PROP_ENABLE_NS = 3;
    final static int PROP_AUTOMATIC_NS_PREFIX = 4;
    // Escaping text content/attr values:
    final static int PROP_TEXT_ESCAPER = 5;
    final static int PROP_ATTR_VALUE_ESCAPER = 6;
    // Problem checking/reporting options
    final static int PROP_PROBLEM_REPORTER = 7;

    // // // And then custom Wstx properties:

    // Namespace support, settings:

    // Output settings:
    final static int PROP_OUTPUT_CDATA_AS_TEXT = 11;

    final static int PROP_COPY_DEFAULT_ATTRS = 12;

    // Validation flags:

    // Let's not use this any more... no point:
    final static int PROP_VALIDATE_STRUCTURE = 15; 
    final static int PROP_VALIDATE_CONTENT = 16;
    final static int PROP_VALIDATE_ATTR = 17;
    final static int PROP_VALIDATE_NAMES = 18;
    final static int PROP_FIX_CONTENT = 19;

    // // // Default settings for additional properties:

    final static boolean DEFAULT_ENABLE_NS = true;

    /* 27-Apr-2005, TSa: Changed the default to 'true' for 2.0rc1,
     *   since usually it is beneficial to still allow for empty
     *   elements...
     */
    final static boolean DEFAULT_AUTOMATIC_EMPTY_ELEMS = true;
    final static boolean DEFAULT_OUTPUT_CDATA_AS_TEXT = false;
    final static boolean DEFAULT_COPY_DEFAULT_ATTRS = false;

    /* How about validation? Let's turn them mostly off by default, since
     * there are some performance hits when enabling them.
     */

    // Structural checks are easy, cheap and useful...
    final static boolean DEFAULT_VALIDATE_STRUCTURE = true;
    final static boolean DEFAULT_VALIDATE_CONTENT = false;
    final static boolean DEFAULT_VALIDATE_ATTR = false;
    final static boolean DEFAULT_VALIDATE_NAMES = false;
    /* In a way it doesn't matter; if validation is not enabled, neither
     * is fixing...
     */
    final static boolean DEFAULT_FIX_CONTENT = true;

    /**
     * Default config flags are converted from individual settings,
     * to conform to StAX 1.0 specifications.
     */
    final static int DEFAULT_FLAGS_J2ME =
        0 // | CFG_AUTOMATIC_NS
        | (DEFAULT_ENABLE_NS ? CFG_ENABLE_NS : 0)

        | (DEFAULT_AUTOMATIC_EMPTY_ELEMS ? CFG_AUTOMATIC_EMPTY_ELEMS : 0)
        | (DEFAULT_OUTPUT_CDATA_AS_TEXT ? CFG_OUTPUT_CDATA_AS_TEXT : 0)
        | (DEFAULT_COPY_DEFAULT_ATTRS ? CFG_COPY_DEFAULT_ATTRS : 0)

        | (DEFAULT_VALIDATE_STRUCTURE ? CFG_VALIDATE_STRUCTURE : 0)
        | (DEFAULT_VALIDATE_CONTENT ? CFG_VALIDATE_CONTENT : 0)
        | (DEFAULT_VALIDATE_ATTR ? CFG_VALIDATE_ATTR : 0)
        | (DEFAULT_VALIDATE_NAMES ? CFG_VALIDATE_NAMES : 0)
        | (DEFAULT_FIX_CONTENT ? CFG_FIX_CONTENT : 0)
        ;

    /**
     * For now, full instances start with same settings as J2ME subset
     */
    final static int DEFAULT_FLAGS_FULL = DEFAULT_FLAGS_J2ME;

    // // // 

    /**
     * Map to use for converting from String property ids to ints
     * described above; useful to allow use of switch later on.
     */
    final static HashMap sProperties = new HashMap(8);
    static {
        // Standard ones; support for features
        sProperties.put(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                        new Integer(PROP_AUTOMATIC_NS));

        // // StAX (1.0) standard ones:

        // Namespace support
        sProperties.put(XMLOutputFactory2.P_NAMESPACE_AWARE,
                        new Integer(PROP_ENABLE_NS));

        // // StAX2 standard ones:

        // Generic output
        sProperties.put(XMLOutputFactory2.P_AUTOMATIC_EMPTY_ELEMENTS,
                        new Integer(PROP_AUTOMATIC_EMPTY_ELEMS));
        // Namespace support
        sProperties.put(XMLOutputFactory2.P_AUTOMATIC_NS_PREFIX,
                        new Integer(PROP_AUTOMATIC_NS_PREFIX));
        // Text/attr value escaping (customized escapers)
        sProperties.put(XMLOutputFactory2.P_TEXT_ESCAPER,
                        new Integer(PROP_TEXT_ESCAPER));
        sProperties.put(XMLOutputFactory2.P_ATTR_VALUE_ESCAPER,
                        new Integer(PROP_ATTR_VALUE_ESCAPER));
        // Problem checking/reporting options
        sProperties.put(XMLOutputFactory2.P_PROBLEM_REPORTER,
                        new Integer(PROP_PROBLEM_REPORTER));

        // // Woodstox-specifics:

        // Output conversions
        sProperties.put(WstxOutputProperties.P_OUTPUT_CDATA_AS_TEXT,
                        new Integer(PROP_OUTPUT_CDATA_AS_TEXT));
        sProperties.put(WstxOutputProperties.P_COPY_DEFAULT_ATTRS,
                        new Integer(PROP_COPY_DEFAULT_ATTRS));

        // Validation settings:
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE,
                        new Integer(PROP_VALIDATE_STRUCTURE));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT,
                        new Integer(PROP_VALIDATE_CONTENT));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR,
                        new Integer(PROP_VALIDATE_ATTR));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES,
                        new Integer(PROP_VALIDATE_NAMES));
    }

    /*
    //////////////////////////////////////////////////////////
    // Current config state:
    //////////////////////////////////////////////////////////
     */

    final boolean mIsJ2MESubset;

    protected int mConfigFlags;

    protected String mAutoNsPrefix;

    protected EscapingWriterFactory mTextEscaperFactory = null;

    protected EscapingWriterFactory mAttrValueEscaperFactory = null;

    protected XMLReporter mProblemReporter = null;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle:
    //////////////////////////////////////////////////////////
     */

    private WriterConfig(boolean j2meSubset, int flags, String autoNsPrefix,
                         EscapingWriterFactory textEscaperF,
                         EscapingWriterFactory attrValueEscaperF,
                         XMLReporter problemReporter)
    {
        mIsJ2MESubset = j2meSubset;
        mConfigFlags = flags;
        mAutoNsPrefix = autoNsPrefix;
        mTextEscaperFactory = textEscaperF;
        mAttrValueEscaperFactory = attrValueEscaperF;
        mProblemReporter = problemReporter;
    }

    public static WriterConfig createJ2MEDefaults()
    {
        WriterConfig rc = new WriterConfig
            (true, DEFAULT_FLAGS_J2ME, DEFAULT_AUTOMATIC_NS_PREFIX,
             null, null, null);
        return rc;
    }

    public static WriterConfig createFullDefaults()
    {
        WriterConfig rc = new WriterConfig
            (true, DEFAULT_FLAGS_FULL, DEFAULT_AUTOMATIC_NS_PREFIX,
             null, null, null);
        return rc;
    }

    public WriterConfig createNonShared()
    {
        WriterConfig rc = new WriterConfig(mIsJ2MESubset,
                                           mConfigFlags, mAutoNsPrefix,
                                           mTextEscaperFactory,
                                           mAttrValueEscaperFactory,
                                           mProblemReporter);
        return rc;
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String name) {
        return sProperties.containsKey(name);
    }

    public Object getProperty(String name)
    {
        int id = getPropertyId(name);

        switch (id) {
            // First, standard properties:

        case PROP_AUTOMATIC_NS:
            return automaticNamespacesEnabled() ? Boolean.TRUE : Boolean.FALSE;

            // // // Then custom ones:

            // First, true/false ones:

        case PROP_ENABLE_NS:
            return willSupportNamespaces() ? Boolean.TRUE : Boolean.FALSE;

        case PROP_AUTOMATIC_EMPTY_ELEMS:
            return automaticEmptyElementsEnabled() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_OUTPUT_CDATA_AS_TEXT:
            return willOutputCDataAsText() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_COPY_DEFAULT_ATTRS:
            return willCopyDefaultAttrs() ? Boolean.TRUE : Boolean.FALSE;

        case PROP_VALIDATE_STRUCTURE:
            return willValidateStructure() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_CONTENT:
            return willValidateContent() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_ATTR:
            return willValidateAttributes() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_NAMES:
            return willValidateNames() ? Boolean.TRUE : Boolean.FALSE;

            // // // Object valued ones:

        case PROP_AUTOMATIC_NS_PREFIX:
            return getAutomaticNsPrefix();
        case PROP_TEXT_ESCAPER:
            return getTextEscaperFactory();
        case PROP_ATTR_VALUE_ESCAPER:
            return getAttrValueEscaperFactory();
        case PROP_PROBLEM_REPORTER:
            return getProblemReporter();
        }

        throw new Error("Internal error: no handler for property with internal id "+id+".");
    }

    /**
     * @return True, if the specified property was <b>succesfully</b>
     *    set to specified value; false if its value was not changed
     */
    public boolean setProperty(String name, Object value)
    {
        int id = getPropertyId(name);

        switch (id) {
            // First, standard properties:

        case PROP_AUTOMATIC_NS:
            enableAutomaticNamespaces(ArgUtil.convertToBoolean(name, value));
            break;

            // // // Then custom ones:

        case PROP_ENABLE_NS:
            doSupportNamespaces(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_AUTOMATIC_EMPTY_ELEMS:
            enableAutomaticEmptyElements(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_OUTPUT_CDATA_AS_TEXT:
            doOutputCDataAsText(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_COPY_DEFAULT_ATTRS:
            doCopyDefaultAttrs(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_STRUCTURE:
            doValidateContent(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_CONTENT:
            doValidateContent(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_ATTR:
            doValidateAttributes(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_NAMES:
            doValidateNames(ArgUtil.convertToBoolean(name, value));
            break;


        case PROP_AUTOMATIC_NS_PREFIX:
            // value should be a String, but let's verify that:
            setAutomaticNsPrefix(value.toString());
            break;

        case PROP_TEXT_ESCAPER:
            setTextEscaperFactory((EscapingWriterFactory) value);
            break;

        case PROP_ATTR_VALUE_ESCAPER:
            setAttrValueEscaperFactory((EscapingWriterFactory) value);
            break;

        case PROP_PROBLEM_REPORTER:
            setProblemReporter((XMLReporter) value);
            break;

        default:
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }

        return true;
    }

    /*
    //////////////////////////////////////////////////////////
    // Extended Woodstox API, accessors/modifiers
    //////////////////////////////////////////////////////////
     */

    // // // "Raw" accessors for on/off properties:

    public int getConfigFlags() { return mConfigFlags; }


    // // // Accessors, standard properties:

    public boolean automaticNamespacesEnabled() {
        return hasConfigFlag(CFG_AUTOMATIC_NS);
    }

    // // // Accessors, Woodstox properties:

    public boolean automaticEmptyElementsEnabled() {
        return hasConfigFlag(CFG_AUTOMATIC_EMPTY_ELEMS);
    }

    public boolean willSupportNamespaces() {
        return hasConfigFlag(CFG_ENABLE_NS);
    }

    public boolean willOutputCDataAsText() {
        return hasConfigFlag(CFG_OUTPUT_CDATA_AS_TEXT);
    }

    public boolean willCopyDefaultAttrs() {
        return hasConfigFlag(CFG_COPY_DEFAULT_ATTRS);
    }

    public boolean willValidateStructure() {
        return hasConfigFlag(CFG_VALIDATE_STRUCTURE);
    }

    public boolean willValidateContent() {
        return hasConfigFlag(CFG_VALIDATE_CONTENT);
    }

    public boolean willValidateAttributes() {
        return hasConfigFlag(CFG_VALIDATE_ATTR);
    }

    public boolean willValidateNames() {
        return hasConfigFlag(CFG_VALIDATE_NAMES);
    }

    public boolean willFixContent() {
        return hasConfigFlag(CFG_FIX_CONTENT);
    }

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
     */
    public String getAutomaticNsPrefix() {
        return mAutoNsPrefix;
    }

    public EscapingWriterFactory getTextEscaperFactory() {
        return mTextEscaperFactory;
    }

    public EscapingWriterFactory getAttrValueEscaperFactory() {
        return mAttrValueEscaperFactory;
    }

    public XMLReporter getProblemReporter() {
        return mProblemReporter;
    }

    // // // Mutators:

    // Standard properies:

    public void enableAutomaticNamespaces(boolean state) {
        setConfigFlag(CFG_AUTOMATIC_NS, state);
    }

    // Wstx properies:

    public void enableAutomaticEmptyElements(boolean state) {
        setConfigFlag(CFG_AUTOMATIC_EMPTY_ELEMS, state);
    }

    public void doSupportNamespaces(boolean state) {
        setConfigFlag(CFG_ENABLE_NS, state);
    }

    public void doOutputCDataAsText(boolean state) {
        setConfigFlag(CFG_OUTPUT_CDATA_AS_TEXT, state);
    }

    public void doCopyDefaultAttrs(boolean state) {
        setConfigFlag(CFG_COPY_DEFAULT_ATTRS, state);
    }

    public void doValidateStructure(boolean state) {
        setConfigFlag(CFG_VALIDATE_STRUCTURE, state);
    }

    public void doValidateContent(boolean state) {
        setConfigFlag(CFG_VALIDATE_CONTENT, state);
    }

    public void doValidateAttributes(boolean state) {
        setConfigFlag(CFG_VALIDATE_ATTR, state);
    }

    public void doValidateNames(boolean state) {
        setConfigFlag(CFG_VALIDATE_NAMES, state);
    }

    public void doFixContent(boolean state) {
        setConfigFlag(CFG_FIX_CONTENT, state);
    }

    /**
     * @param prefix Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     */
    public void setAutomaticNsPrefix(String prefix) {
        mAutoNsPrefix = prefix;
    }

    public void setTextEscaperFactory(EscapingWriterFactory f) {
        mTextEscaperFactory = f;
    }

    public void setAttrValueEscaperFactory(EscapingWriterFactory f) {
        mAttrValueEscaperFactory = f;
    }

    public void setProblemReporter(XMLReporter rep) {
        mProblemReporter = rep;
    }

    /*
    //////////////////////////////////////////////////////////
    // Extended Woodstox API, profiles
    //////////////////////////////////////////////////////////
     */

    /**
     * For Woodstox, this profile enables all basic well-formedness checks,
     * including checking for name validity.
     */
    public void configureForXmlConformance()
    {
        doValidateAttributes(true);
        doValidateContent(true);
        doValidateStructure(true);
        doValidateNames(true);
    }

    /**
     * For Woodstox, this profile enables all basic well-formedness checks,
     * including checking for name validity, and also enables all matching
     * "fix-me" properties (currently only content-fixing property exists).
     */
    public void configureForRobustness()
    {
        doValidateAttributes(true);
        doValidateStructure(true);
        doValidateNames(true);

        /* This the actual "meat": we do want to not only check if the
         * content is ok, but also "fix" it if not, and if there's a way
         * to fix it:
         */
        doValidateContent(true);
        doFixContent(true);
    }

    /**
     * For Woodstox, setting this profile disables most checks for validity;
     * specifically anything that can have measurable performance impact.
     * 
     */
    public void configureForSpeed()
    {
        doValidateAttributes(false);
        doValidateContent(false);
        doValidateNames(false);

        // Structural validation is cheap: can be left enabled (if already so)
        //doValidateStructure(false);
    }

    /*
    //////////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////////
     */

    private void setConfigFlag(int flag, boolean state) {
        if (state) {
            mConfigFlags |= flag;
        } else {
            mConfigFlags &= ~flag;
        }
    }

    private boolean hasConfigFlag(int flag) {
        return ((mConfigFlags & flag) == flag);
    } 

    private int getPropertyId(String id) {
        Integer I = (Integer) sProperties.get(id);
        if (I == null) {
            throw new IllegalArgumentException("Property '"+id+"' not supported.");
        }
        return I.intValue();
    }
}
