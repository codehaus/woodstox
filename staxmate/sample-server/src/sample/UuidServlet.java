package sample;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
// Stax API:
import javax.xml.stream.XMLStreamException;

// StaxMate:
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.in.*;
import org.codehaus.staxmate.out.*;

// Java Uuid Generator:
import org.safehaus.uuid.EthernetAddress; // for time+location based method
import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

/**
 * This is the simple UUID servlet, implemented using StaxMate XML
 * library and JUG Uuid Generator.
 *<p>
 * POST and GET implementations are different just to demonstrate
 * that it's easy to allow both query-parameter (GET)
 * and XML-document (POST) approaches to implementing REST (aka
 * Plain Old Xml == POX) services. And of course, how easy it
 * is to both write and read xml using StaxMate.
 *<p>
 * Here's sample xml request format:
 *<pre>
 *  <request>
 *   <generate-uuid method="random" />
 *   <generate-uuid method="location" count="3" />
 *   <generate-uuid method="name">http://www.cowtowncoder.com/foo</generate-uuid>
 *  </request>
 *</pre>
 *<p>
 * And here's a sample response, for given request:
 *<pre>
 *  <response>
 *   <uuid></uuid>
 *  </respone>
 *</pre>
 *<p>
 * Additionally, query interface recognizes following parameters:
 * <ul>
 *  <li>method: same as method attribute
 *   </li>
 *  <li>count: same as count attribute
 *   </li>
 *  <li>name: argument used with method 'name'
 *   </li>
 *  </ul>
 */
public class UuidServlet extends HttpServlet
{
    /**
     * Could require Ethernet address to be passed, or could use
     * JNI-based access: but for now, let's just generate a
     * dummy (multicast) address.
     */
    final EthernetAddress mMacAddress;

    public UuidServlet() {
        mMacAddress = UUIDGenerator.getInstance().getDummyAddress();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        try {
            // First, let's determine the method to use
            UUIDMethod method = determineMethod(req.getParameter("method"));
            String str = req.getParameter("count");
            int count = (str == null || str.length() == 0) ? 1 : determineCount(str);
            String name = req.getParameter("name");
            checkParameters(method, count, name);
            List<UUID> uuids = generateUuids(method, count, name);
            writeResponse(resp, uuids);
        } catch (Throwable t) {
            reportProblem(resp, null, t);
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        try {
            UUIDMethod method = null;
            int count = 1;
            String name = null;

            // Let's use the global Stax factory for the example
            InputStream in = req.getInputStream();
            SMInputCursor rootc = SMInputFactory.rootElementCursor(SMInputFactory.getGlobalXMLInputFactory().createXMLStreamReader(in));
            rootc.getNext(); // well-formed docs have single root

            // And root element should be "<request>"
            if (!"request".equals(rootc.getLocalName())) {
                reportProblem(resp, "Root element not <request>, as expected, but <"+rootc.getLocalName()+">", null);
                return;
            }
            // Request has no attributes, but has 0+ methods (batches)
            SMInputCursor requests = rootc.childElementCursor();

            // !!! TBI
        } catch (Throwable t) {
            reportProblem(resp, "Failed to process POST request", t);
        }
    }

    void writeResponse(HttpServletResponse resp, List<UUID> uuids)
        throws IOException, XMLStreamException
    {
        resp.setContentType("text/xml");
        OutputStream out = resp.getOutputStream();
        SMOutputElement rootElem = writeDocWithRoot(out, "response");
        for (UUID uuid : uuids) {
            rootElem.addElement("uuid").addCharacters(uuid.toString());
        }
        // Need to close the root, to ensure all elements closed, flushed
        ((SMOutputDocument)rootElem.getParent()).closeRoot();
        out.flush();
    }

    SMOutputElement writeDocWithRoot(OutputStream out, String nonnsRootName)
        throws XMLStreamException
    {
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(SMOutputFactory.getGlobalXMLOutputFactory().createXMLStreamWriter(out, "UTF-8"), "1.0", "UTF-8", true);
        /* Let's indent for debugging purposes: in production usually
         * shouldn't, to minimize message size. These settings give linefeed,
         * plus 2 spaces per level (initially just one char from the string,
         * linefeed, then 2 more chars per level
         */
        doc.setIndentation("\n                                    ", 1, 2);
        return doc.addElement(nonnsRootName);
    }

    void reportProblem(HttpServletResponse resp, String msg,
                       Throwable t)
        throws IOException
    {
        resp.setContentType("text/xml");
        OutputStream out = resp.getOutputStream();

        try {
            SMOutputElement rootElem = writeDocWithRoot(out, "error");
            
            // Let's customize a bit based on type of exception:
            if (t instanceof IllegalArgumentException) {
                // no need to pass exception, message is all we need
                msg = "Input argument problem: "+t;
                t = null;
            } else if (t instanceof XMLStreamException) {
                msg = "Problem parsing xml request: "+t;
            } else {
                if (msg == null) {
                    msg = "Problem processing request";
                }
            }
            rootElem.addElement("msg").addCharacters(msg);
            if (t != null) {
                SMOutputElement elem = rootElem.addElement("cause");
                elem.addAttribute("type", t.getClass().toString());
                elem.addCharacters(t.getMessage());
            }
            ((SMOutputDocument)rootElem.getParent()).closeRoot();
        } catch (XMLStreamException strex) {
            IOException ioe = new IOException(strex.getMessage());
            ioe.initCause(strex);
            throw ioe;
        }
    }
    
    List<UUID> generateUuids(UUIDMethod method, int count, String name)
    {
        UUIDGenerator gen = UUIDGenerator.getInstance();
        ArrayList<UUID> uuids = new ArrayList<UUID>(count);
        for (int i = 0; i < count; ++i) {
            UUID uuid;

            switch (method) {
            case RANDOM: // UUID using ~128 bits of randomness
                uuid = gen.generateRandomBasedUUID();
                break;
            case TIME: // UUID using time+location
                uuid = gen.generateTimeBasedUUID(mMacAddress);
                break;
            case NAME: // UUID computed from the given name
                /* Note: we do NOT use a context value, for simplicity --
                 * usually one should be used, and UUID class already
                 * specifies 4 suggested standard contexts
                 */
                uuid = gen.generateNameBasedUUID(null, name);
                break;
            default:
                throw new Error(); // never gets here
            }
            uuids.add(uuid);
        }
        return uuids;
    }

    // // // Input access, validation

    private int determineCount(String str)
    {
        if (str == null || str.length() == 0) { // missing? Defaults to 1
            return 1;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nex) {
            throw new IllegalArgumentException("Value of parameter 'count' not numeric ('"+str+"')");
        }
    }

    private UUIDMethod determineMethod(String str)
    {
        if (str == null || str.length() == 0) { // missing? Default to random
            return UUIDMethod.RANDOM;
        }
        try {
            return UUIDMethod.valueOf(str);
        } catch (IllegalArgumentException ex) {
            // Let's improve the message, make it more accurate
            throw new IllegalArgumentException("Unrecognized method '"+str+"', needs to be one of RANDOM, TIME (default) or NAME");
        }
    }

    private void checkParameters(UUIDMethod method, int count, String name)
    {
        if (method == UUIDMethod.NAME) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("Missing 'name' argument for UUID generation method NAME");
            }
        }
        if (count < 1) {
            throw new IllegalArgumentException("Illegal count value ("+count+"), has to be non-zero positive number");
        }
    }

    /**
     * Enumeration used to define
     */
    enum UUIDMethod {
        RANDOM, TIME, NAME
    }
}
