package org.codehaus.staxbind.xslt;

import java.io.*;
import javax.xml.stream.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

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
    /*
    //////////////////////////////////////////
    // Configuration, driver
    //////////////////////////////////////////
     */

    protected File _xmlDir, _xslDir;

    protected TransformerFactory _xslFactory;

    /*
    //////////////////////////////////////////
    // Configuration, test case
    //////////////////////////////////////////
     */

    protected Templates _stylesheet;

    protected ByteArrayInputStream _in;

    protected ByteArrayOutputStream _out;

    /*
    //////////////////////////////////////////
    // Test state, results
    //////////////////////////////////////////
     */

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

    /*
    //////////////////////////////////////////
    // Actual Japex API impl
    //////////////////////////////////////////
     */

    @Override
    public void initializeDriver()
    {
        // Where are the docs?
        File basedir = new File(getParam("japex.inputDir"));
        _xmlDir = new File(basedir, "xml");
        if (!_xmlDir.exists()) {
            throw new IllegalArgumentException("No input dir '"+_xmlDir.getAbsolutePath()+"'");
        }
        _xslDir = new File(basedir, "xsl");
        if (!_xslDir.exists()) {
            throw new IllegalArgumentException("No input dir '"+_xslDir.getAbsolutePath()+"'");
        }

        // First: which transformer factory should we use?
        String key = "javax.xml.transform.TransformerFactory";
        String xslf = getParam(key);
        if (xslf == null) {
            throw new IllegalArgumentException("Missing setting for parameter '"+key+"'");
        }
        try {
            _xslFactory = (TransformerFactory) Class.forName(xslf).newInstance();
        } catch (Exception e) {
            throw wrapException(e);
        }

        /*
        (getParam("javax.xml.stream.XMLInputFactory"),
        ((StaxXmlConverter) _converter).initStax
            (getParam("javax.xml.stream.XMLInputFactory"),
             getParam("javax.xml.stream.XMLOutputFactory")
             );
        */
    }
    
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

        File xslFile = new File(_xslDir, xslName);
        if (!xslFile.exists()) {
            throw new IllegalArgumentException("No input file '"+xslFile.getAbsolutePath()+"'");
        }
        File xmlFile = new File(_xmlDir, xmlName);
        if (!xmlFile.exists()) {
            throw new IllegalArgumentException("No input file '"+xmlFile.getAbsolutePath()+"'");
        }
        try {
            byte[] data = readAll(xmlFile);
            _in = new ByteArrayInputStream(data);
            _out = new ByteArrayOutputStream(4000); // will get resized

            _stylesheet = null; // clear old one
            _stylesheet = _xslFactory.newTemplates(new StreamSource(xslFile));
        } catch (Exception e) {
            throw wrapException(e);
        }
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

        _in.reset();
        _out.reset();

        try {
            Transformer tx = _stylesheet.newTransformer();
            tx.transform(new StreamSource(_in), new StreamResult(_out));
        } catch (Exception e) {
            throw wrapException(e);
        }

        _bogusResult = _out.size();
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

    /*
    /////////////////////////////////////////////////
    // Internal helper methods
    /////////////////////////////////////////////////
     */

    private RuntimeException wrapException(Exception e)
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

