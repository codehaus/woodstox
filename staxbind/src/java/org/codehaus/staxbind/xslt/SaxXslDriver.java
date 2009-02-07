package org.codehaus.staxbind.xslt;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.*;

import org.xml.sax.*;

public final class SaxXslDriver
    extends XslDriver
{
    XMLReader _xmlReader;

    public SaxXslDriver() { }

    @Override
    public void initializeDriver()
    {
        super.initializeDriver();

        String key = "javax.xml.parsers.SAXParserFactory";
        String fc = getParam(key);
        if (fc == null) {
            throw new IllegalArgumentException("Missing setting for parameter '"+key+"'");
        }

        // Defaulting: passing empty String means "use default SAX parser"
        if (fc.length() == 0) {
            _xmlReader = null;
        } else {
            try {
                SAXParserFactory f = (SAXParserFactory) Class.forName(fc).newInstance();
                f.setNamespaceAware(true);
                _xmlReader = f.newSAXParser().getXMLReader();
            } catch (Exception e) {
                throw wrapException(e);
            }
        }
    }

    protected final void _transform(Transformer tx, ByteArrayInputStream in,
                                    ByteArrayOutputStream out)
        throws Exception
    {
        if (_xmlReader == null) { // default sax parser
            tx.transform(new StreamSource(in), new StreamResult(_out));
        } else {
            tx.transform(new SAXSource(_xmlReader, new InputSource(_in)), new StreamResult(_out));
        }
    }
}
