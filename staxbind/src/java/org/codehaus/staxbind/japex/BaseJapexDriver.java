package org.codehaus.staxbind.japex;

import java.io.*;
import java.util.*;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

/**
 * Common base class for all StaxBind test drivers needed to run
 * tests on Japex.
 */
public abstract class BaseJapexDriver<E extends Enum>
    extends JapexDriverBase
{
    protected final static int DEFAULT_BUF_SIZE = 4000;

    protected final Class<? extends Enum> _enumType;

    /**
     * We will keep track of fake result, just in case JIT might
     * sneakily try to eliminate unnecessary code.
     */
    protected int _bogusResult;

    /**
     * For now let's assume total length fits in 32-bit int
     */
    protected int _totalLength;

    protected E _operation;

    /*
    /////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////
    */

    /**
     * @param dummyOperation Argument needed to make generics work
     *   ok -- we need the actual Enum class object, to convert
     *   from String to Enum
     */
    protected BaseJapexDriver(E dummyOperation)
    {
        _enumType = dummyOperation.getClass();
    }

    /*
    /////////////////////////////////////////////////
    // Skeleton impl of test driver methods
    /////////////////////////////////////////////////
    */

    @Override
    public void prepare(TestCase testCase)
    {
        try {
            String operStr = testCase.getParam("japex.itemOperation");
            try {
                _operation = (E) Enum.valueOf(_enumType, operStr);
            } catch (Exception e) { // unrecognized/null etc
                ;
            }
            if (_operation == null) {
                throw new IllegalArgumentException("Invalid or missing value for japex.itemOperation (value: ["+operStr+"]), has to be one of valid alternatives for type "+_enumType);
            }
            loadTestData(testCase, _operation);
        } catch (Exception e) {
            RuntimeException re = (e instanceof RuntimeException) ?
                (RuntimeException) e : new RuntimeException(e);
            throw re;
        }
    }

    @Override
    public void initializeDriver() {
        // nothing to do, for now?
    }
    
    @Override
    public void warmup(TestCase testCase)
    {
        // Let's just run the test case once
        run(testCase);
    }

    @Override
    public void run(TestCase testCase)
    {
        _bogusResult = -1;
        _totalLength = 0;

        try {
            _bogusResult = runTest(_operation);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void finish(TestCase testCase)
    {
        /* First, let's access the bogus value to ensure it does
         * get calculated (i.e. can't be eliminated as dead code, just
         * in case that was possible otherwise)
         */
        getTestSuite().setParam("japex.dummyResult", String.valueOf(_bogusResult));

        // Set file size in KB on X axis
        testCase.setDoubleParam("japex.resultValueX", (double) (_totalLength >> 10));
        getTestSuite().setParam("japex.resultUnitX", "KB");

        /* TPS or MBps? For now, TPS seems more useful, given that input
         * sizes vary, and we really care more about how many docs get
         * processed.
         */
        getTestSuite().setParam("japex.resultUnit", "tps");
        //getTestSuite().setParam("japex.resultUnit", "mbps");
    }

    /*
    /////////////////////////////////////////////////
    // Accessors for sub-classes
    /////////////////////////////////////////////////
    */

    /*
    /////////////////////////////////////////////////
    // Abstract methods sub-classes are to implement
    /////////////////////////////////////////////////
    */

    protected abstract int runTest(E operation) throws Exception;

    protected abstract void doLoadTestData(E operation, File dir) throws Exception;

    /*
    /////////////////////////////////////////////////
    // Internal methods, setup
    /////////////////////////////////////////////////
    */
    
    protected final void loadTestData(TestCase testCase, E operation)
        throws Exception
    {
        String dirStr = testCase.getParam("japex.inputDir");
        if (dirStr == null) {
            throw new IllegalArgumentException("japex.inputDir not specified");
        }
        // Must also be an actual directory...
        File dir = new File(dirStr);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("japex.inputDir value ('"+dirStr+"') does not point to a valid directory (full: "+dir.getAbsolutePath()+")");
        }
        doLoadTestData(operation, dir);
    }

    protected void readAll(File f, byte[] buffer, ByteArrayOutputStream tmpStream)
        throws IOException
    {
        tmpStream.reset();
        int count;
        FileInputStream fis = new FileInputStream(f);

        while ((count = fis.read(buffer)) > 0) {
            tmpStream.write(buffer, 0, count);
        }
        fis.close();
    }
}
