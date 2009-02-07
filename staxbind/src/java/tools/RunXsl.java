package tools;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;

import org.xml.sax.*;

public class RunXsl
{
    private RunXsl() { }

    protected String doIt(String[] args)
        throws Exception
    {
        if (args.length != 4) {
            return "Usage: java "+getClass().getName()+" [processor: xalan/saxon] [parser: xerces/woodstox/aalto] [xslfile] [xmlfile]";
        }
        String procName = args[0];
        String procClass = null;

        if (procName.equals("saxon")) {
            procClass = "net.sf.saxon.TransformerFactoryImpl";
        } else if (procName.equals("xalan")) {
            procClass = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
        } else {
            return "Known xsl processors: saxon, xalan";
        }
        TransformerFactory txf = (TransformerFactory) Class.forName(procClass).newInstance();

        String parserName = args[1];
        String parserClass = null;

        if (parserName.equals("xerces")) {
            parserClass = "org.apache.xerces.jaxp.SAXParserFactoryImpl";
        } else if (parserName.equals("woodstox")) {
            parserClass = "com.ctc.wstx.sax.WstxSAXParserFactory";
        } else if (parserName.equals("aalto")) {
            parserClass = "org.codehaus.wool.sax.SAXParserFactoryImpl";
        } else {
            return "Known xml parsers: aalto, woodstox, xerces";
        }

        SAXParserFactory xmlF = (SAXParserFactory) Class.forName(parserClass).newInstance();
        xmlF.setNamespaceAware(true);
        XMLReader xr = xmlF.newSAXParser().getXMLReader();

        File xslFile = new File(args[2]);
        File xmlFile = new File(args[3]);
        Templates stylesheet = txf.newTemplates(new StreamSource(xslFile));
        Transformer tx = stylesheet.newTransformer();
        StringWriter sw = new StringWriter(1000);

        System.out.println("[START]");
        tx.transform(new SAXSource(xr, new InputSource(new FileInputStream(xmlFile))),
                     new StreamResult(sw));
        System.out.print(sw.toString());
        System.out.flush();
        System.out.println();
        System.out.println("[END]");

        return null;
    }

    public static void main(String[] args) throws Exception
    {
        String err = new RunXsl().doIt(args);
        if (err != null) {
            System.err.println("Problem: "+err);
            System.exit(1);
        }
    }
}
