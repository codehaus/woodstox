package com.ctc.wstx.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.*;

import org.codehaus.stax2.EscapingWriterFactory;

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

    // Namespace support, settings:
    final static int PROP_AUTOMATIC_NS = 1; // standard property ("repairing")
    final static int PROP_ENABLE_NS = 2;
    final static int PROP_AUTOMATIC_NS_PREFIX = 3;

    // Output settings:
    final static int PROP_OUTPUT_EMPTY_ELEMS = 4;
    final static int PROP_OUTPUT_CDATA_AS_TEXT = 5;

    // Validation flags:
    final static int PROP_VALIDATE_NS = 6;
    final static int PROP_VALIDATE_STRUCTURE = 7;
    final static int PROP_VALIDATE_CONTENT = 8;
    final static int PROP_VALIDATE_ATTR = 9;
    final static int PROP_VALIDATE_NAMES = 10;

    // Escaping text content/attr values:
    final static int PROP_TEXT_ESCAPER = 11;
    final static int PROP_ATTR_VALUE_ESCAPER = 12;

    // // // Default settings for additional properties:

    final static boolean DEFAULT_ENABLE_NS = true;
    final static boolean DEFAULT_OUTPUT_EMPTY_ELEMS = false;

    /* How about validation? Let's turn them mostly off by default, since
     * there are some performance hits when enabling them.
     */

    final static boolean DEFAULT_VALIDATE_NS = false;
    // Structural checks are easy, cheap and useful...
    final static boolean DEFAULT_VALIDATE_STRUCTURE = true;
    final static boolean DEFAULT_VALIDATE_CONTENT = false;
    final static boolean DEFAULT_VALIDATE_ATTR = false;
    final static boolean DEFAULT_VALIDATE_NAMES = false;

    /**
     * Default config flags are converted from individual settings,
     * to conform to StAX 1.0 specifications.
     */
    final static int DEFAULT_FLAGS_J2ME =
        0 // | CFG_AUTOMATIC_NS
        | (DEFAULT_ENABLE_NS ? CFG_ENABLE_NS : 0)
        | (DEFAULT_OUTPUT_EMPTY_ELEMS ? CFG_OUTPUT_EMPTY_ELEMS : 0)
        | (DEFAULT_VALIDATE_NS ? CFG_VALIDATE_NS : 0)
        | (DEFAULT_VALIDATE_STRUCTURE ? CFG_VALIDATE_STRUCTURE : 0)
        | (DEFAULT_VALIDATE_CONTENT ? CFG_VALIDATE_CONTENT : 0)
        | (DEFAULT_VALIDATE_ATTR ? CFG_VALIDATE_ATTR : 0)
        | (DEFAULT_VALIDATE_NAMES ? CFG_VALIDATE_NAMES : 0)
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

        // // Non-standard ones:

        // Namespace support
        sProperties.put(WstxOutputProperties.P_OUTPUT_ENABLE_NS,
                        new Integer(PROP_ENABLE_NS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_AUTOMATIC_NS_PREFIX,
                        new Integer(PROP_AUTOMATIC_NS_PREFIX));

        // Output conversions
        sProperties.put(WstxOutputProperties.P_OUTPUT_EMPTY_ELEMENTS,
                        new Integer(PROP_OUTPUT_EMPTY_ELEMS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_CDATA_AS_TEXT,
                        new Integer(PROP_OUTPUT_CDATA_AS_TEXT));

        // Validation settings:
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_NS,
                        new Integer(PROP_VALIDATE_NS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE,
                        new Integer(PROP_VALIDATE_STRUCTURE));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT,
                        new Integer(PROP_VALIDATE_CONTENT));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR,
                        new Integer(PROP_VALIDATE_ATTR));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES,
                        new Integer(PROP_VALIDATE_NAMES));

        // Text/attr value escaping (customized escapers)

        sProperties.put(WstxOutputProperties.P_OUTPUT_TEXT_ESCAPER,
                        new Integer(PROP_TEXT_ESCAPER));
        sProperties.put(WstxOutputProperties.P_OUTPUT_ATTR_VALUE_ESCAPER,
                        new Integer(PROP_ATTR_VALUE_ESCAPER));
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

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle:
    //////////////////////////////////////////////////////////
     */

    private WriterConfig(boolean j2meSubset, int flags, String autoNsPrefix,
                         EscapingWriterFactory textEscaperF,
                         EscapingWriterFactory attrValueEscaperF)
    {
        mIsJ2MESubset = j2meSubset;
        mConfigFlags = flags;
        mAutoNsPrefix = autoNsPrefix;
        mTextEscaperFactory = textEscaperF;
        mAttrValueEscaperFactory = attrValueEscaperF;
    }

    public static WriterConfig createJ2MEDefaults()
    {
        WriterConfig rc = new WriterConfig
            (true, DEFAULT_FLAGS_J2ME, DEFAULT_AUTOMATIC_NS_PREFIX,
             null, null);
        return rc;
    }

    public static WriterConfig createFullDefaults()
    {
        WriterConfig rc = new WriterConfig
            (true, DEFAULT_FLAGS_FULL, DEFAULT_AUTOMATIC_NS_PREFIX,
             null, null);
        return rc;
    }

    public WriterConfig createNonShared()
    {
        WriterConfig rc = new WriterConfig(mIsJ2MESubset,
                                           mConfigFlags, mAutoNsPrefix,
                                           mTextEscaperFactory,
                                           mAttrValueEscaperFactory);
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

        case PROP_OUTPUT_EMPTY_ELEMS:
            return willOutputEmptyElements() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_OUTPUT_CDATA_AS_TEXT:
            return willOutputCDataAsText() ? Boolean.TRUE : Boolean.FALSE;

        case PROP_VALIDATE_NS:
            return willValidateNamespaces() ? Boolean.TRUE : Boolean.FALSE;
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
        }

        throw new Error("Internal error: no handler for property with internal id "+id+".");
    }

    public void setProperty(String name, Object value)
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

        case PROP_OUTPUT_EMPTY_ELEMS:
            doOutputEmptyElements(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_OUTPUT_CDATA_AS_TEXT:
            doOutputCDataAsText(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_NS:
            doValidateNamespaces(ArgUtil.convertToBoolean(name, value));
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

        default:
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }
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

    public boolean willSupportNamespaces() {
        return hasConfigFlag(CFG_ENABLE_NS);
    }

    public boolean willOutputEmptyElements() {
        return hasConfigFlag(CFG_OUTPUT_EMPTY_ELEMS);
    }

    public boolean willOutputCDataAsText() {
        return hasConfigFlag(CFG_OUTPUT_CDATA_AS_TEXT);
    }

    public boolean willValidateNamespaces() {
        return hasConfigFlag(CFG_VALIDATE_NS);
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

    // // // Mutators:

    // Standard properies:

    public void enableAutomaticNamespaces(boolean state) {
        setConfigFlag(CFG_AUTOMATIC_NS, state);
    }

    // Wstx properies:

    public void doSupportNamespaces(boolean state) {
        setConfigFlag(CFG_ENABLE_NS, state);
    }

    public void doOutputEmptyElements(boolean state) {
        setConfigFlag(CFG_OUTPUT_EMPTY_ELEMS, state);
    }

    public void doOutputCDataAsText(boolean state) {
        setConfigFlag(CFG_OUTPUT_CDATA_AS_TEXT, state);
    }

    public void doValidateNamespaces(boolean state) {
        setConfigFlag(CFG_VALIDATE_NS, state);
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

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
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

    /*
    //////////////////////////////////////////////////////////
    // Extended Woodstox API, profiles
    //////////////////////////////////////////////////////////
     */

    /**
     * Method call to make writer be as strict (anal) with output as possible,
     * ie maximize validation it does to try to catch any well-formedness
     * or validity problems. In a way, reverse of calling
     * {@link #configureForMinValidation}.
     */
    public void configureForMaxValidation()
    {
        doValidateAttributes(true);
        doValidateContent(true);
        doValidateNamespaces(true);
        doValidateStructure(true);
        doValidateNames(true);
    }

    /**
     * Method call to make writer be as lenient with output as possible,
     * ie minimize validation it does. In a way, reverse of calling
     * {@link #configureForMaxValidation}.
     */
    public void configureForMinValidation()
    {
        doValidateAttributes(false);
        doValidateContent(false);
        doValidateNamespaces(false);
        doValidateStructure(false);
        doValidateNames(false);
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
