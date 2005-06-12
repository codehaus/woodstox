package org.codehaus.staxmate.sw;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Class that encapsulates details about context in which StaxMate output
 * is done. The most important of the details is the stream writer to use
 * (since that is eventually invoked to do the real output), and its
 * properties.
 *<p>
 * Usually the process of outputting XML content with StaxMate starts by
 * instantiating an {@link SMOutputContext}. It can then be used to create
 * output fragments; all of which bind to that context. Context is thus
 * what "connects" various fragments when they are buffered (when there
 * may or may not be child/parent relationships yet defined).
 *<p>
 * Context is also used (in addition to storing output relevant settings and
 * acting as a fragment factory)
 * as the owner of various other objects, most notable namespaces. All
 * local namespaces are owned by one and only one context.
 */
public final class SMOutputContext
{
    /*
    //////////////////////////////////////////////////////
    // Constants
    //////////////////////////////////////////////////////
    */

    protected final static SMNamespace sNsEmpty =
	new SMGlobalNamespace("", XMLConstants.DEFAULT_NS_PREFIX);
    protected final static SMNamespace sNsXml =
	new SMGlobalNamespace(XMLConstants.XML_NS_PREFIX,
			      XMLConstants.XML_NS_URI);
    protected final static SMNamespace sNsXmlns =
	new SMGlobalNamespace(XMLConstants.XMLNS_ATTRIBUTE,
			      XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

    final static HashMap sGlobalNsMap = new HashMap();
    static {
        sGlobalNsMap.put(sNsEmpty.getURI(), sNsEmpty);
        sGlobalNsMap.put(sNsXml.getURI(), sNsXml);
        sGlobalNsMap.put(sNsXmlns.getURI(), sNsXmlns);
    }

    /*
    //////////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////////
    */

    final XMLStreamWriter mStreamWriter;
    final boolean mRepairing;

    /*
    //////////////////////////////////////////////////////
    // State
    //////////////////////////////////////////////////////
    */

    HashMap mLocalNsMap = null;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
    */

    protected SMOutputContext(XMLStreamWriter sw)
    {
        mStreamWriter = sw;
        Object o = sw.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        mRepairing = (o instanceof Boolean) && ((Boolean) o).booleanValue();
    }
    
    /*
    //////////////////////////////////////////////////////
    // Factory methods, context creation
    //////////////////////////////////////////////////////
    */

    public static SMOutputContext createInstance(XMLStreamWriter sw)
        throws XMLStreamException
    {
        return new SMOutputContext(sw);
    }

    /*
    //////////////////////////////////////////////////////
    // Factory methods, full document writer creation
    //
    // These methods are used when creating full stand-alone
    // documents using StaxMate
    //////////////////////////////////////////////////////
    */

    /**
     * Method used to create a StaxMate output fragment that corresponds
     * to a single well-formed XML document. Assumption, then, is that
     * the underlying stream writer has only been created, but no writes
     * have yet been done.
     *<p>
     * This version of the method calls the matching no-arguments method
     * in the stream writer.
     */
    public SMOutputDocument createDocument()
        throws XMLStreamException
    {
        return new SMOutputDocument(this);
    }

    /**
     * Method used to create a StaxMate output fragment that corresponds
     * to a single well-formed XML document. Assumption, then, is that
     * the underlying stream writer has only been created, but no writes
     * have yet been done.
     *<p>
     * This version of the method calls the matching stream writer method
     * which takes full xml declaration information.
     */
    public SMOutputDocument createDocument(String version, String encoding)
        throws XMLStreamException
    {
        return new SMOutputDocument(this, version, encoding);
    }

    public SMOutputDocument createDocument(String version, String encoding,
                                           boolean standalone)
        throws XMLStreamException
    {
        return new SMOutputDocument(this, version, encoding, standalone);
    }
    
    /*
    //////////////////////////////////////////////////////
    // Factory methods, fragment creation
    //
    // These methods are used when only sub-trees are created
    // using StaxMate (although they can also be used to 
    // create buffered fragments; but usually that is simpler
    // to do via fragment object's factory methods)
    //////////////////////////////////////////////////////
    */

    /**
     * Method to use when outputting an XML sub-tree, in which case the
     * underlying stream writer may be (or has been) used for outputting
     * XML content in addition to content that is output using StaxMate.
     * Resulting fragment is not buffered, and will thus be fully
     * streamed (except for buffering caused by adding buffered children)
     */
    public SMRootFragment createRootFragment()
        throws XMLStreamException
    {
	return new SMRootFragment(this);
    }

    public SMBufferedFragment createBufferedFragment()
	throws XMLStreamException
    {
	return new SMBufferedFragment(this);
    }

    /*
    //////////////////////////////////////////////////////
    // Factory methods, simple node creation
    //////////////////////////////////////////////////////
    */

    public SMLinkedOutput createCharacters(String text) {
        return SMOCharacters.create(text);
    }

    public SMLinkedOutput createCharacters(char[] buf, int offset, int len) {
        return SMOCharacters.createShared(buf, offset, len);
    }

    /**
     * Specialized alternative to {link #createCharacters(char[],int,int)}
     * that can count on the passed char array NOT being shared. This means
     * that no intermediate copy needs to be done -- instance can just use
     * the passed in reference knowing it will not be messed by other threads.
     */
    public SMLinkedOutput createNonSharedCharacters(char[] buf, int offset, int len) {
        return SMOCharacters.createNonShared(buf, offset, len);
    }

    public SMLinkedOutput createCData(String text) {
        return SMOCData.create(text);
    }

    public SMLinkedOutput createCData(char[] buf, int offset, int len) {
        return SMOCData.createShared(buf, offset, len);
    }

    /**
     * Specialized alternative to {link #createCData(char[],int,int)}
     * that can count on the passed char array NOT being shared. This means
     * that no intermediate copy needs to be done -- instance can just use
     * the passed in reference knowing it will not be messed by other threads.
     */
    public SMLinkedOutput createNonSharedCData(char[] buf, int offset, int len) {
        return SMOCData.createNonShared(buf, offset, len);
    }

    public SMLinkedOutput createComment(String text) {
        return new SMOComment(text);
    }

    public SMLinkedOutput createEntityRef(String name) {
        return new SMOEntityRef(name);
    }

    public SMLinkedOutput createProcessingInstruction(String target, String data) {
        return new SMOProcInstr(target, data);
    }

    /*
    //////////////////////////////////////////////////////
    // Namespace handling
    //////////////////////////////////////////////////////
    */

    public final SMNamespace getNamespace(String uri)
    {
        if (uri == null || uri.length() == 0) {
            return sNsEmpty;
        }
        if (mLocalNsMap != null) {
            SMNamespace ns = (SMNamespace) mLocalNsMap.get(uri);
            if (ns != null) {
                return ns;
            }
        }
        SMNamespace ns = (SMNamespace) sGlobalNsMap.get(uri);
        if (ns == null) {
            ns = new SMLocalNamespace(this, uri, null);
            if (mLocalNsMap == null) {
                mLocalNsMap = new HashMap();
            }
            mLocalNsMap.put(uri, ns);
        }
        return ns;
    }

    public final SMNamespace getNamespace(String uri, String prefPrefix)
    {
        if (uri == null || uri.length() == 0) {
            return sNsEmpty;
        }
        if (mLocalNsMap != null) {
            SMNamespace ns = (SMNamespace) mLocalNsMap.get(uri);
            if (ns != null) {
                return ns;
            }
        }
        SMNamespace ns = (SMNamespace) sGlobalNsMap.get(uri);
        if (ns == null) {
            ns = new SMLocalNamespace(this, uri, prefPrefix);
            if (mLocalNsMap == null) {
                mLocalNsMap = new HashMap();
            }
            mLocalNsMap.put(uri, ns);
        }
        return ns;
    }

    public final static SMNamespace getEmptyNamespace()
    {
	return sNsEmpty;
    }

    /*
    //////////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////////
    */

    public final XMLStreamWriter getWriter() {
        return mStreamWriter;
    }
    
    public final boolean isWriterRepairing() {
        return mRepairing;
    }

    /*
    //////////////////////////////////////////////////////
    // Outputting of the actual content; done via context
    // so that overriding is possible
    //////////////////////////////////////////////////////
    */

    public void writeCharacters(String text)
        throws XMLStreamException
    {
        mStreamWriter.writeCharacters(text);
    }
    
    public void writeCharacters(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        mStreamWriter.writeCharacters(buf, offset, len);
    }
    
    public void writeCData(String text)
        throws XMLStreamException
    {
        mStreamWriter.writeCData(text);
    }
    
    public void writeCData(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        // Can we use StAX2?
        if (mStreamWriter instanceof XMLStreamWriter2) {
            ((XMLStreamWriter2) mStreamWriter).writeCData(buf, offset, len);
        } else {
            mStreamWriter.writeCData(new String(buf, offset, len));
        }
    }
    
    public void writeComment(String text)
        throws XMLStreamException
    {
        mStreamWriter.writeComment(text);
    }
    
    public void writeEntityRef(String name)
        throws XMLStreamException
    {
        mStreamWriter.writeEntityRef(name);
    }
    
    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException
    {
        if (data == null) {
            mStreamWriter.writeProcessingInstruction(target);
        } else {
            mStreamWriter.writeProcessingInstruction(target, data);
        }
    }

    public void writeAttribute(String localName, SMNamespace ns,
			       String value)
	throws XMLStreamException
    {
        // !!! TBI: Need to bind the namespace?
        
        // !!! TBI: actual outputting
    }

    public void writeStartElement(String localName, SMNamespace ns)
        throws XMLStreamException
    {
        // !!! TBI: Need to bind the namespace?
        
        // !!! TBI: actual outputting
    }
    
    public void writeEndElement(String localName, SMNamespace ns)
        throws XMLStreamException
    {
        // !!! TBI: actual outputting
        
        // !!! TBI: Need to unbind the namespaces?
    }
}
