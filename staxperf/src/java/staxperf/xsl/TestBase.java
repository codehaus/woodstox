package staxperf.xsl;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import staxperf.TestUtil;

/**
 * Base class for testing sustainable performance of various Java XSL
 * processor implementations. Basic operation is as follows:
 *<ul>
 * <li>First implementation is set up, and then <b>warmed up</b>, by doing
 *   couple (30) of repetitions over test document. This ensures JIT has
 *   had a chance to use hot spot compilation of performance bottlenecks
 *  </li>
 * <li>Main testing loop iterates over parsing as many times as possible,
 *    during test period (30 seconds), and reports number of iterations
 *    succesfully done.
 *  </li>
 *</ul>
 */
abstract class TestBase
{
    private final static int DEFAULT_TEST_SECS = 30;
    private final static int WARMUP_ROUNDS = 30;

    protected TransformerFactory _factory;

    protected ByteArrayOutputStream _outStream = new ByteArrayOutputStream();

    protected int _batchSize;

    protected abstract TransformerFactory getFactory();

    protected abstract StreamResult getResult(ByteArrayOutputStream bos);

    protected void init()
    {
        _factory = getFactory();
    }

    protected int testExec(Transformer tf, byte[] input) throws Exception
    {
        InputStream in = new ByteArrayInputStream(input);
        _outStream.reset();
        StreamResult result = getResult(_outStream);
        tf.transform(new StreamSource(in), result);
        int ret = _outStream.size();
        in.close();
        _outStream.close();
        return ret;
    }

    public void test(String[] args)
        throws Exception
    {
        if (args.length < 2) {
            System.err.println("Usage: java ... "+getClass().getName()+" [style-sheet] [input-file] <test-period-secs>");
            System.exit(1);
        }

        init();

        String xslFn = args[0];
        String inputFn = args[1];
        int SECS = DEFAULT_TEST_SECS;
        if (args.length > 2) {
            SECS = Integer.parseInt(args[2]);
        }
        File xslF = new File(xslFn).getAbsoluteFile();
        File inputF = new File(inputFn).getAbsoluteFile();

        // First, read the stylesheet in:
        System.out.println("Reading stylesheet from: "+xslF);
        Transformer tf = _factory.newTransformer(new StreamSource(xslF));
        System.out.println(" -> transformer: "+tf.getClass().getName());

        // First, warm up:
        byte[] payload = TestUtil.readData(inputF);
        System.out.println("Read xml data from '"+inputF+"', length: "+payload.length);

        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations (real test will run for "+SECS+" seconds): ");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ) {
            // Let's estimate speed from the last warmup...
            if (++i == WARMUP_ROUNDS) {
                long now = System.currentTimeMillis();
                total = testExec(tf, payload);
                _batchSize = TestUtil.calcBatchSize(System.currentTimeMillis() - now);
            } else {
                total = testExec(tf, payload);
            }
            //testFinish();
            System.out.print(".");
            // Let's allow some slack time between iterations
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        }
        System.out.println(" (batch size "+_batchSize+")");

        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        System.out.println("Ok, warmup done. Now doing real testing, running for "+SECS+" seconds.");

        long nextTime = System.currentTimeMillis();
        long endTime = nextTime + (SECS * 1000);
        int count = 0;
        total = 0;
        int subtotal = 0;
        final long SUB_PERIOD = 1000L; // print once a second
        nextTime += SUB_PERIOD;
        final int REPS = _batchSize;

        /* Let's try to reduce overhead of System.currentTimeMillis()
         * by calling test method twice each round. May be a problem for
         * big docs/slow readers... but otherwise not.
         */
        while (true) {
            for (int i = 0; i < REPS; ++i) {
                total += testExec(tf, payload);
            }
            count += REPS;
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            /* let's only print once a second... limits console overhead,
             * but still informs about progress.
             */
            ++subtotal;
            if (now > nextTime) {
                char c;
                subtotal -= 1;
                if (subtotal > 35) {
                    c = '+';
                } else if (subtotal > 9) {
                    c = (char) ('a' + (subtotal-10));
                } else {
                    c = (char) ('0' + subtotal);
                }
                System.out.print(c);
                nextTime += SUB_PERIOD;
                if (nextTime < now) {
                    nextTime = now;
                }
                subtotal = 0;
            }
        }

        System.out.println("Total iterations done: "+count+" [done "+total+"]");
    }
}

