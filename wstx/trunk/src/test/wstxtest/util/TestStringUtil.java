package wstxtest.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.util.StringUtil;

/**
 * Simple unit tests for testing methods of {@link StringUtil} utility
 * class.
 */
public class TestStringUtil
    extends TestCase
{
    public TestStringUtil(String name) {
        super(name);
    }


    public void testIsAllWhitespace()
    {
        assertTrue(StringUtil.isAllWhitespace("  \r   \r\n    \t"));
        assertTrue(StringUtil.isAllWhitespace(" "));
        assertTrue(StringUtil.isAllWhitespace(" ".toCharArray(), 0, 1));
        assertTrue(StringUtil.isAllWhitespace("\r\n\t"));
        assertTrue(StringUtil.isAllWhitespace("\r\n\t".toCharArray(), 0, 3));
        assertTrue(StringUtil.isAllWhitespace("x \t".toCharArray(), 1, 2));

        assertFalse(StringUtil.isAllWhitespace("x"));
        assertFalse(StringUtil.isAllWhitespace("                      !"));
    }
}
