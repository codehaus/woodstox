package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Output class that models a full XML document, with xml declaration.
 */
public class SMOutputDocument
    extends SMRootOutput
{
    final XMLStreamWriter mWriter;

    protected SMOutputDocument(SMOutputContext ctxt)
	throws XMLStreamException
    {
	super(ctxt);
	mWriter = getWriter();
	mWriter.writeStartDocument();
    }

    protected SMOutputDocument(SMOutputContext ctxt,
			       String version, String encoding)
	throws XMLStreamException
    {
	super(ctxt);
	mWriter = getWriter();
	// note: Stax 1.0 has weird ordering for the args...
	mWriter.writeStartDocument(encoding, version);
    }

    protected SMOutputDocument(SMOutputContext ctxt,
			       String version, String encoding, boolean standalone)
	throws XMLStreamException
    {
	super(ctxt);
	mWriter = ctxt.getWriter();

	// Can we use StAX2?
	if (mWriter instanceof XMLStreamWriter2) {
	    ((XMLStreamWriter2) mWriter).writeStartDocument(version, encoding, standalone);
	} else {
	    // note: Stax 1.0 has weird ordering for the args...
	    mWriter.writeStartDocument(encoding, version);
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
	if (mWriter instanceof XMLStreamWriter2) {
	    ((XMLStreamWriter2) mWriter).writeDTD
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
	    mWriter.writeDTD(dtd);
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
	// Let's do this first, just in case (not strictly necessary)
	mWriter.flush();
	// And then this should take care of all open elements, if any:
	mWriter.writeEndDocument();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */
}
