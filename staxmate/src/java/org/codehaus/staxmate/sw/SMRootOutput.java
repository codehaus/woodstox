package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Intermediate base class common to all StaxMate root-level container
 * outputters.
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
    /**
     * Simple state flag; children can only be added when root container
     * is still active.
     */
    protected boolean mActive;

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

}
