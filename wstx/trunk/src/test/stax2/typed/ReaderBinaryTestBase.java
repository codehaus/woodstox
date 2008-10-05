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
        1, 2, 3, 4, 7, 39, 116, 400, 900, 5003, 17045
    };
    final static int[] LEN_ATTR = new int[] {
        1, 2, 3, 5, 17, 59, 357, 1920
    };

    final static int[] LEN_ELEM_MULTIPLE = new int[] {
        4, 7, 16
    };


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

    private void _testBinaryElem(int readMethod, boolean addNoise)
        throws XMLStreamException
    {
        for (int x = 0; x < LEN_ELEM.length; ++x) {
            int size = LEN_ELEM[x];
            Random r = new Random(size);
            byte[] data = generateData(r, size);
            String doc = buildDoc(r, data, addNoise);

            XMLStreamReader2 sr = getElemReader(doc);
            _verifyElemData(sr, r, data, readMethod);
            sr.close();
        }
    }

    private void _verifyElemData(XMLStreamReader2 sr, Random r, byte[] data, int readMethod)
        throws XMLStreamException
    {
        final int size = data.length;

        switch (readMethod) {
        case 1: // minimal reads, single byte at a time
            {
                byte[] buffer = new byte[5];
                int ptr = 0;
                int count;
                
                while ((count = sr.readElementAsBinary(buffer, 2, 1)) > 0) {
                    assertEquals(1, count);
                    if ((ptr+1) < size) {
                        if (data[ptr] != buffer[2]) {
                            fail("Corrupt decode at #"+ptr+"/"+size+", expected "+displayByte(data[ptr])+", got "+displayByte(buffer[2]));
                        }
                    }
                    ++ptr;
                }
                if (ptr != size) {
                    fail("Expected to get "+size+" bytes, got "+ptr);
                }
            }
            break;
        case 2: // full read
            {
                byte[] buffer = new byte[size + 100];
                /* Let's assume reader will actually read it all:
                 * while not absolutely required, in practice it should
                 * happen. If this is not true, need to change unit
                 * test to reflect it.
                 */
                int count = sr.readElementAsBinary(buffer, 3, buffer.length-3);
                assertEquals(size, count);
                for (int i = 0; i < size; ++i) {
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
                    if ((ptr + count) > size) {
                        ptr += count;
                        break;
                    }
                    for (int i = 0; i < count; ++i) {
                        if (data[ptr+i] != buffer[i]) {
                            fail("Corrupt decode at #"+(ptr+i)+"/"+size+" (read len: "+len+"; got "+count+"), expected "+displayByte(data[ptr+i])+", got "+displayByte(buffer[i]));
                        }
                    }
                    ptr += count;
                }
                
                if (ptr != size) {
                    fail("Expected "+size+" bytes, got "+ptr);
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
        // Hmmh. Let's actually use base64 codec from RI here:
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
            char[] buffer = new char[200];

            while (!enc.isCompleted()) {
                int offset = r.nextInt() & 0xF;
                int len = 20 + (r.nextInt() & 127);
                int end = enc.encodeMore(buffer, offset, offset+len);
                sb.append(buffer, offset, end-offset);

                // Let's add noise 25% of time
                if (r.nextBoolean() && r.nextBoolean()) {
                    if (r.nextBoolean()) {
                        sb.append("<!-- comment: "+len+" -->");
                    } else {
                        sb.append("<?pi "+len+"?>");
                    }
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
