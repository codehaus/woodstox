package com.ctc.wstx.api;

import java.util.*;

import org.codehaus.stax2.XMLStreamProperties;

/**
 * Shared common base class for variour configuration container implementations
 * for public factories Woodstox uses: implementations of
 * {@link javax.xml.stream.XMLInputFactory},
 * {@link javax.xml.stream.XMLOutputFactory} and
 * {@link org.codehaus.stax2.validation.XMLValidationSchemaFactory}.
 * Implements basic settings for some shared settings, defined by the
 * shared property interface {@link XMLStreamProperties}.
 */
abstract class CommonConfig
    implements XMLStreamProperties
{
    /*
    ////////////////////////////////////////////////
    // Implementation info
    ////////////////////////////////////////////////
    */

    protected final static String IMPL_NAME = "woodstox";
    
    /* !!! TBI: get from props file or so? Or build as part of Ant
     *    build process?
     */
    protected final static String IMPL_VERSION = "3.0.3";

    /*
    ////////////////////////////////////////////////
    // Internal constants
    ////////////////////////////////////////////////
    */

    final static int PROP_IMPL_NAME = 1;
    final static int PROP_IMPL_VERSION = 2;

    final static int PROP_SUPPORTS_XML11 = 3;

    /**
     * Map to use for converting from String property ids to enumeration
     * (ints). Used for faster dispatching.
     */
    final static HashMap sStdProperties = new HashMap(16);
    static {
        // Basic information about the implementation:
        sStdProperties.put(XMLStreamProperties.XSP_IMPLEMENTATION_NAME,
                        new Integer(PROP_IMPL_NAME));
        sStdProperties.put(XMLStreamProperties.XSP_IMPLEMENTATION_VERSION,
                        new Integer(PROP_IMPL_VERSION));

        // XML version support:
        sStdProperties.put(XMLStreamProperties.XSP_SUPPORTS_XML11,
                        new Integer(PROP_SUPPORTS_XML11));
    }

    protected CommonConfig() { }

    /*
    //////////////////////////////////////////////////////////
    // Public API, generic StAX config methods
    //////////////////////////////////////////////////////////
     */

    public final Object getProperty(String propName)
    {
        int id = findPropertyId(propName);
        if (id >= 0) {
            return getProperty(id);
        }
        id = findStdPropertyId(propName);
        if (id < 0) {
            throw new IllegalArgumentException("Unrecognized property '"+propName+"'");
        }
        return getStdProperty(id);
    }

    public final boolean isPropertySupported(String propName)
    {
        return (findPropertyId(propName) >= 0)
            || (findStdPropertyId(propName) >= 0);
    }

    /**
     * @return True, if the specified property was <b>succesfully</b>
     *    set to specified value; false if its value was not changed
     */
    public final boolean setProperty(String propName, Object value)
    {
        int id = findPropertyId(propName);
        if (id >= 0) {
            return setProperty(propName, id, value);
        }
        id = findStdPropertyId(propName);
        if (id < 0) {
            throw new IllegalArgumentException("Unrecognized property '"+propName+"'");
        }
        return setStdProperty(propName, id, value);
    }

    /*
    //////////////////////////////////////////////////////////
    // Interface sub-classes have to implement / can override
    //////////////////////////////////////////////////////////
     */

    /**
     * @return Internal enumerated int matching the String name
     *   of the property, if one found: -1 to indicate no match
     *   was found.
     */
    protected abstract int findPropertyId(String propName);

    protected boolean doesSupportXml11() {
        /* Woodstox does support xml 1.1 ... but sub-classes can
         * override it if/as necessary (validator factories might not
         * support it?)
         */
        return true;
    }

    protected abstract Object getProperty(int id);

    protected abstract boolean setProperty(String propName, int id, Object value);

    /*
    //////////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////////
     */

    protected int findStdPropertyId(String propName)
    {
        Integer I = (Integer) sStdProperties.get(propName);
        return (I == null) ? -1 : I.intValue();
    }

    protected boolean setStdProperty(String propName, int id, Object value)
    {
        // None of the current shared properties are settable...
        return false;
    }

    protected Object getStdProperty(int id)
    {
        switch (id) {
        case PROP_IMPL_NAME:
            return IMPL_NAME;
        case PROP_IMPL_VERSION:
            return IMPL_VERSION;
        case PROP_SUPPORTS_XML11:
            return doesSupportXml11() ? Boolean.TRUE : Boolean.FALSE;
        default: // sanity check, should never happen
            throw new Error("Internal error: no handler for property with internal id "+id+".");
        }
    }
}
