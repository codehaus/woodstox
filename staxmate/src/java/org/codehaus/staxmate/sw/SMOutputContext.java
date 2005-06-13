package org.codehaus.staxmate.sw;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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

    /* Any documents really use more than 16 explicit namespaces?
     */
    final static int DEF_NS_STACK_SIZE = 16;

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
    // Configuration settings
    //////////////////////////////////////////////////////
    */

    final XMLStreamWriter mStreamWriter;
    final NamespaceContext mRootNsContext;
    final boolean mRepairing;

    /**
     * Prefix to use for creating automatic namespace prefixes. For example,
     * setting this to "ns" would result in automatic prefixes of form
     * "ns1", "ns2" and so on.
     */
    String mNsPrefixPrefix = "ns";

    int mNsPrefixSeqNr = 1;

    /**
     * Configuration flag that specifies whether by default namespaces
     * should bind as the default namespaces for elements or not. If true,
     * all unbound namespaces are always bound as the default namespace,
     * when elements are output: if false, more complicated logics is used
     * (which considers preferred prefixes, past bindings etc).
     */
    boolean mPreferDefaultNs = false;

    /*
    //////////////////////////////////////////////////////
    // State
    //////////////////////////////////////////////////////
    */

    /**
     * Map that contains all local namespaces, that is, namespaces
     * that have been created for use with documents output using
     * this context.
     */
    HashMap mLocalNsMap = null;
    
    /**
     * Currently active default namespace; one that is in effect within
     * current scope (inside currently open element, if any; if none,
     * within root level).
     */
    SMNamespace mDefaultNs = null;

    /**
     * Stack of bound non-default namespaces.
     */
    SMNamespace[] mNsStack = null;

    /**
     * Number of bound namespaces in {@link mNsStack}
     */
    int mBoundNsCount = 0;
    
    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
    */

    protected SMOutputContext(XMLStreamWriter sw, NamespaceContext rootNsCtxt)
    {
        mStreamWriter = sw;
        mRootNsContext = rootNsCtxt;
        Object o = sw.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        mRepairing = (o instanceof Boolean) && ((Boolean) o).booleanValue();
    }
    
    /*
    //////////////////////////////////////////////////////
    // Factory methods, context creation
    //////////////////////////////////////////////////////
    */

    public static SMOutputContext createInstance(XMLStreamWriter sw, NamespaceContext rootNsCtxt)
        throws XMLStreamException
    {
        return new SMOutputContext(sw, rootNsCtxt);
    }

    public static SMOutputContext createInstance(XMLStreamWriter sw)
        throws XMLStreamException
    {
        return createInstance(sw, sw.getNamespaceContext());
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
            ns = new SMLocalNamespace(this, uri, mPreferDefaultNs, null);
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
            ns = new SMLocalNamespace(this, uri, mPreferDefaultNs, prefPrefix);
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

    public void flushWriter()
        throws XMLStreamException
    {
        mStreamWriter.flush();
    }

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
        /* First things first: in repairing mode this is specifically
         * easy...
         */
        if (mRepairing) {
            // If no prefix preference, let's not pass one:
            String prefix = ns.getPreferredPrefix();
            if (prefix == null) {
                mStreamWriter.writeAttribute(ns.getURI(), localName, value);
            } else {
                mStreamWriter.writeAttribute(prefix,
                                             ns.getURI(), localName, value);
            }
            return;
        }

        // If not repairing, we need to handle bindings:

        /* No/empty namespace is simple for attributes, though; the
         * default namespace is never used...
         */
        if (ns == sNsEmpty) {
            mStreamWriter.writeAttribute(localName, value);
            return;
        }

        String prefix = ns.getBoundPrefix();
        if (prefix == null || prefix.length() == 0) {
            // First check: maybe it is still bound in the root context?
            prefix = findRootPrefix(ns);
            if (prefix != null) {
                // Yup. Need to mark it as permanently bound, then
                ns.bindPermanentlyAs(prefix);
            } else {
                // Ok. So which prefix should we bind (can't use def ns)?
                prefix = ns.getLastBoundPrefix();
                if (prefix == null) {
                    prefix = ns.getPreferredPrefix();
                }
                if (prefix == null || isPrefixBound(prefix)) {
                    prefix = generateUnboundPrefix();
                }
                // Ok, can bind now...
                ns.bindAs(prefix);
            }
        }

        mStreamWriter.writeAttribute(prefix, ns.getURI(), localName, value);
    }

    /**
     * Method called by the element object when it is about to get written
     * out. In this case, element will keep track of part of namespace
     * context information for this context object (to save allocation
     * of separate namespace context object).
     *
     * @return Namespace that was the active namespace in parent scope
     *   of this element. Will be different from the default namespace
     *   if a new default namespace was declared to be used by this
     *   element.
     */
    public SMNamespace writeStartElement(SMNamespace ns, String localName)
        throws XMLStreamException
    {
        /* In repairing mode we won't do binding,
         * nor keep track of them
         */
        if (!mRepairing) {
            String prefix = ns.getPreferredPrefix();
            // If no prefix preference, let's not pass one:
            if (prefix == null) {
                mStreamWriter.writeStartElement(ns.getURI(), localName);
            } else {
                mStreamWriter.writeStartElement(prefix, localName, ns.getURI());
            }
            return mDefaultNs;
        }

        SMNamespace oldDefaultNs = mDefaultNs;
        String prefix;

        // Namespace we need is either already the default namespace?
        if (ns == oldDefaultNs) { // ok, simple; already the default NS:
            prefix = "";
        } else {
            // Perhaps it's already bound to a specific prefix though?
            prefix = ns.getBoundPrefix();
            if (prefix == null) { // no such luck... need to bind
                // Ok, how about the root namespace context?
                prefix = findRootPrefix(ns);
                if (prefix != null) {
                    // Yup. Need to mark it as permanently bound, then
                    ns.bindPermanentlyAs(prefix);
                } else {
                    // Bind as the default namespace?
                    if (ns.prefersDefaultNs()) { // yes, please
                        mDefaultNs = ns;
                    } else { // well, let's see if we have used a prefix earlier
                        String newPrefix = ns.getLastBoundPrefix();
                        if (newPrefix != null && !isPrefixBound(newPrefix)) {
                            ns.bindAs(newPrefix);
                        } else { // nope... but perhaps we have a preference?
                            newPrefix = ns.getPreferredPrefix();
                            if (newPrefix != null && !isPrefixBound(newPrefix)) {
                                // Ok, cool let's just bind it then:
                                ns.bindAs(newPrefix);
                            } else {
                                // Nah, let's just bind as the default, then
                                mDefaultNs = ns;
                            }
                        }
                    }
                }
            }
        }
        
        mStreamWriter.writeStartElement(prefix, localName, ns.getURI());
        return oldDefaultNs;
    }
    
    public void writeEndElement(int parentNsCount, SMNamespace parentDefNs)
        throws XMLStreamException
    {
        mStreamWriter.writeEndElement();

        /* Ok, if we are not in repairing mode, may need to unbind namespace
         * bindings for namespaces bound with matching start element
         */
        if (!mRepairing) {
            if (mBoundNsCount > parentNsCount) {
                int i = mBoundNsCount;
                mBoundNsCount = parentNsCount;
                while (i-- > parentNsCount) {
                    SMNamespace ns = mNsStack[i];
                    mNsStack[i] = null;
                    ns.unbind();
                }
            }
        }

        mDefaultNs = parentDefNs;
    }

    public void writeStartDocument()
        throws XMLStreamException
    {
        mStreamWriter.writeStartDocument();
    }

    public void writeStartDocument(String version, String encoding)
        throws XMLStreamException
    {
        // note: Stax 1.0 has weird ordering for the args...
        mStreamWriter.writeStartDocument(encoding, version);
    }

    public void writeStartDocument(String version, String encoding,
                                   boolean standalone)
        throws XMLStreamException
    {
        XMLStreamWriter w = mStreamWriter;

        // Can we use StAX2?
        if (w instanceof XMLStreamWriter2) {
            ((XMLStreamWriter2) w).writeStartDocument(version, encoding, standalone);
        } else {
            // note: Stax 1.0 has weird ordering for the args...
            w.writeStartDocument(encoding, version);
        }
    }

    public void writeEndDocument()
        throws XMLStreamException
    {
        mStreamWriter.writeEndDocument();
        // And finally, let's indicate stream writer about closure too...
        mStreamWriter.close();
    }

    public void writeDoctypeDeclaration(String rootName,
                                        String systemId, String publicId,
                                        String intSubset)
        throws XMLStreamException
    {
        XMLStreamWriter w = mStreamWriter;
        if (w instanceof XMLStreamWriter2) {
            ((XMLStreamWriter2) w).writeDTD
                (rootName, systemId, publicId, intSubset);
        } else {
            // Damn this is ugly, with stax1.0...
            String dtd = "<!DOCTYPE "+rootName;
            if (publicId == null) {
                if (systemId != null) {
                    dtd += " SYSTEM";
                }
            } else {
                dtd += " PUBLIC '"+publicId+"'";
            }
            if (systemId != null) {
                dtd += " '"+systemId+"'";
            }
            if (intSubset != null) {
                dtd += " ["+intSubset+"]";
            }
            dtd += ">";
            w.writeDTD(dtd);
        }
    }

    /*
    //////////////////////////////////////////////////////
    // Other public utility methods
    //////////////////////////////////////////////////////
    */

    public String generateUnboundPrefix() {
        while (true) {
            String prefix = mNsPrefixPrefix + (mNsPrefixSeqNr++);
            if (!isPrefixBound(prefix)) {
                return prefix;
            }
        }
    }

    public boolean isPrefixBound(String prefix)
    {
        for (int i = mBoundNsCount; i >= 0; --i) {
            SMNamespace ns = mNsStack[i];
            if (prefix.equals(ns.getBoundPrefix())) {
                /* Note: StaxMate never creates masking bindings, so we
                 * know it's still active
                 */
                return true;
            }
        }
        /* So far so good. But perhaps it's bound in the root NamespaceContext?
         */
        if (mRootNsContext != null) {
            String uri = mRootNsContext.getNamespaceURI(prefix);
            if (uri != null && uri.length() > 0) {
                return true;
            }
        }
        return false;
    }

    public String findRootPrefix(SMNamespace ns)
    {
        if (mRootNsContext != null) {
            String uri = ns.getURI();
            String prefix = mRootNsContext.getPrefix(uri);
            /* Should seldom if ever get a match for the default NS; but
             * if we do, let's not take it.
             */
            if (prefix != null && prefix.length() > 0) {
                return prefix;
            }
        }
        return null;
    }

    /*
    //////////////////////////////////////////////////////
    // Package methods
    //////////////////////////////////////////////////////
    */

    /**
     * @return Number of bound non-default namespaces (ones with explicit
     *   prefix) currently
     */
    int getNamespaceCount() {
        return mBoundNsCount;
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
    */

    private void bindNs(SMNamespace ns)
    {
        SMNamespace[] stack = mNsStack;
        if (stack == null) {
            mNsStack = stack = new SMNamespace[DEF_NS_STACK_SIZE];
        } else if (mBoundNsCount >= stack.length) {
            mNsStack = new SMNamespace[stack.length * 2];
            System.arraycopy(stack, 0, mNsStack, 0, stack.length);
            stack = mNsStack;
        }
        stack[mBoundNsCount++] = ns;
    }
}
