package test;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.*;
import javax.xml.stream.*;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class TestDomReader
{
    public static void main(String[] args)
        throws Exception
    {
        String XML = "<blah xmlns=\"http://blah.org\"><foo>foo</foo></blah>";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(XML)));
        Source source = new DOMSource(doc);
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLStreamReader sr = f.createXMLStreamReader(source);
        
        while (sr.hasNext()) {
            int type = sr.next();
            System.out.print("["+type+"]");
            
            if (sr.hasName()) {
                System.out.println(" name = '"+sr.getName()+"'");
            }
            
            System.out.println();
        }
    }
}


