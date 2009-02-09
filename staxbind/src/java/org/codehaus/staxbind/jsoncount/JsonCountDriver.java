package org.codehaus.staxbind.jsoncount;

import java.io.*;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

/**
 * Base class for a simple Json-based test case, where instance
 * counts of distinct field names are counted.
 */
public abstract class JsonCountDriver
    extends JapexDriverBase
{
    /*
    //////////////////////////////////////////
    // Configuration, test case
    //////////////////////////////////////////
     */

    protected File _docDir;

    /*
    //////////////////////////////////////////
    // Test state
    //////////////////////////////////////////
     */

    protected byte[] _docData;
    protected int _docLen;

    /**
     * We'll keep track of field (name) instances throughout
     * tests; the idea is just to ensure that the document
     * is processed.
     */
    protected CountResult _results;

    protected JsonCountDriver() { }

    /*
    //////////////////////////////////////////
    // Actual Japex API impl
    //////////////////////////////////////////
     */

    @Override
    public void initializeDriver()
    {
        // Where are the input docs?
        _docDir = new File(getParam("japex.inputDir"));
        if (!_docDir.exists()) {
            throw new IllegalArgumentException("No input dir '"+_docDir.getAbsolutePath()+"' exists");
        }
    }
    
    @Override
    public void prepare(TestCase testCase)
    {
        // we'll use test case name as expected file name (minus .json)
        String filename = testCase.getName()+".json";
        File jsonFile = new File(_docDir, filename);

        if (!jsonFile.exists()) {
            throw new IllegalArgumentException("No input file '"+jsonFile.getAbsolutePath()+"'");
        }
        try {
            _docData = readAll(jsonFile);
            _docLen = _docData.length;
        } catch (Exception e) {
            throw wrapException(e);
        }

        /* And then, let's verify that this driver produces expected
         * results!
         */
        _results = new CountResult();
        run(testCase);
        ResultVerifier.checkResults(getClass(), _docData, _results);

        // should we now clear the results?
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
        try {
            read(_docData, _results);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    /**
     * Main test method, sub-classes implement.
     */
    protected abstract void read(byte[] docData, CountResult results)
        throws Exception;
    
    @Override
    public void finish(TestCase testCase)
    {
        // Set file size in KB on X axis
        testCase.setDoubleParam("japex.resultValueX", ((double) _docLen) / 1024.0);
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
    // Internal helper methods
    /////////////////////////////////////////////////
     */

    protected RuntimeException wrapException(Exception e)
    {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    protected byte[] readAll(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] result = new byte[len];
        FileInputStream fis = new FileInputStream(f);
        int offset = 0;

        while (offset < len) {
            int count = fis.read(result, offset, result.length-offset);
            offset += count;
        }
        fis.close();

        return result;
    }
}

