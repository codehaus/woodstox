package wstxtest.util;

import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.util.WordResolver;

/**
 * Simple unit tests for testing {@link WordResolver}.
 */
public class TestWordResolver
    extends TestCase
{
    public TestWordResolver(String name) {
        super(name);
    }

    public void testNormal()
    {
        TreeSet set = new TreeSet();

        set.add("word");
        set.add("123");
        set.add("len");
        set.add("length");
        set.add("leno");
        set.add("1");
        set.add("foobar");

        WordResolver wr = WordResolver.constructInstance(set);

        assertEquals(wr.size(), set.size());

        Iterator it = set.iterator();

        // Let's first check if words that should be there, are:
        while (it.hasNext()) {
            String str = (String) it.next();

            assertEquals(str, wr.find(str));
            // And then, let's make sure intern()ing isn't needed:
            assertEquals(str, wr.find(""+str));

            char[] strArr = str.toCharArray();
            char[] strArr2 = new char[strArr.length + 4];
            System.arraycopy(strArr, 0, strArr2, 3, strArr.length);
            assertEquals(str, wr.find(strArr, 0, str.length()));
            assertEquals(str, wr.find(strArr2, 3, str.length() + 3));
        }

        // And then that ones shouldn't be there aren't:
        checkNotFind(wr, "foo");

    }

    /**
     * This unit test was created as a regression test, to check for
     * a bug that was found during development.
     */
    public void testSingle()
    {
        TreeSet set = new TreeSet();
        set.add("CDATA");
        WordResolver res = WordResolver.constructInstance(set);
        assertEquals("CDATA", res.find(new String("CDATA")));
        assertNull(res.find("CDAT"));
        assertNull(res.find("CDATA "));

        // this caused an NPE:
        assertNull(res.find("value"));
    }

    /*
    ///////////////////////////////////////////////////////
    // Private methods:
    ///////////////////////////////////////////////////////
     */

    private void checkNotFind(WordResolver wr, String str)
    {
        char[] strArr = str.toCharArray();
        char[] strArr2 = new char[strArr.length + 4];
        System.arraycopy(strArr, 0, strArr2, 1, strArr.length);

        assertNull(wr.find(str));
        assertNull(wr.find(strArr, 0, strArr.length));
        assertNull(wr.find(strArr2, 1, strArr.length + 1));
    }
}

