package stax2.stream;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

/**
 * Set of unit tests that checks that configuring of
 * {@link XMLInputFactory2} works ok.
 */
public class TestConfig
    extends BaseStax2Test
{
    public void testProfiles()
        throws XMLStreamException
    {
        // configureForXmlConformance
        XMLInputFactory2 ifact = getNewInputFactory();
        ifact.configureForXmlConformance();
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));

        // configureForConvenience
        ifact = getNewInputFactory();
        ifact.configureForConvenience();
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.IS_COALESCING));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory2.P_REPORT_ALL_TEXT_AS_CHARACTERS));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory2.P_PRESERVE_LOCATION));

        // configureForSpeed
        ifact = getNewInputFactory();
        ifact.configureForSpeed();
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory.IS_COALESCING));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory2.P_PRESERVE_LOCATION));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory2.P_INTERN_NAMES));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory2.P_INTERN_NS_URIS));

        // configureForLowMemUsage
        ifact = getNewInputFactory();
        ifact.configureForLowMemUsage();
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory.IS_COALESCING));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory2.P_PRESERVE_LOCATION));

        // configureForRoundTripping
        ifact = getNewInputFactory();
        ifact.configureForRoundTripping();
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory.IS_COALESCING));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        assertEquals(Boolean.FALSE, ifact.getProperty(XMLInputFactory2.P_REPORT_ALL_TEXT_AS_CHARACTERS));
        assertEquals(Boolean.TRUE, ifact.getProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE));
    }
}
