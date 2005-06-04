package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Output class that models a full XML document, with xml declaration.
 */
public class SMOutputDocument
    extends SMRootFragment
{
    protected SMOutputDocument(SMOutputContext ctxt)
        throws XMLStreamException
    {
        super(ctxt);
        getWriter().writeStartDocument();
    }
    
    protected SMOutputDocument(SMOutputContext ctxt,
                               String version, String encoding)
        throws XMLStreamException
    {
        super(ctxt);
        // note: Stax 1.0 has weird ordering for the args...
        getWriter().writeStartDocument(encoding, version);
    }

    protected SMOutputDocument(SMOutputContext ctxt,
                               String version, String encoding, boolean standalone)
        throws XMLStreamException
    {
        super(ctxt);
        XMLStreamWriter w = ctxt.getWriter();

        // Can we use StAX2?
        if (w instanceof XMLStreamWriter2) {
            ((XMLStreamWriter2) w).writeStartDocument(version, encoding, standalone);
        } else {
            // note: Stax 1.0 has weird ordering for the args...
            w.writeStartDocument(encoding, version);
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Overridden output methods
    ///////////////////////////////////////////////////////////
    */

    /*
    ///////////////////////////////////////////////////////////
    // Additional output methods
    //
    // note: no validation is done WRT ordering since underlying
    // stream writer is likely to catch them.
    ///////////////////////////////////////////////////////////
    */

    public void addDoctypeDeclaration(String rootName,
                                      String systemId, String publicId)
        throws XMLStreamException
    {
        addDoctypeDeclaration(rootName, systemId, publicId, null);
    }

    public void addDoctypeDeclaration(String rootName,
                                      String systemId, String publicId,
                                      String intSubset)
        throws XMLStreamException
    {
        XMLStreamWriter w = getWriter();
        if (w instanceof XMLStreamWriter2) {
            ((XMLStreamWriter2) w).writeDTD
                (rootName, systemId, publicId, intSubset);
        } else {
            // Damn this is ugly, with stax1.0...
            String dtd = "<!DOCTYPE "+rootName;
            if (publicId == null) {
                if (systemId != null) {
                    dtd += " SYSTEM";
                }
            } else {
                dtd += " PUBLIC '"+publicId+"'";
            }
            if (systemId != null) {
                dtd += " '"+systemId+"'";
            }
            if (intSubset != null) {
                dtd += " ["+intSubset+"]";
            }
            dtd += ">";
            w.writeDTD(dtd);
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
    */

    /**
     * Method that HAS to be called when all additions have been done
     * via StaxMate API. Since it is possible that the underlying stream
     * writer may be buffering some parts, it needs to be informed of
     * the closure.
     */
    public void closeRoot()
        throws XMLStreamException
    {
        super.closeRoot();

        // And finally, let's indicate stream writer about closure too...
        XMLStreamWriter w = getWriter();
        w.writeEndDocument();
        w.close();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
    */
}
