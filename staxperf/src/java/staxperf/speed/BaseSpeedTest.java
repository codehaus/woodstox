package staxperf.speed;

import java.io.File;
import java.io.Reader;

import javax.xml.stream.XMLStreamConstants;

/**
 * Base class for testing various StAX implementations.
 */
abstract class BaseSpeedTest
    implements XMLStreamConstants
{
    public void test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        String filename = args[0];

        //final int ROUNDS = 700;
        final int ROUNDS = 111;
        //final int ROUNDS = 1;
        for (int i = 0; i < ROUNDS; ++i) {
            long now = System.currentTimeMillis();
            File f = new File(filename);
            Reader fin = new java.io.BufferedReader(new java.io.FileReader(f));
            int total = 0;
            try {
                total = testExec(f, fin);
                now = System.currentTimeMillis() - now;
                testFinish();
            } finally {
                fin.close();
            }
 
            // Let's see how much garbage we got:
            long mem = Runtime.getRuntime().freeMemory();

            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();
            try {  Thread.sleep(200L); } catch (InterruptedException ie) { }

            mem = Runtime.getRuntime().freeMemory() - mem;

            System.out.println("Took "+now+" msecs (total: "+total+"); "+mem+" bytes garbage.");
            System.out.println("#"+i+":  -----------------  ");

            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            //if (true) { break; }
        }
    }

    public abstract int testExec(File f, Reader r) throws Exception;

    public abstract void testFinish() throws Exception;
}
