package org.codehaus.stax2;

import javax.xml.stream.Location;

/**
 * Extension of {@link Location} that adds accessor to retrieve nested
 * location information.
 */
public interface XMLStreamLocation2
    extends Location
{
    /**
     * Method that can be used to get exact byte offset (number of bytes
     * read from the stream right before getting to this location) in the
     * stream that is pointed to by this Object, if such information is
     * available.
     * Generally information is NOT available if the stream reader was
     * created from a character-only source (Reader, String); otherwise
     * it may be available.
     *<p>
     * Note: this value MAY be the same as the one returned by
     * {@link #getCharacterOffset}, but usually only for single-byte
     * character streams (Ascii, ISO-Latin).
     *
     * @return Byte offset (== number of bytes reader so far) within the
     *   underlying stream, at location this object represents, if the
     *   stream (and stream reader) are able to provide this (separate
     *   from the character offset, for variable-byte encodings); 
     *   -1 if not.
     */
    public long getActualByteOffset();

    /**
     * Method that can be used to get exact characters offset (number of
     * characters read from the stream right before getting to this location)
     * in the stream that is pointed to by this Object, if such information is
     * available.
     * This information should usually be available, independent of the
     * input source, since the stream reader always deals with characters.
     *<p>
     * Note: this value MAY be the same as the one returned by
     * {@link #getCharacterOffset}; this is the case for single-byte
     * character streams (Ascii, ISO-Latin), as well as for streams for
     * which byte offset information is not available (Readers, Strings).
     *
     * @return Byte offset (== number of bytes reader so far) within the
     *   underlying stream, at location this object represents, if the
     *   stream (and stream reader) are able to provide this (separate
     *   from the character offset, for variable-byte encodings); 
     *   -1 if not.
     */
    public long getActualCharacterOffset();

    /**
     * Method that can be used to traverse nested locations, like ones
     * created when expanding entities (especially external entities).
     * If so, single location object only contains information about
     * specific offsets and ids, and a link to its context. Outermost
     * location will return null to indicate there is no more information
     * to retrieve.
     *
     * @return Location in the context (parent input source), if any;
     *    null for locations in the outermost known context
     */
    public XMLStreamLocation2 getContext();
}
