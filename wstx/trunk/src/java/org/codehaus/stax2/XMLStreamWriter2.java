package org.codehaus.stax2;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Extended interface that implements functionality that is necessary
 * to properly build event API on top of {@link XMLStreamWriter},
 * as well as to configure individual instances.
 * It also adds limited number of methods that are important for
 * efficient pass-through processing (such as one needed when routing
 * SOAP-messages).
 */
public interface XMLStreamWriter2
    extends XMLStreamWriter
{
    /*
    ///////////////////////////
    // Configuration
    ///////////////////////////
    */

    /**
     * @param name Name of the feature of which value to get
     *
     * @return Value of the feature (possibly null), if supported; null
     *     otherwise
     */
    public Object getFeature(String name);

    /**
     * @param name Name of the feature to set
     * @param value Value to set feature to.
     */
    public void setFeature(String name, Object value);

    /*
    //////////////////////////////////////////
    // Write methods base interface is missing
    //////////////////////////////////////////
    */

    public void writeDTD(String rootName, String systemId, String publicId,
                         String internalSubset)
        throws XMLStreamException;

    /**
     * Method similar to {@link #writeEndElement}, but that will always
     * write the full end element, instead of empty element. This only
     * matters for cases where the element itself has no content, and
     * if writer is allowed to write empty elements when it encounters
     * such start/end element write pairs.
     */
    public void writeFullEndElement() throws XMLStreamException;

    /*
    ///////////////////////////
    // Pass-through methdods
    ///////////////////////////
    */

    /**
     * Method that writes specified content as is, without encoding or
     * deciphering it in any way. It will not update state of the writer
     * (except by possibly flushing output of previous writes, like
     * finishing a start element),
     * nor be validated in any way. As such, care must be taken, if this
     * method is used.
     *<p>
     * Method is usually used when encapsulating output from another writer
     * as a sub-tree, or when passing through XML fragments.
     */
    public void writeRaw(String text)
        throws XMLStreamException;

    /**
     * Method that writes specified content as is, without encoding or
     * deciphering it in any way. It will not update state of the writer
     * (except by possibly flushing output of previous writes, like
     * finishing a start element),
     * nor be validated in any way. As such, care must be taken, if this
     * method is used.
     *<p>
     * Method is usually used when encapsulating output from another writer
     * as a sub-tree, or when passing through XML fragments.
     */
    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException;

    /**
     * Method that essentially copies
     * event that the specified reader has just read.
     * This can be both more convenient
     * (no need to worry about details) and more efficient
     * than separately calling access methods of the reader and
     * write methods of the writer, since writer may know more
     * about reader than the application (and may be able to use
     * non-public methods)
     *
     * @param r Reader to use for accessing event to copy
     * @param preserveEventData If true, writer is not allowed to change
     *   the state of the reader (so that all the data associated with the
     *   current event has to be preserved); if false, writer is allowed
     *   to use methods that may cause some data to be discarded. Setting
     *   this to false may improve the performance, since it may allow
     *   full no-copy streaming of data, especially textual contents.
     */
    public void copyEventFromReader(XMLStreamReader2 r, boolean preserveEventData)
        throws XMLStreamException;
}
