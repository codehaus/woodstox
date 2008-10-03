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
        7, 39, 116, 900, 5003
    };
    final static int[] LEN_ATTR = new int[] {
        5, 17, 59, 357, 1920
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

    public void testBinaryElemSegmented() throws XMLStreamException
    {
        _testBinaryElem(3, false);
        _testBinaryElem(3, true);
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
                                fail("Corrupt decode at #"+ptr+", expected "+data[ptr]+", got "+buffer[2]);
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
                            fail("Corrupt decode at #"+i+", expected "+data[i]+", got "+buffer[3+i]);
                        }
                    }
                }
                break;
            default: // misc sizes
                {
                    byte[] buffer = new byte[200];
                    int ptr = 0;

                    while (true) {
                        int len = 20 + (r.nextInt() & 127);
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
                                fail("Corrupt decode at #"+i+", expected "+data[ptr+i]+", got "+buffer[i]);
                            }
                        }
                        ptr += count;
                    }

                    if (ptr != size) {
                        fail("Expected "+size+" bytes, got "+ptr);
                    }
                }
            }
        }
    }

    private byte[] generateData(Random r, int size)
    {
        byte[] result = new byte[size];
        r.nextBytes(result);
        return result;
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
}
