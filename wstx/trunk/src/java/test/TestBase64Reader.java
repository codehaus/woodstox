package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.typed.TypedXMLStreamReader;

/**
 * Simple helper test class for checking how stream reader handles xml
 * documents.
 */
public class TestBase64Reader
    implements XMLStreamConstants
{
    public void test() throws XMLStreamException
    {
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
        XMLInputFactory f = XMLInputFactory.newInstance();
        String xml = "<root>"
+"TWFuIGlzIGRpc3Rpbmd1"
+"aXNoZWQsIG5vdCBvbmx5"
+"IGJ5IGhpcyByZWFzb24s"
+"IGJ1dCBieSB0aGlz\n"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg\n"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu\n"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo\n"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            +"</root>";
        final int CHUNK_LEN = 7;
        byte[] buffer = new byte[CHUNK_LEN];
        TypedXMLStreamReader sr = (TypedXMLStreamReader) f.createXMLStreamReader(new StringReader(xml));
        sr.next();

        int offset = 0;

        while (true) {
            int count = sr.readElementAsBinary(buffer, 0, buffer.length);
            System.out.print("Result("+offset+"+"+count+"): ");
            if (count < 0) {
                break;
            }
            System.out.print('"');
            for (int i = 0; i < count; ++i) {
                System.out.print((char) buffer[i]);
            }
            System.out.print('"');
            offset += count;
            System.out.println();
        }
        System.out.println("DONE!");
        sr.close();
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestBase64Reader().test();
    }
}
