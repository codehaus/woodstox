package org.codehaus.stax2;

import java.io.File;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Extension of {@link XMLInputFactory} to add missing functionality.
 *<p>
 * Note: currently just a placeholder, but will add URL-based constructors
 * RSN.
 */
public abstract class XMLInputFactory2
    extends XMLInputFactory
{
    protected XMLInputFactory2() {
        super();
    }

    // // // New event reader creation methods:

    /**
     * Factory method that allows for parsing a document accessible via
     * specified URL. Note that URL may refer to all normal URL accessible
     * resources, from files to web- and ftp-accessible documents.
     */
    public abstract XMLEventReader2 createXMLEventReader(URL src)
        throws XMLStreamException;

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    public abstract XMLEventReader2 createXMLEventReader(File f)
        throws XMLStreamException;

    // // // New stream reader creation methods:

    /**
     * Factory method that allows for parsing a document accessible via
     * specified URL. Note that URL may refer to all normal URL accessible
     * resources, from files to web- and ftp-accessible documents.
     */
    public abstract XMLStreamReader2 createXMLStreamReader(URL src)
        throws XMLStreamException;

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    public abstract XMLStreamReader2 createXMLStreamReader(File f)
        throws XMLStreamException;
}
