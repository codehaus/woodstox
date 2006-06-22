package staxperf.speed;

import java.io.*;

import com.ctc.wstx.io.*;

/**
 * Simple test class that compares speed of various charset decoders;
 * both JDK-provided and custom-coded. Intention is to figure out
 * which ones are the fastest.
 */
public class TestWriterSpeed
{
    final static int WRITE_LEN = 2000;
    //final static int WRITE_LEN = 50;

    private TestWriterSpeed() { }

    private void test(File f)
        throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        int count;
        long size = f.length();
        byte[] data = new byte[(int) size];

        // Let's calc REP_COUNT
        int REP_COUNT = 6;

        long limit = 1000000L;
        while (size < limit) {
            REP_COUNT += (REP_COUNT / 2);
            limit >>= 1;
        }

        // not optimal, but let's be cheap here (should always succeed):
        if ((count = fin.read(data)) < f.length()) {
            throw new Error("Failed to read all "+f.length()+" bytes, only got "+count);
        }
        fin.close();
        // And then decoded (assume UTF-8)
        StringBuffer sb = new StringBuffer();
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        InputStreamReader sin = new InputStreamReader(bis, "UTF-8");
        char[] buffer = new char[4000];
        while ((count = sin.read(buffer)) > 0) {
            sb.append(buffer, 0, count);
        }
        sin.close();
        char[] chars = sb.toString().toCharArray();
        System.out.println("Ok, read "+data.length+" bytes, "+chars.length+" chars... testing Writer speed.");
        System.out.println("Repeating read "+REP_COUNT+" times.");
        test2(data, chars, REP_COUNT);
    }

    private void test2(byte[] buf, char[] cbuf, int REP_COUNT)
        throws IOException
    {
       int round = 0;
       ByteArrayOutputStream bout = new ByteArrayOutputStream(buf.length);

       for (; true; ++round) {
           long now = System.currentTimeMillis();
           String msg = "[null]";
           int total = 0;

           if ((round % 6) == 0) {
               System.out.println();
           }

           for (int i = 0; i < REP_COUNT; ++i) {
               Writer w = null;
               bout.reset(); // can reuse its buffers this way

               switch (round % 2) {
               case 1:
                   //w = new OutputStreamWriter(bout, "UTF-8");
                   w = new BufferedWriter(new OutputStreamWriter(bout, "UTF-8"), 1000);
                   msg = "[JDK, UTF-8]";
                   break;
               case 0:
                   w = new Utf8Writer(bout);
                   msg = "[Custom, UTF-8]";
                   break;
                   /*
               case 2:
                   w = new JavolutionUtf8Writer(bout);
                   msg = "[Javolution, UTF-8]";
                   break;
               case 3:
                   r = new InputStreamReader(in, "UTF-8");
                   msg = "[JDK, UTF-8]";
                   break;
               case 4:
                   r = new UTF8Reader(in, new byte[WSTX_LEN], 0, 0);
                   msg = "[Custom, UTF-8]";
                   break;
               default:
                   r = XmlReader.createReader(in, "UTF-8");
                   msg = "[StaxRI, UTF8]";
                   break;
                   */
               }

               int offset = 0;

               while (true) {
                   int len = cbuf.length - offset;
                   if (len < 1) {
                       break;
                   }
                   if (len > WRITE_LEN) {
                       len = WRITE_LEN;
                   }
                   w.write(cbuf, offset, len);
                   total += cbuf[offset]; // just to prevent dead code elimination
                   offset += len;
               }
               w.flush();
               w.close();
           }

           now = System.currentTimeMillis() - now;
           System.out.print(msg+" -> "+now+" msecs (total "+total+")");

           if (bout.size() != buf.length) {
               //throw new IOException("Unexpected output length ("+bout.size()+"), should have gotten "+buf.length);
               System.err.println("WARNING: Unexpected output length ("+bout.size()+"), should have gotten "+buf.length);
           } else {
               // also, let's verify accuracy, just to be sure
               byte[] result = bout.toByteArray();
               for (int i = 0, len = result.length; i < len; ++i) {
                   if (result[i] != buf[i]) {
                       throw new IOException("Failure: bytes at index #"+i+" different: got "+(result[i] & 0xFF)+", exp "+(buf[i] & 0xFF));
                   }
               }
           }
           System.out.println(" [verified data, "+buf.length+" b/"+cbuf.length+" c]");

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
        new TestWriterSpeed().test(new File(args[0]));
    }
}

