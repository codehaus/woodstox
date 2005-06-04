package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

/**
 * Interface that denotes output objects (fragments, elements) that explicitly
 * start their life-cycle as buffered (other objects can be implicitly buffered
 * due to explict ones as parents or previous siblings).
 */
public interface SMBufferable
{
    /**
     * Method called to signal that the node need not be buffered any more
     * (if not required to do so by parent/children restrictions)
     */
    public void release()
	throws XMLStreamException;

    /**
     * @return True if this object is still buffered; false if not
     */
    public boolean isBuffered();

    /**
     * Method called by a container when bufferable item is linked as its
     * child.
     */
    public void linkParent(SMOutputContainer parent);
}
