package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Concrete non-buffered fragment (== container) class used as the root
 * level output container. Root-level does not necessarily have to mean
 * XML root level; it may also be a child context of a stream writer
 * in which StaxMate is only used to output specific sub-trees.
 */
public class SMRootFragment
    extends SMRootOutput
{
    public SMRootFragment(SMOutputContext ctxt)
	throws XMLStreamException
    {
	super(ctxt);
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
	// Let's first try to close them nicely:
	if (!doOutput(true)) {
	    /* but if that doesn't work, should just unbuffer all children...
	     */
	    // !!! TBI: need to recursively unbuffer
	}

	XMLStreamWriter writer = getWriter();
	// And this may also be a good idea:
	writer.flush();
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */
}
