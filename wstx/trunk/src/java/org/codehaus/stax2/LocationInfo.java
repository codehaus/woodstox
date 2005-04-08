package org.codehaus.stax2;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Interface that specifies additional access methods for accessing
 * full location information of an input location within a stream reader.
 * Access interface may be directly implemented by the reader, or by
 * another (reusable or per-call-instantiated) helper object.
 *<p>
 * Note: instances of LocationInfo are only guaranteed to persist as long
 * as the (stream) reader points to the current element (whatever it is).
 * After next call to <code>streamReader.next</code>, it it possible that
 * the previously accessed LocationInfo points to the old information, new
 * information, or may even contain just garbage. That is, for each new
 * event, <code>getLocationInfo</code> should be called separately.
 */
public interface LocationInfo
{
    // // // Existing method from XMLStreamReader:

    public Location getLocation();

    // // // New methods:

    /**
     * An optional method that either returns the location object that points the
     * starting position of the current event, or null if implementation
     * does not keep track of it (some may return only end location; and
     * some no location at all).
     *<p>
     * Note: since it is assumed that the start location must either have
     * been collected by now, or is not accessible (i.e. implementation
     * [always] returns null), no exception is allowed to be throws, as
     * no parsing should ever need to be done (unlike with
     * {@link #getEndLocation}).
     *
     * @return Location of the first character of the current event in
     *   the input source (which will also be the starting location
     *   of the following event, if any, or EOF if not), or null (if
     *   implementation does not track locations).
     */
    public XMLStreamLocation2 getStartLocation();

    /**
     * An optional method that either returns the location object that points the
     * ending position of the current event, or null if implementation
     * does not keep track of it (some may return only start location; and
     * some no location at all).
     *<p>
     * Note: since some implementations may not yet know the end location
     * (esp. ones that do lazy loading), this call may require further
     * parsing. As a result, this method may throw a parsing or I/O
     * errors.
     *
     * @return Location right after the end
     *   of the current event (which will also be the start location of
     *   the next event, if any, or of EOF otherwise).
     *
     * @throws XMLStreamException If the stream reader had to advance to
     *  the end of the event (to find the location), it may encounter a
     *  parsing (or I/O) error; if so, that gets thrown
     */
    public XMLStreamLocation2 getEndLocation()
        throws XMLStreamException;

    /**
     * A method that returns the current location of the stream reader
     * at the input source. This is somewhere between the start
     * and end locations (inclusive), depending on how parser does it
     * parsing (for non-lazy implementations it's always the end location;
     * for others something else).
     *<p>
     * Since this location information should always be accessible, no
     * further parsing is to be done, and no exceptions can be thrown.
     *
     * @return Location of the next character reader will parse in the
     *   input source.
     */
    public XMLStreamLocation2 getCurrentLocation();
}
