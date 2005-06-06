package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

/**
 * Output class that models an outputtable XML element.
 */
public class SMOutputElement
    extends SMOutputContainer
{
    final String mLocalName;
    final SMNamespace mNs;

    protected SMOutputElement(SMOutputContext ctxt, SMOutputContainer parent,
                              String localName, SMNamespace ns,
                              boolean parentBlocked)
    {
        super(ctxt, parent);
        mLocalName = localName;
        mNs = ns;
    }
    
    public String getLocalName() {
        return mLocalName;
    }
    
    public SMNamespace getNamespace() {
        return mNs;
    }

    /*
    ///////////////////////////////////////////////////////////
    // Additional output methods
    ///////////////////////////////////////////////////////////
     */

    public void addAttribute(String localName, SMNamespace ns,
			     String value)
	throws XMLStreamException
    {
	if (canOutputNewChild()) {
	    mContext.writeAttribute(localName, ns, value);
	} else {
	    // !!! TBI: add attribute to this element
	}
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
     */

    protected void childReleased(SMLinkedOutput child)
	throws XMLStreamException
    {
	// !!! TBI
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	// !!! TBI
	return false;
    }

    protected void forceOutput()
	throws XMLStreamException
    {
	// !!! TBI
    }

    public boolean canOutputNewChild()
	throws XMLStreamException
    {
	// !!! TBI
	return false;
    }
}
