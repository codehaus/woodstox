package com.ctc.wstx.api;

import javax.xml.stream.XMLOutputFactory;

/**
 * Interface that defines extended Woodstox Output Factory API; methods
 * that output factories implement above and beyond default StAX API.
 * Additionally it does contain those StAX API methods that are directly
 * related to configuration settings (but not actual factory methods).
 *<p>
 * Interface-based approach was chosen because of problems with trying
 * to extend abstract StAX factory classes.
 */
public interface WstxOutputFactoryConfig
{
    /*
    ////////////////////////////////////////////////
    // Methods shared with StAX XMLOutputFactory
    ////////////////////////////////////////////////
     */

    /**
     * Method from {@link XMLOutputFactory}; included here
     * for convenience
     */
    public Object getProperty(String name);

    /**
     * Method from {@link XMLOutputFactory}; included here
     * for convenience
     */
    public boolean isPropertySupported(String name);

    /**
     * Method from {@link XMLOutputFactory}; included here
     * for convenience
     */
    public void setProperty(String propName, Object value);

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, simple accessors
    ////////////////////////////////////////////////
     */

    // Standard StAX properties:

    public boolean automaticNamespacesEnabled();

    // Wstx properies:

    public boolean willSupportNamespaces();

    public boolean willOutputEmptyElements();

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
     */
    public String getAutomaticNsPrefix();

    public boolean willValidateNamespaces();

    public boolean willValidateStructure();

    public boolean willValidateContent();

    public boolean willValidateAttributes();

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, simple mutators
    ////////////////////////////////////////////////
     */

    // Standard StAX properies:

    public void enableAutomaticNamespaces(boolean state);

    // Wstx properies:

    public void doSupportNamespaces(boolean state);

    public void doOutputEmptyElements(boolean state);

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
     */
    public void setAutomaticNsPrefix(String prefix);

    public void doValidateNamespaces(boolean state);

    public void doValidateStructure(boolean state);

    public void doValidateContent(boolean state);

    public void doValidateAttributes(boolean state);

    /*
    ////////////////////////////////////////////////
    // Extended Woodstox API, profile setters
    ////////////////////////////////////////////////
     */

    /**
     * Method call to make writer be as strict (anal) with output as possible,
     * ie maximize validation it does to try to catch any well-formedness
     * or validity problems. In a way, reverse of calling
     * {@link #configureForMinValidation}.
     */
    public void configureForMaxValidation();

    /**
     * Method call to make writer be as lenient with output as possible,
     * ie minimize validation it does. In a way, reverse of calling
     * {@link #configureForMaxValidation}.
     */
    public void configureForMinValidation();
}
