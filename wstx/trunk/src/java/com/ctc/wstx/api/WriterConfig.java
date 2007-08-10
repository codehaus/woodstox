package com.ctc.wstx.api;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2; // for property consts
import org.codehaus.stax2.XMLStreamProperties;
import org.codehaus.stax2.io.EscapingWriterFactory;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.io.BufferRecycler;
import com.ctc.wstx.util.ArgUtil;

/**
 * Simple configuration container class; passed by reader factory to reader
 * instance created.
 */
public final class WriterConfig
    extends CommonConfig
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

    // Output settings:
    final static int PROP_OUTPUT_CDATA_AS_TEXT = 11;

    final static int PROP_COPY_DEFAULT_ATTRS = 12;

    final static int PROP_ESCAPE_CR = 13;

    final static int PROP_ADD_SPACE_AFTER_EMPTY_ELEM = 14;

    // Validation flags:

    final static int PROP_VALIDATE_STRUCTURE = 15; 
    final static int PROP_VALIDATE_CONTENT = 16;
    final static int PROP_VALIDATE_ATTR = 17;
    final static int PROP_VALIDATE_NAMES = 18;
    final static int PROP_FIX_CONTENT = 19;

    // Per-writer instance information

    final static int PROP_UNDERLYING_STREAM = 30;
    final static int PROP_UNDERLYING_WRITER = 31;

    // // // Default settings for additional properties:

    final static boolean DEFAULT_ENABLE_NS = true;

    /* 27-Apr-2005, TSa: Changed the default to 'true' for 2.0rc1,
     *   since usually it is beneficial to still allow for empty
     *   elements...
     */
    final static boolean DEFAULT_AUTOMATIC_EMPTY_ELEMS = true;

    final static boolean DEFAULT_OUTPUT_CDATA_AS_TEXT = false;
    final static boolean DEFAULT_COPY_DEFAULT_ATTRS = false;

    /* 26-Dec-2006, TSa: Since CRs have been auto-escaped so far, let's
     *   retain the defaults when adding new properties/features.
     */
    final static boolean DEFAULT_ESCAPE_CR = true;

    /**
     * 09-Aug-2007, TSa: Space has always been added after empty
     *   element (before closing "/>"), but now it is configurable.
     *   Default left as true for backwards compatibility.
     */
    final static boolean DEFAULT_ADD_SPACE_AFTER_EMPTY_ELEM = true;

    /* How about validation? Let's turn them mostly off by default, since
     * there are some performance hits when enabling them.
     */

    // Structural checks are easy, cheap and useful...
    final static boolean DEFAULT_VALIDATE_STRUCTURE = true;

    /* 17-May-2006, TSa: Since content validation is now much cheaper
     *   (due to integrated transcoders) than it used to be, let's
     *   just enabled content validation too.
     */
    final static boolean DEFAULT_VALIDATE_CONTENT = true;
    final static boolean DEFAULT_VALIDATE_ATTR = false;
    final static boolean DEFAULT_VALIDATE_NAMES = false;

    // This only matters if content validation is enabled...
    /**
     * As per [WSTX-120], default was changed to false,
     * from true (default prior to wstx 4.0)
     */
    //final static boolean DEFAULT_FIX_CONTENT = true;
    final static boolean DEFAULT_FIX_CONTENT = false;

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
        | (DEFAULT_ESCAPE_CR ? CFG_ESCAPE_CR : 0)
        | (DEFAULT_ADD_SPACE_AFTER_EMPTY_ELEM ? CFG_ESCAPE_CR : 0)

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
        // // StAX (1.0) standard ones:
        sProperties.put(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                        Integer.valueOf(PROP_AUTOMATIC_NS));

        // // StAX2 standard ones:

        // Namespace support
        sProperties.put(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                        Integer.valueOf(PROP_ENABLE_NS));

        // Generic output
        sProperties.put(XMLOutputFactory2.P_AUTOMATIC_EMPTY_ELEMENTS,
                        Integer.valueOf(PROP_AUTOMATIC_EMPTY_ELEMS));
        // Namespace support
        sProperties.put(XMLOutputFactory2.P_AUTOMATIC_NS_PREFIX,
                        Integer.valueOf(PROP_AUTOMATIC_NS_PREFIX));
        // Text/attr value escaping (customized escapers)
        sProperties.put(XMLOutputFactory2.P_TEXT_ESCAPER,
                        Integer.valueOf(PROP_TEXT_ESCAPER));
        sProperties.put(XMLOutputFactory2.P_ATTR_VALUE_ESCAPER,
                        Integer.valueOf(PROP_ATTR_VALUE_ESCAPER));
        // Problem checking/reporting options
        sProperties.put(XMLStreamProperties.XSP_PROBLEM_REPORTER,
                        Integer.valueOf(PROP_PROBLEM_REPORTER));

        // // Woodstox-specifics:

        // Output conversions
        sProperties.put(WstxOutputProperties.P_OUTPUT_CDATA_AS_TEXT,
                        Integer.valueOf(PROP_OUTPUT_CDATA_AS_TEXT));
        sProperties.put(WstxOutputProperties.P_COPY_DEFAULT_ATTRS,
                        Integer.valueOf(PROP_COPY_DEFAULT_ATTRS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_ESCAPE_CR,
                        Integer.valueOf(PROP_ESCAPE_CR));
        sProperties.put(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM
,
                        Integer.valueOf(PROP_ADD_SPACE_AFTER_EMPTY_ELEM));

        // Validation settings:
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE,
                        Integer.valueOf(PROP_VALIDATE_STRUCTURE));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT,
                        Integer.valueOf(PROP_VALIDATE_CONTENT));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR,
                        Integer.valueOf(PROP_VALIDATE_ATTR));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES,
                        Integer.valueOf(PROP_VALIDATE_NAMES));
        sProperties.put(WstxOutputProperties.P_OUTPUT_FIX_CONTENT,
                        Integer.valueOf(PROP_FIX_CONTENT));

        // Underlying stream/writer access
        sProperties.put(WstxOutputProperties.P_OUTPUT_UNDERLYING_STREAM,
                        Integer.valueOf(PROP_UNDERLYING_STREAM));
        sProperties.put(WstxOutputProperties.P_OUTPUT_UNDERLYING_STREAM,
                        Integer.valueOf(PROP_UNDERLYING_STREAM));
    }

    /*
    //////////////////////////////////////////////////////////
    // Current config state:
    //////////////////////////////////////////////////////////
     */

    final boolean mIsJ2MESubset;

    protected int mConfigFlags;

    /*
    //////////////////////////////////////////////////////////
    // More special(ized) configuration objects
    //////////////////////////////////////////////////////////
     */

    //protected String mAutoNsPrefix;
    //protected EscapingWriterFactory mTextEscaperFactory = null;
    //protected EscapingWriterFactory mAttrValueEscaperFactory = null;
    //protected XMLReporter mProblemReporter = null;

    Object[] mSpecialProperties = null;

    private final static int SPEC_PROC_COUNT = 4;

    private final static int SP_IX_AUTO_NS_PREFIX = 0;
    private final static int SP_IX_TEXT_ESCAPER_FACTORY = 1;
    private final static int SP_IX_ATTR_VALUE_ESCAPER_FACTORY = 2;
    private final static int SP_IX_PROBLEM_REPORTER = 3;

    /*
    //////////////////////////////////////////////////////////
    // Buffer recycling:
    //////////////////////////////////////////////////////////
     */

    /**
     * This <code>ThreadLocal</code> contains a {@link SoftRerefence}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling between Reader instances.
     */
    final static ThreadLocal mRecyclerRef = new ThreadLocal();

    /**
     * This is the actually container of the recyclable buffers. It
     * is obtained via ThreadLocal/SoftReference combination, if one
     * exists, when Config instance is created. If one does not
     * exists, it will created first time a buffer is returned.
     */
    BufferRecycler mCurrRecycler = null;

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle:
    //////////////////////////////////////////////////////////
     */

    private WriterConfig(boolean j2meSubset, int flags, Object[] specProps)
    {
        mIsJ2MESubset = j2meSubset;
        mConfigFlags = flags;
        mSpecialProperties = specProps;

        /* Ok, let's then see if we can find a buffer recycler. Since they
         * are lazily constructed, and since GC may just flush them out
         * on its whims, it's possible we might not find one. That's ok;
         * we can reconstruct one if and when we are to return one or more
         * buffers.
         */
        SoftReference ref = (SoftReference) mRecyclerRef.get();
        if (ref != null) {
            mCurrRecycler = (BufferRecycler) ref.get();
        }
    }

    public static WriterConfig createJ2MEDefaults()
    {
        return new WriterConfig(true, DEFAULT_FLAGS_J2ME, null);
    }

    public static WriterConfig createFullDefaults()
    {
        return new WriterConfig(true, DEFAULT_FLAGS_FULL, null);
    }

    public WriterConfig createNonShared()
    {
        Object[] specProps;

        if (mSpecialProperties != null) {
            int len = mSpecialProperties.length;
            specProps = new Object[len];
            System.arraycopy(mSpecialProperties, 0, specProps, 0, len);
        } else {
            specProps = null;
        }
        return new WriterConfig(mIsJ2MESubset, mConfigFlags, specProps);
    }

    /*
    //////////////////////////////////////////////////////////
    // Implementation of abstract methods
    //////////////////////////////////////////////////////////
     */

    protected int findPropertyId(String propName)
    {
        Integer I = (Integer) sProperties.get(propName);
        return (I == null) ? -1 : I.intValue();
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    public Object getProperty(int id)
    {
        switch (id) {

        // First, Stax 1.0 properties:

        case PROP_AUTOMATIC_NS:
            return automaticNamespacesEnabled() ? Boolean.TRUE : Boolean.FALSE;

        // Then Stax2 properties:

            // First, properties common to input/output factories:

        case PROP_ENABLE_NS:
            return willSupportNamespaces() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_PROBLEM_REPORTER:
            return getProblemReporter();

            // Then output-specific properties:
        case PROP_AUTOMATIC_EMPTY_ELEMS:
            return automaticEmptyElementsEnabled() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_AUTOMATIC_NS_PREFIX:
            return getAutomaticNsPrefix();
        case PROP_TEXT_ESCAPER:
            return getTextEscaperFactory();
        case PROP_ATTR_VALUE_ESCAPER:
            return getAttrValueEscaperFactory();

        // // // Then Woodstox-specific properties:

        case PROP_OUTPUT_CDATA_AS_TEXT:
            return willOutputCDataAsText() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_COPY_DEFAULT_ATTRS:
            return willCopyDefaultAttrs() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_ESCAPE_CR:
            return willEscapeCr() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_ADD_SPACE_AFTER_EMPTY_ELEM:
            return willAddSpaceAfterEmptyElem() ? Boolean.TRUE : Boolean.FALSE;

        case PROP_VALIDATE_STRUCTURE:
            return willValidateStructure() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_CONTENT:
            return willValidateContent() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_ATTR:
            return willValidateAttributes() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_VALIDATE_NAMES:
            return willValidateNames() ? Boolean.TRUE : Boolean.FALSE;
        case PROP_FIX_CONTENT:
            return willFixContent() ? Boolean.TRUE : Boolean.FALSE;


            // And then per-instance properties: not valid via config object
        case PROP_UNDERLYING_STREAM:
        case PROP_UNDERLYING_WRITER:
            throw new IllegalStateException("Can not access per-stream-writer properties via factory");
        }

        throw new Error("Internal error: no handler for property with internal id "+id+".");
    }

    /**
     * @return True, if the specified property was <b>succesfully</b>
     *    set to specified value; false if its value was not changed
     */
    public boolean setProperty(String name, int id, Object value)
    {
        switch (id) {
        // First, Stax 1.0 properties:

        case PROP_AUTOMATIC_NS:
            enableAutomaticNamespaces(ArgUtil.convertToBoolean(name, value));
            break;

       // // // Then Stax2 ones:

        case PROP_ENABLE_NS:
            doSupportNamespaces(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_PROBLEM_REPORTER:
            setProblemReporter((XMLReporter) value);
            break;

        case PROP_AUTOMATIC_EMPTY_ELEMS:
            enableAutomaticEmptyElements(ArgUtil.convertToBoolean(name, value));
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

            // // // Then Woodstox-specific ones:

        case PROP_OUTPUT_CDATA_AS_TEXT:
            doOutputCDataAsText(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_COPY_DEFAULT_ATTRS:
            doCopyDefaultAttrs(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_ESCAPE_CR:
            doEscapeCr(ArgUtil.convertToBoolean(name, value));
            break;
        case PROP_ADD_SPACE_AFTER_EMPTY_ELEM:
            doAddSpaceAfterEmptyElem(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_VALIDATE_STRUCTURE:
            doValidateStructure(ArgUtil.convertToBoolean(name, value));
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
        case PROP_FIX_CONTENT:
            doFixContent(ArgUtil.convertToBoolean(name, value));
            break;

        case PROP_UNDERLYING_STREAM:
        case PROP_UNDERLYING_WRITER:
            throw new IllegalStateException("Can not modify per-stream-writer properties via factory");

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

    public boolean willEscapeCr() {
        return hasConfigFlag(CFG_ESCAPE_CR);
    }

    public boolean willAddSpaceAfterEmptyElem() {
        return hasConfigFlag(CFG_ADD_SPACE_AFTER_EMPTY_ELEM);
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
        String prefix = (String) getSpecialProperty(SP_IX_AUTO_NS_PREFIX);
        if (prefix == null) {
            prefix = DEFAULT_AUTOMATIC_NS_PREFIX;
        }
        return prefix;
    }

    public EscapingWriterFactory getTextEscaperFactory() {
        return (EscapingWriterFactory) getSpecialProperty(SP_IX_TEXT_ESCAPER_FACTORY);
    }

    public EscapingWriterFactory getAttrValueEscaperFactory() {
        return (EscapingWriterFactory) getSpecialProperty(SP_IX_ATTR_VALUE_ESCAPER_FACTORY);
    }

    public XMLReporter getProblemReporter() {
        return (XMLReporter) getSpecialProperty(SP_IX_PROBLEM_REPORTER);
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

    public void doEscapeCr(boolean state) {
        setConfigFlag(CFG_ESCAPE_CR, state);
    }

    public void doAddSpaceAfterEmptyElem(boolean state) {
        setConfigFlag(CFG_ADD_SPACE_AFTER_EMPTY_ELEM, state);
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
        setSpecialProperty(SP_IX_AUTO_NS_PREFIX, prefix);
    }

    public void setTextEscaperFactory(EscapingWriterFactory f) {
        setSpecialProperty(SP_IX_TEXT_ESCAPER_FACTORY, f);
    }

    public void setAttrValueEscaperFactory(EscapingWriterFactory f) {
        setSpecialProperty(SP_IX_ATTR_VALUE_ESCAPER_FACTORY, f);
    }

    public void setProblemReporter(XMLReporter rep) {
        setSpecialProperty(SP_IX_PROBLEM_REPORTER, rep);
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
    /////////////////////////////////////////////////////
    // Buffer recycling:
    /////////////////////////////////////////////////////
     */

    /**
     * Method called to allocate intermediate recyclable copy buffers
     */
    public char[] allocMediumCBuffer(int minSize)
    {
//System.err.println("DEBUG: cfg, allocCMedium: "+mCurrRecycler);
        if (mCurrRecycler != null) {
            char[] result = mCurrRecycler.getMediumCBuffer(minSize);
            if (result != null) {
                return result;
            }
        }
        return new char[minSize];
    }

    public void freeMediumCBuffer(char[] buffer)
    {
//System.err.println("DEBUG: cfg, freeCMedium: "+buffer);
        // Need to create (and assign) the buffer?
        if (mCurrRecycler == null) {
            mCurrRecycler = createRecycler();
        }
        mCurrRecycler.returnMediumCBuffer(buffer);
    }

    public char[] allocFullCBuffer(int minSize)
    {
//System.err.println("DEBUG: cfg, allocCFull: "+mCurrRecycler);
        if (mCurrRecycler != null) {
            char[] result = mCurrRecycler.getFullCBuffer(minSize);
            if (result != null) {
                return result;
            }
        }
        return new char[minSize];
    }

    public void freeFullCBuffer(char[] buffer)
    {
//System.err.println("DEBUG: cfg, freeCFull: "+buffer);
        // Need to create (and assign) the buffer?
        if (mCurrRecycler == null) {
            mCurrRecycler = createRecycler();
        }
        mCurrRecycler.returnFullCBuffer(buffer);
    }

    public byte[] allocFullBBuffer(int minSize)
    {
//System.err.println("DEBUG: cfg, allocBFull: "+mCurrRecycler);
        if (mCurrRecycler != null) {
            byte[] result = mCurrRecycler.getFullBBuffer(minSize);
            if (result != null) {
                return result;
            }
        }
        return new byte[minSize];
    }

    public void freeFullBBuffer(byte[] buffer)
    {
//System.err.println("DEBUG: cfg, freeBFull: "+buffer);
        // Need to create (and assign) the buffer?
        if (mCurrRecycler == null) {
            mCurrRecycler = createRecycler();
        }
        mCurrRecycler.returnFullBBuffer(buffer);
    }

    static int Counter = 0;

    private BufferRecycler createRecycler()
    {
        BufferRecycler recycler = new BufferRecycler();
        // No way to reuse/reset SoftReference, have to create new always:
//System.err.println("DEBUG: RefCount: "+(++Counter));
        mRecyclerRef.set(new SoftReference(recycler));
        return recycler;
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

    private final boolean hasConfigFlag(int flag) {
        return ((mConfigFlags & flag) == flag);
    } 

    private final Object getSpecialProperty(int ix)
    {
        if (mSpecialProperties == null) {
            return null;
        }
        return mSpecialProperties[ix];
    }

    private final void setSpecialProperty(int ix, Object value)
    {
        if (mSpecialProperties == null) {
            mSpecialProperties = new Object[SPEC_PROC_COUNT];
        }
        mSpecialProperties[ix] = value;
    }
}
