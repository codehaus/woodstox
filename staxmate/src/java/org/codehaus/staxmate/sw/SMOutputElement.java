package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

/**
 * Output class that models an outputtable XML element.
 */
public class SMOutputElement
    extends SMOutputContainer
{
    // No output done, due to blocking:
    protected final static int OUTPUT_NONE = 0;
    // Element name and prefix output, possibly some attributes
    protected final static int OUTPUT_ATTRS = 1;
    // Start element completely output:
    protected final static int OUTPUT_CHILDREN = 2;
    // End element output, ie. fully closed
    protected final static int OUTPUT_CLOSED = 3;

    /*
    /////////////////////////////////////////////
    // Element properties
    /////////////////////////////////////////////
    */

    final String mLocalName;
    final SMNamespace mNs;

    /*
    /////////////////////////////////////////////
    // Output state
    /////////////////////////////////////////////
    */

    protected int mOutputState = OUTPUT_NONE;

    protected SMOutputElement(SMOutputContext ctxt, SMOutputContainer parent,
                              String localName, SMNamespace ns,
                              boolean parentBlocked)
        throws XMLStreamException
    {
        super(ctxt, parent);
        mLocalName = localName;
        mNs = ns;
	if (!parentBlocked) { // can output start element right away?
	    // !!! TBI: Namespace binding etc
	    getWriter().writeStartElement(ns.getURI(), localName);
	    mOutputState = OUTPUT_ATTRS;
	}
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
        final SMOutputContext ctxt = mContext;

	// Let's make sure NS declaration is good, first:
        if (ns == null) {
            ns = ctxt.getEmptyNamespace();
        } else if (!ns.isValidIn(ctxt)) { // shouldn't happen, but...
            /* Let's find instance from our current context, instead of the
             * one from some other context
             */
            ns = getNamespace(ns.getURI());
        }

	// Ok, what can we do, then?
	switch (mOutputState) {
	case OUTPUT_NONE: // blocked
	    // !!! TBI: buffer attribute to this element
	    break;
	case OUTPUT_ATTRS: // perfect
	    // !!! TBI: make sure namespace is bound etc
	    ctxt.writeAttribute(localName, ns, value);
	    break;
	default:
	    throwClosedForAttrs();
	} 

	if (canOutputNewChild()) {
	} else {
	}
    }

    protected void throwClosedForAttrs()
    {
	String desc = (mOutputState == OUTPUT_CLOSED) ?
	    "CLOSED" : "CHILDREN";
        throw new IllegalStateException("Can't add attributes for an element in state '"
					+desc+"' ("+mOutputState+")");
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
