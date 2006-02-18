package wstxtest.util;

import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.util.DataUtil;

/**
 * Simple unit tests for testing methods in {@link com.ctc.wstx.util.XmlChars}
 * and {@link com.ctc.wstx.io.WstxInputData}
 */
public class TestXmlChars
    extends TestCase
{
    public void testXml10Chars()
    {
        // First, 8-bit range:
        assertTrue(WstxInputData.is10NameStartChar('F'));
        assertTrue(WstxInputData.is10NameChar('F'));
        assertTrue(WstxInputData.is10NameStartChar('_'));
        assertTrue(WstxInputData.is10NameChar('_'));
        assertTrue(WstxInputData.is10NameChar('x'));
        assertFalse(WstxInputData.is10NameStartChar('-'));
        assertTrue(WstxInputData.is10NameChar('-'));
        assertFalse(WstxInputData.is10NameStartChar('.'));
        assertTrue(WstxInputData.is10NameChar('.'));

        // Then more exotic chars:

        assertTrue(WstxInputData.is10NameStartChar((char) 0x03ce));
        assertTrue(WstxInputData.is10NameChar((char) 0x03ce));
        assertTrue(WstxInputData.is10NameStartChar((char) 0x0e21));
        assertTrue(WstxInputData.is10NameChar((char) 0x0e21));
        assertTrue(WstxInputData.is10NameStartChar((char) 0x3007));
        assertFalse(WstxInputData.is10NameStartChar(' '));
        /* colon is NOT a start char for this method; although it is
         * in xml specs -- reason has to do with namespace handling
         */
        assertFalse(WstxInputData.is10NameStartChar(':'));

        assertFalse(WstxInputData.is10NameStartChar((char) 0x3008));
        assertFalse(WstxInputData.is10NameChar((char) 0x3008));
        assertTrue(WstxInputData.is10NameStartChar((char) 0x30ea));
        assertTrue(WstxInputData.is10NameChar((char) 0x30ea));
    }

    public void testXml11NameStartChars()
    {
        // First, 8-bit range:
        assertTrue(WstxInputData.is11NameStartChar('F'));
        assertTrue(WstxInputData.is11NameChar('F'));
        assertTrue(WstxInputData.is11NameStartChar('_'));
        assertTrue(WstxInputData.is11NameChar('_'));
        assertTrue(WstxInputData.is11NameChar('x'));
        assertFalse(WstxInputData.is11NameStartChar('-'));
        assertTrue(WstxInputData.is11NameChar('-'));
        assertFalse(WstxInputData.is11NameStartChar('.'));
        assertTrue(WstxInputData.is11NameChar('.'));

        // Then more exotic chars:

        assertTrue(WstxInputData.is11NameStartChar((char) 0x03ce));
        assertTrue(WstxInputData.is11NameChar((char) 0x03ce));
        assertTrue(WstxInputData.is11NameStartChar((char) 0x0e21));
        assertTrue(WstxInputData.is11NameChar((char) 0x0e21));
        assertTrue(WstxInputData.is11NameStartChar((char) 0x3007));
        assertFalse(WstxInputData.is11NameStartChar(' '));
        /* colon is NOT a start char for this method; although it is
         * in xml specs -- reason has to do with namespace handling
         */
        assertFalse(WstxInputData.is11NameStartChar(':'));
        assertFalse(WstxInputData.is11NameStartChar((char) 0x3000));
    }
}
