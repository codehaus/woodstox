package wstxtest;

import java.io.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.codehaus.stax2.*;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.MinimalOutputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

/**
 * Simple test-driver that tries to exercise some of basic output factory
 * settings, like instantiating various writer instances, checking for
 * invalid arguments and so on.
 */
public class TestOutputFactory
    extends BaseWstxTest
{
    public void testSubsetFactories()
    {
        MinimalOutputFactory f = MinimalOutputFactory.newMinimalInstance();

        WriterConfig cfg = f.getConfig();
        assertNotNull(cfg);

        assertFalse(f.isPropertySupported("foobar"));
    }

    public void testStreamConstructors()
        throws XMLStreamException
    {
        // !!! TBI
    }

    public void testEventConstructors()
        throws XMLStreamException
    {
        // !!! TBI
    }

    public void testConfig()
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getNewOutputFactory();

        WriterConfig cfg = ((WstxOutputFactory) f).getConfig();
        assertNotNull(cfg);

        assertFalse(f.isPropertySupported("foobar"));

        // Let's just test some of known properties that should be supported...
        assertTrue(f.isPropertySupported(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE));
        assertTrue(f.isPropertySupported(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT));

        // And their default values?
        assertEquals(Boolean.TRUE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE));
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT));

        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR));
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES));
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_CDATA_AS_TEXT));
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_COPY_DEFAULT_ATTRS));
        assertEquals(Boolean.TRUE, f.getProperty(WstxOutputProperties.P_OUTPUT_FIX_CONTENT));
        assertEquals(Boolean.TRUE, f.getProperty(XMLOutputFactory2.P_AUTOMATIC_EMPTY_ELEMENTS));
        assertEquals(Boolean.TRUE, f.getProperty(XMLOutputFactory2.P_NAMESPACE_AWARE));

        assertNull(f.getProperty(XMLOutputFactory2.P_PROBLEM_REPORTER));
        assertNull(f.getProperty(XMLOutputFactory2.P_TEXT_ESCAPER));
        assertNull(f.getProperty(XMLOutputFactory2.P_ATTR_VALUE_ESCAPER));

        // ... which can be changed
        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE, Boolean.FALSE);
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE));

        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT, Boolean.TRUE);
        assertEquals(Boolean.TRUE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT));

        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT, Boolean.FALSE);
        assertEquals(Boolean.FALSE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT));

        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, Boolean.TRUE);
        assertEquals(Boolean.TRUE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES));
        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR, Boolean.TRUE);
        assertEquals(Boolean.TRUE, f.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR));
    }

    public void testMisc()
        throws XMLStreamException
    {
        /* This is silly, but coverage testing is not happy that our
         * constant-defining classes are never constructed. So here we go,
         * just to mark it off the list...
         */
        WstxInputProperties fooin = new WstxInputProperties();
        WstxOutputProperties fooout = new WstxOutputProperties();
    }
}
