package sample;

import java.io.*;
import java.security.MessageDigest;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;
import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Example servlet that will serve all Files in specified directory,
 * wrapped in an XML document. Contents of files are base64 encoded.
 * An SHA message digest (hash) is also computed and included in
 * an element for verificatio purposes (or in this just to show
 * how base64 encoded binary can be used with attributes as well
 * as regular character data).
 */
public class BinaryService
    extends HttpServlet
{
    final static String DIGEST_TYPE = "SHA";

    XMLOutputFactory _xmlOutputFactory;

    /**
     * Directory that contains files to be made downloadable
     */
    File _downloadableFiles;

    @Override
    public void init(ServletConfig cfg)
    {
        _xmlOutputFactory = XMLOutputFactory.newInstance();
        // NOTE: Should configure from something; init-params or such
        _downloadableFiles = new File("/tmp/testfiles");
        if (!_downloadableFiles.isDirectory()) {
            throw new IllegalArgumentException("No directory '"+_downloadableFiles+"'");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        resp.setContentType("text/xml");
        try {
            writeFileContentsAsXML(resp.getOutputStream());
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void writeFileContentsAsXML(OutputStream out)
        throws IOException, XMLStreamException
    {
        XMLStreamWriter2 sw = (XMLStreamWriter2) _xmlOutputFactory.createXMLStreamWriter(out);
        sw.writeStartDocument();
        sw.writeStartElement("files");
        byte[] buffer = new byte[4000];
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(DIGEST_TYPE);
        } catch (Exception e) { // no such hash type?
            throw new IOException(e);
        }

        for (File f : _downloadableFiles.listFiles()) {
            sw.writeStartElement("file");
            sw.writeAttribute("name", f.getName());
            sw.writeAttribute("checksumType", DIGEST_TYPE);
            FileInputStream fis = new FileInputStream(f);
            int count;
            while ((count = fis.read(buffer)) != -1) {
                md.update(buffer, 0, count);
                sw.writeBinary(buffer, 0, count);
            }
            fis.close();
            sw.writeEndElement(); // file
            sw.writeStartElement("checksum");
            sw.writeBinaryAttribute("", "", "value", md.digest());
            sw.writeEndElement(); // checksum
        }
        sw.writeEndElement(); // files
        sw.writeEndDocument();
        sw.close();
    }
}
