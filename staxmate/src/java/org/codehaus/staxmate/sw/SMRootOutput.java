package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Intermediate base class common to all root-level StaxMate outputters.
 * Root-level simply means the outermost write context for StaxMate;
 * it may or may not be the actual XML document root level; it may also be
 * a child context of a stream writer
 * in which StaxMate is only used to output specific sub-trees.
 *<p>
 * There are separate concrete implementation classes for full document
 * and fragment outputs.
 */
public abstract class SMRootOutput
    extends SMOutputContainer
{
    public SMRootOutput(SMOutputContext ctxt)
	throws XMLStreamException
    {
	super(ctxt, null);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Additional public methods
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that has to be called when all additions have been done
     * via StaxMate API. It will release all buffered nodes (if any), as
     * well as force output to be flushed using the underlying writer.
     */
    public abstract void closeRoot()
	throws XMLStreamException;

    /*
    ///////////////////////////////////////////////////////////
    // Abstract base class methods
    ///////////////////////////////////////////////////////////
     */

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	if (canClose) {
	    return closeAndOutputChildren();
	}
	return closeAllButLastChild();
    }

    protected void forceOutput()
	throws XMLStreamException
    {
	forceChildOutput();
    }

    protected void childReleased(SMLinkedOutput child)
	throws XMLStreamException
    {
	/* The only child that can block output is the first one... 
	 * If that was released, may be able to output more as well.
	 * Note that since there's never parent (this is the root fragment),
	 * there's no need to try to inform anyone else.
	 */
	if (child == mFirstChild) {
	    closeAllButLastChild();
	}

	// Either way, we are now done
    }

    public boolean canOutputNewChild()
	throws XMLStreamException
    {
	return closeAndOutputChildren();
    }
}
