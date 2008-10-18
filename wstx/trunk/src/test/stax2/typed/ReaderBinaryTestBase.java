package stax2.typed;

import java.util.Random;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.ri.typed.AsciiValueEncoder;
import org.codehaus.stax2.ri.typed.ValueEncoderFactory;
import org.codehaus.stax2.typed.*;

import stax2.BaseStax2Test;

/**
 * Base class that contains set of simple unit tests to verify implementation
 * of parts {@link TypedXMLStreamReader} that deal with base64 encoded
 * binary data.
 * Concrete sub-classes are used to test both native and wrapped Stax2
 * implementations.
 *
 * @author Tatu Saloranta
 */
public abstract class ReaderBinaryTestBase
    extends BaseStax2Test
{
    // Let's test variable length arrays
    final static int[] LEN_ELEM = new int[] {
        1, 2, 3, 4, 7, 39, 116, 400, 900, 5003, 17045, 125000, 499999
    };
    final static int[] LEN_ATTR = new int[] {
        1, 2, 3, 5, 17, 59, 357, 1920
    };

    final static int[] LEN_ELEM_MULTIPLE = new int[] {
        4, 7, 16
    };

    final static int METHOD_SINGLE = 1;
    final static int METHOD_FULL = 2;

    /*
    ////////////////////////////////////////
    // Abstract methods
    ////////////////////////////////////////
     */

    protected abstract XMLStreamReader2 getReader(String contents)
        throws XMLStreamException;

    protected XMLStreamReader2 getElemReader(String contents)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getReader(contents);
        assertTokenType(START_ELEMENT, sr.next());
        return sr;
    }

    /*
    ////////////////////////////////////////
    // Test methods, elem, valid
    ////////////////////////////////////////
     */

    public void testBinaryElemByteByByte() throws XMLStreamException
    {
        _testBinaryElem(1, false);
        _testBinaryElem(1, true);
    }

    public void testBinaryElemFull() throws XMLStreamException
    {
        _testBinaryElem(2, false);
        _testBinaryElem(2, true);
    }

    public void testBinaryElem2Bytes() throws XMLStreamException
    {
        _testBinaryElem(3, false);
        _testBinaryElem(3, true);
    }

    public void testBinaryElemSegmented() throws XMLStreamException
    {
        _testBinaryElem(4, false);
        _testBinaryElem(4, true);
    }

    /**
     * Unit test that verifies that decoding state is properly
     * reset even if not all data is read.
     * Access is done using supported method (i.e. starting with
     * 
     */
    public void testMultipleBinaryElems() throws XMLStreamException
    {
        /* Let's try couple of sizes here too, but only check partial
         * content; this to ensure content is properly cleared between
         * calls
         */
        final int REPS = 3;
        for (int x = 0; x < LEN_ELEM_MULTIPLE.length; ++x) {
            int size = LEN_ELEM_MULTIPLE[x];
            Random r = new Random(size+1);
            byte[][] dataTable = generateDataTable(r, size, REPS);
            String doc = buildMultiElemDoc(dataTable);
            // First, get access to root elem
            XMLStreamReader2 sr = getElemReader(doc);

            // single-byte check should uncover problems
            for (int i = 0; i < REPS; ++i) {
                assertTokenType(START_ELEMENT, sr.next());
                _verifyElemData1(sr, dataTable[i]);
                // Should not have hit END_ELEMENT yet
                if (sr.getEventType() == END_ELEMENT) {
                    fail("Should not have yet advanced to END_ELEMENT, when decoding not finished");
                }
                // but needs to if we advance
                assertTokenType(END_ELEMENT, sr.next());
            }
            sr.close();
        }
    }

    /**
     * Test that uses 'mixed' segments (CHARACTERS and CDATA), in
     * which base64 units (4 chars producing 3 bytes) can be split
     * between segments.
     */
    public void testBinaryMixedSegments() throws XMLStreamException
    {
        // We'll do just one long test
        Random r = new Random(123);
        final int SIZE = 128000;
        byte[] data = generateData(r, SIZE);
        char[] buffer = new char[100];

        StringBuffer b64 = new StringBuffer(data.length * 2);

        /* Ok, first, let's first just generate long String of base64
         * data:
         */
        int ptr = 0;
        do {
            int chunkLen = 1 + (r.nextInt() & 0x7);
            AsciiValueEncoder enc = new ValueEncoderFactory().getEncoder(data, ptr, chunkLen);
            ptr += chunkLen;
            int len = enc.encodeMore(buffer, 0, buffer.length);
            b64.append(buffer, 0, len);
        } while (b64.length() < SIZE);
        // And then create document, with split content

        final int byteLen = ptr;
        String refDoc = "<root>"+b64.toString()+"</root>";

        // But first: let's verify content is encoded correctly:
        {
            XMLStreamReader2 sr = getElemReader(refDoc);
            _verifyElemData(sr, r, data, byteLen, METHOD_FULL);
            sr.close();
        }

        StringBuffer sb = new StringBuffer(b64.length() * 2);
        sb.append("<root>");

        ptr = 0;
        boolean cdata = false;

        while (ptr < b64.length()) {
            int segLen = 1 + (r.nextInt() & 0x7);
            if (cdata) {
                sb.append("<![CDATA[");
            }
            segLen = Math.min(segLen, (b64.length() - ptr));
            for (int i = 0; i < segLen; ++i) {
                sb.append(b64.charAt(ptr++));
            }
            if (cdata) {
                sb.append("]]>");
            }
            cdata = !cdata;
        }
        sb.append("</root>");
        String actualDoc = sb.toString();

        XMLStreamReader2 sr = getElemReader(actualDoc);
        // should be enough to verify byte-by-byte?
        _verifyElemData(sr, r, data, byteLen, METHOD_SINGLE);
        sr.close();
    }

    private void _testBinaryElem(int readMethod, boolean addNoise)
        throws XMLStreamException
    {
        for (int x = 0; x < LEN_ELEM.length; ++x) {
            int size = LEN_ELEM[x];
            Random r = new Random(size);
            byte[] data = generateData(r, size);
            String doc = buildDoc(r, data, addNoise);

            XMLStreamReader2 sr = getElemReader(doc);
            _verifyElemData(sr, r, data, data.length, readMethod);
            sr.close();
        }
    }

    private void _verifyElemData(XMLStreamReader2 sr, Random r, byte[] data, int dataLen, int readMethod)
        throws XMLStreamException
    {
        switch (readMethod) {
        case 1: // minimal reads, single byte at a time
            {
                byte[] buffer = new byte[5];
                int ptr = 0;
                int count;
                
                while ((count = sr.readElementAsBinary(buffer, 2, 1)) > 0) {
                    assertEquals(1, count);
                    if ((ptr+1) < dataLen) {
                        if (data[ptr] != buffer[2]) {
                            fail("Corrupt decode at #"+ptr+"/"+dataLen+", expected "+displayByte(data[ptr])+", got "+displayByte(buffer[2]));
                        }
                    }
                    ++ptr;
                }
                if (ptr != dataLen) {
                    fail("Expected to get "+dataLen+" bytes, got "+ptr);
                }
            }
            break;
        case 2: // full read
            {
                byte[] buffer = new byte[dataLen + 100];
                /* Let's assume reader will actually read it all:
                 * while not absolutely required, in practice it should
                 * happen. If this is not true, need to change unit
                 * test to reflect it.
                 */
                int count = sr.readElementAsBinary(buffer, 3, buffer.length-3);
                assertEquals(dataLen, count);
                for (int i = 0; i < dataLen; ++i) {
                    if (buffer[3+i] != data[i]) {
                        fail("Corrupt decode at #"+i+", expected "+displayByte(data[i])+", got "+displayByte(buffer[3+i]));
                    }
                }
            }
            break;
            
        case 3: // 2 bytes at a time
        default: // misc sizes
            {
                boolean random = (readMethod > 3);
                
                byte[] buffer = new byte[200];
                int ptr = 0;
                
                while (true) {
                    int len = random ? (20 + (r.nextInt() & 127)) : 2;
                    int count = sr.readElementAsBinary(buffer, 0, len);
                    if (count < 0) {
                        break;
                    }
                    if ((ptr + count) > dataLen) {
                        ptr += count;
                        break;
                    }
                    for (int i = 0; i < count; ++i) {
                        if (data[ptr+i] != buffer[i]) {
                            fail("Corrupt decode at #"+(ptr+i)+"/"+dataLen+" (read len: "+len+"; got "+count+"), expected "+displayByte(data[ptr+i])+", got "+displayByte(buffer[i]));
                        }
                    }
                    ptr += count;
                }
                
                if (ptr != dataLen) {
                    fail("Expected "+dataLen+" bytes, got "+ptr);
                }
            }
        }
        assertTokenType(END_ELEMENT, sr.getEventType());
    }

    private void _verifyElemData1(XMLStreamReader2 sr, byte[] data)
        throws XMLStreamException
    {
        byte[] buffer = new byte[5];
        assertEquals(1, sr.readElementAsBinary(buffer, 1, 1));
        assertEquals(data[0], buffer[1]);
    }
        
    /*
    ////////////////////////////////////////
    // Test methods, elem, invalid
    ////////////////////////////////////////
     */

    /**
     * Rules for padding are quite simple: you can use one or two padding
     * characters, which indicate 1 or 2 bytes instead full 3 for the
     * decode unit.
     */
    public void testInvalidPadding()
        throws XMLStreamException
    {
        // Let's try out couple of arbitrary broken ones...
        final String[] INVALID = new String[] {
            "AAAA====", "AAAAB===", "AA=A"
        };
        final byte[] resultBuffer = new byte[20];

        for (int i = 0; i < INVALID.length; ++i) {
            String doc = "<root>"+INVALID[i]+"</root>";
            XMLStreamReader2 sr = getElemReader(doc);
            try {
                /*int count = */ sr.readElementAsBinary(resultBuffer, 0, resultBuffer.length);
                fail("Should have received an exception for invalid padding");
            } catch (TypedXMLStreamException ex) {
                // any way to check that it's the excepted message? not right now
            }
            sr.close();
        }
    }

    /**
     * Whitespace is allowed within base64, but only to separate 4 characters
     * base64 units. Ideally (and by the spec) they should be used every
     * 76 characters (== every 19 units), but it'd be hard to enforce this
     * as well as fail on much of existing supposedly base64 compliant
     * systems. So, we will just verify that white space can not be used
     * within 4 char units.
     */
    public void testInvalidWhitespace()
        throws XMLStreamException
    {
        // Let's try out couple of arbitrary broken ones...
        final String[] INVALID = new String[] {
            "AAA A", "AAAA BBBB C CCC", "ABCD ABCD AB CD"
        };
        final byte[] resultBuffer = new byte[20];

        for (int i = 0; i < INVALID.length; ++i) {
            String doc = "<root>"+INVALID[i]+"</root>";
            XMLStreamReader2 sr = getElemReader(doc);
            try {
                /*int count = */ sr.readElementAsBinary(resultBuffer, 0, resultBuffer.length);
                fail("Should have received an exception for white space used 'inside' 4-char base64 unit");
            } catch (TypedXMLStreamException ex) {
                // any way to check that it's the excepted message? not right now
            }
            sr.close();
        }
    }

    public void testInvalidWeirdChars()
        throws XMLStreamException
    {
        // Let's try out couple of arbitrary broken ones...
        final String[] INVALID = new String[] {
            "AAA?", "AAAA@@@@", "ABCD\u00A0BCD"
        };
        final byte[] resultBuffer = new byte[20];

        for (int i = 0; i < INVALID.length; ++i) {
            String doc = "<root>"+INVALID[i]+"</root>";
            XMLStreamReader2 sr = getElemReader(doc);
            try {
                /*int count = */ sr.readElementAsBinary(resultBuffer, 0, resultBuffer.length);
                fail("Should have received an exception for invalid base64 character");
            } catch (TypedXMLStreamException ex) {
                // any way to check that it's the excepted message? not right now
            }
            sr.close();
        }
    }

    public void testIncompleteInvalid()
        throws XMLStreamException
    {
        // Let's just try with short partial segments, data used doesn't matter
        final byte[] data = new byte[6];
        final byte[] resultBuffer = new byte[20];

        // So first we'll encode 1 to 6 bytes as base64
        for (int i = 1; i <= data.length; ++i) {
            AsciiValueEncoder enc = new ValueEncoderFactory().getEncoder(data, 0, i);
            char[] cbuf = new char[20];
            int clen = enc.encodeMore(cbuf, 0, cbuf.length);

            // and use all byte last 1, 2 or 3 chars
            for (int j = 1; j <= 3; ++j) {
                int testLen = clen-j;
                StringBuffer sb = new StringBuffer();
                sb.append("<root>");
                sb.append(cbuf, 0, testLen);
                sb.append("</root>");

                XMLStreamReader2 sr = getElemReader(sb.toString());
                try {
                    /*int count = */ sr.readElementAsBinary(resultBuffer, 0, resultBuffer.length);
                    fail("Should have received an exception for incomplete base64 unit");
                } catch (TypedXMLStreamException ex) {
                    // any way to check that it's the excepted message? not right now
                }
                sr.close();
            }
        }
    }

    /*
    ////////////////////////////////////////
    // Test methods, attr, valid
    ////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////
     */

    private byte[] generateData(Random r, int size)
    {
        byte[] result = new byte[size];
        r.nextBytes(result);
        return result;
    }

    private byte[][] generateDataTable(Random r, int size, int reps)
    {
        byte[][] table = new byte[reps][];
        for (int i = 0; i < reps; ++i) {
            table[i] = generateData(r, size);
        }
        return table;
    }

    private String buildDoc(Random r, byte[] data, boolean addNoise)
    {
        // Let's use base64 codec from RI here:
        AsciiValueEncoder enc = new ValueEncoderFactory().getEncoder(data, 0, data.length);

        StringBuffer sb = new StringBuffer(data.length * 2);
        sb.append("<root>");

        // Without noise it's quite easy, just need enough space:
        if (!addNoise) {
            // Base64 adds 33% overhead, but let's be generous
            char[] buffer = new char[4 + (data.length * 3 / 2)];
            int len = enc.encodeMore(buffer, 0, buffer.length);
            sb.append(buffer, 0, len);
        } else {
            // but with noise, need bit different approach
            char[] buffer = new char[300];

            while (!enc.isCompleted()) {
                int offset = r.nextInt() & 0xF;
                int len;
                int rn = r.nextInt() & 15;

                switch (rn) {
                case 1:
                case 2:
                case 3:
                case 4:
                    len = rn;
                    break;
                case 5:
                case 6:
                case 7:
                    len = 3 + (r.nextInt() & 15);
                    break;
                default:
                    len = 20 + (r.nextInt() & 127);
                    break;
                }
                int end = enc.encodeMore(buffer, offset, offset+len);

                // regular or CDATA?
                boolean cdata = r.nextBoolean() && r.nextBoolean();

                if (cdata) {
                    sb.append("<![CDATA[");
                } 
                sb.append(buffer, offset, end-offset);
                if (cdata) {
                    sb.append("]]>");
                } 

                // Let's add noise 25% of time
                if (r.nextBoolean() && r.nextBoolean()) {
                    sb.append("<!-- comment: "+len+" -->");
                } else {
                    sb.append("<?pi "+len+"?>");
                }
            }
        }
        sb.append("</root>");
        return sb.toString();
    }

    private String buildMultiElemDoc(byte[][] dataTable)
    {
        StringBuffer sb = new StringBuffer(16 + dataTable.length * dataTable[0].length);
        sb.append("<root>");
        for (int i = 0; i < dataTable.length; ++i) {
            byte[] data = dataTable[i];
            char[] buffer = new char[4 + (data.length * 3 / 2)];
            AsciiValueEncoder enc = new ValueEncoderFactory().getEncoder(data, 0, data.length);
            int len = enc.encodeMore(buffer, 0, buffer.length);
            sb.append("<a>");
            sb.append(buffer, 0, len);
            sb.append("</a>");
        }
        sb.append("</root>");
        return sb.toString();
    }

    final static String displayByte(byte b) {
        return "0x"+Integer.toHexString((int) b & 0xFF);
    }
}
