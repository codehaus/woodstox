package test;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.*;

import com.ctc.wstx.api.WstxInputProperties;

/**
 * Simple non-automated unit test for outputting namespace-aware XML
 * documents.
 */
public class TestStreamReader
    implements XMLStreamConstants
{
    protected TestStreamReader() {
    }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");

        XMLInputFactory f =  XMLInputFactory.newInstance();
        System.out.println("Factory instance: "+f.getClass());

        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                      //Boolean.FALSE
                      Boolean.TRUE
                      );

        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);

        f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        f.setProperty(XMLInputFactory.RESOLVER, new TestResolver1());

        if (f.isPropertySupported(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE)) {
            f.setProperty(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE,
                          Boolean.FALSE
                          //Boolean.TRUE
            );
        }

        if (f.isPropertySupported(WstxInputProperties.P_MIN_TEXT_SEGMENT)) {
            f.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT,
                          new Integer(9));
        }

        /*
        if (f.isPropertySupported(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES)) {
            java.util.Map m = new java.util.HashMap();
            m.put("myent", "foobar");
            m.put("myent2", "<tag>R&amp;B + &myent;</tag>");
            f.setProperty(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES, m);
        }
        */

        if (f.isPropertySupported(WstxInputProperties.P_DTD_RESOLVER)) {
            f.setProperty(WstxInputProperties.P_DTD_RESOLVER,
                          new TestResolver2());
        }
        if (f.isPropertySupported(WstxInputProperties.P_ENTITY_RESOLVER)) {
            f.setProperty(WstxInputProperties.P_ENTITY_RESOLVER,
                          new TestResolver2());
        }

        /* Uncomment for boundary-condition stress tests; should be ok to 
         * use some fairly small (but not tiny) number...
         */

        /*
        if (f.isPropertySupported(WstxInputProperties.P_INPUT_BUFFER_LENGTH)) {
            f.setProperty(WstxInputProperties.P_INPUT_BUFFER_LENGTH,
                          new Integer(29));
        }
        if (f.isPropertySupported(WstxInputProperties.P_TEXT_BUFFER_LENGTH)) {
            f.setProperty(WstxInputProperties.P_TEXT_BUFFER_LENGTH,
                          new Integer(20));
        }
        */

        // To test windows linefeeds:
        /*
        if (f.isPropertySupported(WstxInputProperties.P_NORMALIZE_LFS)) {
            f.setProperty(WstxInputProperties.P_NORMALIZE_LFS, Boolean.TRUE);
        } else {
            System.out.println("No property "+WstxInputProperties.P_NORMALIZE_LFS+", skipping.");
        }
        */

        return f;
    }

    protected int test(File file)
        throws Exception
    {
        XMLInputFactory f = getFactory();


        System.out.print("Coalesce: "+f.getProperty(XMLInputFactory.IS_COALESCING));
        System.out.println(", NS-aware: "+f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        System.out.print("Entity-expanding: "+f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        System.out.println(", validating: "+f.getProperty(XMLInputFactory.IS_VALIDATING));

        /*
        if (f.isPropertySupported(WstxInputProperties.P_BASE_URL)) {
            f.setProperty(WstxInputProperties.P_BASE_URL, file.toURL());
        }
        */

        int total = 0;
        InputStream in;
        XMLStreamReader sr;

        in = new FileInputStream(file);

        // Let's deal with gzipped files too?
        if (file.getName().endsWith(".gz")) {
            System.out.println("[gzipped input file!]");
            in = new GZIPInputStream(in);
        }

        // !!! TEST:
        //in = new TestStream(in);

        /*
        {
            byte[] data = readData(file);
            in = new ByteArrayInputStream(data);
            String str = new String(data, "UTF-8");
            sr = f.createXMLStreamReader(new StringReader(str));
        }
        */

        sr = f.createXMLStreamReader(file.toURL().toString(), in);
        //sr = f.createXMLStreamReader(in);

        /* 
        sr = f.createXMLStreamReader(new StringReader(
"<!DOCTYPE root [ <!ELEMENT root ANY> <!ENTITY pi '<?target data'> ]>\n"
+"<root>&pi;   and rest of data?></root>"));
        */

        //Reader r = new FileReader(file);
        //sr = f.createXMLStreamReader(file.toURL().toString(), r);

        while (sr.hasNext()) {
            int type = sr.next();
            total += type; // so it won't be optimized out...

            boolean hasName = sr.hasName();

            System.out.print("["+type+"]");

            if (sr.hasText()) {
                String text = sr.getText();
                int textLen = sr.getTextLength();
                //total += textLen;
                // Sanity check (note: RI tends to return nulls?)
                if (text != null) {
                    char[] textBuf = sr.getTextCharacters();
                    int start = sr.getTextStart();
                    String text2 = new String(textBuf, start, textLen);
                    if (!text.equals(text2)) {
                        throw new Error("Text access via 'getText()' different from accessing via buffer: text='"+text+"', array='"+text2+"'");
                    }
                }

                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length(); // to prevent dead code elimination
                }
                if (type == CHARACTERS || type == CDATA) {
                    System.out.println(" Text = '"+text+"'.");
                } else if (type == SPACE) {
                    System.out.print(" Ws = '"+text+"'.");
                    char c = (text.length() == 0) ? ' ': text.charAt(text.length()-1);
                    if (c != '\r' && c != '\n') {
                        System.out.println();
                    }
                } else if (type == DTD) {
                    List entities = (List) sr.getProperty("javax.xml.stream.entities");
                    List notations = (List) sr.getProperty("javax.xml.stream.notations");
                    int entCount = (entities == null) ? -1 : entities.size();
                    int notCount = (notations == null) ? -1 : notations.size();
                    System.out.print(" DTD ("+entCount+" entities, "+notCount
                                       +" notations), declaration = <<");
                    System.out.print(text);
                    System.out.println(">>");
                } else if (type == ENTITY_REFERENCE) {
                    // entity ref
                    System.out.println(" Entity ref: &"+sr.getLocalName()+" -> '"+sr.getText()+"'.");
                    hasName = false; // to suppress further output
                } else { // comment, PI?
                    ;
                }
            }

            if (type == PROCESSING_INSTRUCTION) {
                System.out.println(" PI target = '"+sr.getPITarget()+"'.");
                System.out.println(" PI data = '"+sr.getPIData()+"'.");
            } else if (type == START_ELEMENT) {
                int count = sr.getAttributeCount();
                System.out.println(" ["+count+" attrs]");
                // debugging:
                for (int i = 0; i < count; ++i) {
                    System.out.print(" attr#"+i+": "+sr.getAttributePrefix(i)
                                     +":"+sr.getAttributeLocalName(i)
                                     +" ("+sr.getAttributeNamespace(i)
                                     +") -> '"+sr.getAttributeValue(i)
                                     +"'");
                    System.out.println(sr.isAttributeSpecified(i) ?
                                       "[specified]" : "[Default]");
                }
            }
            if (hasName) {
                System.out.print(" Name: '"+sr.getName()+"' (prefix <"
                                   +sr.getPrefix()+">)");
            }

            System.out.println();
        }
        return total;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestStreamReader.class+" [file]");
            System.exit(1);
        }

        try {
            int total = new TestStreamReader().test(new File(args[0]));
            System.out.println("Total: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }

    public static byte[] readData(String filename)
        throws IOException
    {
        return readData(new File(filename));
    }   

    public static byte[] readData(File f)
        throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        try {
            byte[] buf = new byte[16000];
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length() + 16);
            int count;

            while ((count = fin.read(buf)) > 0) {
                bos.write(buf, 0, count);
            }
            return bos.toByteArray();
        } finally {
            fin.close();
        }
    }
}

/**
 * Test stream to check whether reader can do proper streaming...
 * Need to throw an exception to ensure _succesful_ functioning?
 */
final class TestStream
    extends FilterInputStream
{
    private final int sleep = 5000;
    
    public TestStream(InputStream is)
    {
        super(is);
    }
    
    public int read() throws IOException
    {
        int r = super.read();
        if (r<0) {
            try {
System.err.println("WAIT/single");
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
System.err.println("Returning "+r);
        return r;
    }
    
    public int read(byte[] b, int off, int len) throws IOException
    {
        int r = super.read(b, off, len);
        if (r<0) {
// Let's trigger test exception...            
//if (true) throw new Error("W00t?");
            try {
                System.err.println("WAIT/multiple");
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
System.err.println("Returning "+r);
        return r;
    }
}

