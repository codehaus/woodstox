import java.io.*;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;
import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

public class BinaryService
    extends HttpServlet
{
    XMLInputFactory _xmlInputFactory;

    /**
     * Directory that contains files to be made downloadable
     */
    File _downloadableFiles;

    @Override
    public void init(ServletConfig cfg)
    {
        _xmlInputFactory = XMLInputFactory.newInstance();
        // NOTE: Should configure from something; init-params or such
        _downloadableFiles = new File("/tmp/testfiles");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
    }
}
