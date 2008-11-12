package staxperf.speed;

import java.io.*;

import com.ctc.wstx.io.*;

import com.bea.xml.stream.reader.XmlReader;

/**
 * Simple test class that compares speed of various charset decoders;
 * both JDK-provided and custom-coded. Intention is to figure out
 * which ones are the fastest.
 */
public class TestStreamSpeed
{
    //final static int WSTX_LEN = 8000;
    final static int WSTX_LEN = 4000;

    final static int READ_LEN = 4000;

    final static int REP_COUNT = 5;
    //final static int REP_COUNT = 1;

    private TestStreamSpeed() { }

    private void test(File f)
        throws IOException
   {
       int round = 0;

       for (; true; ++round) {
           long now = System.currentTimeMillis();
           char[] buf = new char[READ_LEN];
           String msg = "[null]";
           int total = 0;

           if ((round % 6) == 0) {
               System.out.println();
           }
           int byteCount = 0;

           for (int i = 0; i < REP_COUNT; ++i) {
               Reader r;
               InputStream in = new FileInputStream(f);

               switch (round % 6) {
               //switch (3 + (round % 3)) {
               case 0:
                   r = new InputStreamReader(in, "ISO-8859-1");
                   msg = "[JDK, ISO-Latin1]";
                   break;
               case 1:
                   r = new ISOLatinReader(null, in, new byte[WSTX_LEN], 0, 0, false);
                   msg = "[Custom, ISO-Latin1]";
                   break;
               case 2:
                   r = XmlReader.createReader(in, "ISO-8859-1");
                   msg = "[StaxRI, ISO-Latin1]";
                   break;
               case 3:
                   r = new InputStreamReader(in, "UTF-8");
                   msg = "[JDK, UTF-8]";
                   break;
               case 4:
                   r = new UTF8Reader(null, in, new byte[WSTX_LEN], 0, 0, false);
                   msg = "[Custom, UTF-8]";
                   break;
               default:
                   r = XmlReader.createReader(in, "UTF-8");
                   msg = "[StaxRI, UTF8]";
                   break;
               }
               int count;
               byteCount = 0;

               while ((count = r.read(buf)) >= 0) {
                   total += count;
                   /*
                   for (int j = 0; j < count; ++j) {
                       byteCount += buf[j];
                   }
                   */
               }
           }

           now = System.currentTimeMillis() - now;
           System.out.println(msg+" -> "+now+" msecs (total "+total
                              +", byte count 0x"+Integer.toHexString(byteCount)+")");

           try { Thread.sleep(200L); } catch (Exception e) { }
           System.gc();
           try { Thread.sleep(200L); } catch (Exception e) { }
       }
   }

    public static void main(String[] args)
        throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... [input file]");
            System.exit(1);
        }
        new TestStreamSpeed().test(new File(args[0]));
    }
}

