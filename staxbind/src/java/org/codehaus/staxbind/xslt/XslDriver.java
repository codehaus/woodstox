package org.codehaus.staxbind.dbconv;

import java.io.*;
import javax.xml.stream.*;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

/**
 *<p>
 * Note: unlike most other test drivers, xsl driver doesn't extend
 * standard base classes.
 */
public final class XslDriver
    extends JapexDriverBase
{
    /**
     * We will keep track of fake result, just in case JIT might
     * sneakily try to eliminate unnecessary code.
     */
    protected int _bogusResult;

    /**
     * For now let's assume total length fits in 32-bit int
     */
    protected int _totalLength;

    public XslDriver() { }

    @Override
    public void prepare(TestCase testCase)
    {
        String name = testCase.getName();
        String xslName, xmlName;

        int ix = name.indexOf('_');
        if (ix < 0) { // same name
            xslName = xmlName = name;
        } else {
            xslName = name.substring(0, ix);
            xmlName = name.substring(ix+1);
        }

        try {
            //loadTestData(testCase, getOperation(testCase));
        } catch (Exception e) {
            RuntimeException re = (e instanceof RuntimeException) ?
                (RuntimeException) e : new RuntimeException(e);
            throw re;
        }
    }

    @Override
    public void initializeDriver() {
        // nothing to do, for now?
        /*
        (getParam("javax.xml.stream.XMLInputFactory"),
        ((StaxXmlConverter) _converter).initStax
            (getParam("javax.xml.stream.XMLInputFactory"),
             getParam("javax.xml.stream.XMLOutputFactory")
             );
        */
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
            //_bogusResult = runTest(oper);
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
        testCase.setDoubleParam("japex.resultValueX", ((double) _totalLength) / 1024.0);
        getTestSuite().setParam("japex.resultUnitX", "KB");

        /* TPS or MBps? For now, TPS seems more useful, given that input
         * sizes vary, and we really care more about how many docs get
         * processed.
         */
        getTestSuite().setParam("japex.resultUnit", "tps");
        //getTestSuite().setParam("japex.resultUnit", "mbps");
    }
}

