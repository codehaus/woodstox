package com.ctc.wstx.sw;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

// unfortunate dependencies to StAX events:
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.exc.*;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Base class for {@link XMLStreamWriter} implementations Woodstox has.
 * Contains partial stream writer implementation, plus utility methods
 * shared by concrete implementation classes. Main reason for such
 * abstract base class is to allow other parts of Woodstox core to refer
 * to any of stream writer implementations in general way.
 */
public abstract class BaseStreamWriter
    implements XMLStreamWriter,
               OutputConfigFlags
{
    protected final static int STATE_PROLOG = 1;
    protected final static int STATE_TREE = 2;
    protected final static int STATE_EPILOG = 3;

    protected final static char CHAR_SPACE = ' ';

    /*
    ////////////////////////////////////////////////////
    // Output objects
    ////////////////////////////////////////////////////
     */

    /**
     * Actual physical writer to output serialized XML content to
     */
    protected final Writer mWriter;
    
    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Specialized configuration flags, extracted from config flags:

    protected final boolean mCfgOutputEmptyElems;

    protected final boolean mCheckStructure;
    protected final boolean mCheckContent;
    protected final boolean mCheckAttr;

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    protected int mState = STATE_PROLOG;

    /**
     * Flag that is set to true first time something has been output.
     * Generally needed to keep track of whether XML declaration
     * (START_DOCUMENT) can be output or not.
     */
    protected boolean mAnyOutput = false;

    /**
     * Flag that is set during time that a start element is "open", ie.
     * START_ELEMENT has been output (and possibly zero or more name
     * space declarations and attributes), before other main-level
     * constructs have been output.
     */
    protected boolean mStartElementOpen = false;

    /**
     * Flag that indicates that current element is an empty element.
     * This is needed to know what to do when next non-ns/attr node
     * is output; normally a new context is opened, but for empty
     * elements not.
     */
    protected boolean mEmptyElement = false;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    protected BaseStreamWriter(Writer w, WriterConfig cfg)
    {
        mWriter = w;

        int flags = cfg.getConfigFlags();
        mCheckStructure = (flags & CFG_VALIDATE_STRUCTURE) != 0;
        mCheckContent = (flags & CFG_VALIDATE_CONTENT) != 0;
        mCheckAttr = (flags & CFG_VALIDATE_ATTR) != 0;

        mCfgOutputEmptyElems = (flags & CFG_OUTPUT_EMPTY_ELEMS) != 0;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    public void close()
        throws XMLStreamException
    {
        /* 19-Jul-2004, TSa: Hmmh. Let's actually close all still open
         *    elements, starting with currently open start (-> empty)
         *    element, if one exists, and then closing scopes by adding
         *    matching end elements.
         */
        if (mState != STATE_EPILOG) {
            writeEndDocument();
        }
        flush();
    }

    public void flush()
        throws XMLStreamException
    {
        try {
            mWriter.flush();
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public abstract NamespaceContext getNamespaceContext();

    public abstract String getPrefix(String uri);

    public Object getProperty(String name) {
        // No properties yet?
        return null;
    }

    public abstract void setDefaultNamespace(String uri)
        throws XMLStreamException;

    public abstract void setNamespaceContext(NamespaceContext context);

    public abstract void setPrefix(String prefix, String uri)
        throws XMLStreamException;

    public abstract void writeAttribute(String localName, String value)
        throws XMLStreamException;
    
    public abstract void writeAttribute(String nsURI, String localName,
                                        String value)
        throws XMLStreamException;

    public abstract void writeAttribute(String prefix, String nsURI,
                                        String localName, String value)
        throws XMLStreamException;

    public void writeCData(String data)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        // Not legal outside main element tree:
        if (mCheckStructure) {
            if (inPrologOrEpilog()) {
                throw new IllegalStateException("Trying to output a CDATA block outside main element tree (in prolog or epilog)");
            }
        }

        if (mCheckContent) {
            if (data != null && data.length() > 1) {
                int ix = data.indexOf(']');
                if (ix >= 0) {
                    ix = data.indexOf("]]>", ix);
                    if (ix >= 0) {
                        throw new XMLStreamException("Illegal input: CDATA block has embedded ']]>' in it (index "+ix+")");
                    }
                }
            }
        }
 
        try {
            mWriter.write("<![CDATA[");
            if (data != null) {
                mWriter.write(data);
            }
            mWriter.write("]]>");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeCharacters(char[] text, int start, int len)
        throws XMLStreamException
    {
        /* Not legal outside main element tree, except if it's all
         * white space
         */
        if (mCheckStructure) {
            if (inPrologOrEpilog()) {
                if (!XMLQuoter.isAllWhitespace(text, start, len)) {
                    throw new IllegalStateException("Trying to output non-whitespace characters outside main element tree (in prolog or epilog)");
                }
            }
        }

        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        try {
            XMLQuoter.outputXMLText(mWriter, text, start, len);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeCharacters(String text)
        throws XMLStreamException
    {
        // Need to validate structure?
        if (mCheckStructure) {
            // Not valid in prolog/epilog, except if it's all white space:
            if (inPrologOrEpilog()) {
                if (!XMLQuoter.isAllWhitespace(text)) {
                    throw new IllegalStateException("Trying to output non-whitespace characters outside main element tree (in prolog or epilog)");
                }
            }
        }

        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        // Ok, let's just write it out:
        try {
            XMLQuoter.outputXMLText(mWriter, text);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeComment(String data)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        /* No structural validation needed per se, for comments; they are
         * allowed anywhere in XML content. However, content may need to
         * be checked, to see it has no embedded '--'s.
         */
        if (mCheckContent) {
            int ix = data.indexOf('-');
            if (ix >= 0) {
                ix = data.indexOf("--", ix);
                if (ix >= 0) {
                    throw new XMLStreamException("Illegal input: comment content has embedded '--' in it (index "+ix+")");
                }
            }
        }

        try {
            mWriter.write("<!--");
            XMLQuoter.outputXMLText(mWriter, data);
            mWriter.write("-->");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public abstract void writeDefaultNamespace(String nsURI)
        throws XMLStreamException;

    public void writeDTD(String dtd)
        throws XMLStreamException
    {
        // Are there any validations that could be done here?

        try {
            mWriter.write(dtd);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public abstract void writeEmptyElement(String localName)
        throws XMLStreamException;

    public abstract void writeEmptyElement(String nsURI, String localName)
        throws XMLStreamException;

    public abstract void writeEmptyElement(String prefix, String localName, String nsURI)
        throws XMLStreamException;

    public void writeEndDocument() throws XMLStreamException
    {
        // Is tree still open?
        if (mState != STATE_EPILOG) {
            if (mCheckStructure  && mState == STATE_PROLOG) {
                throw new IllegalStateException("Trying to write END_DOCUMENT when document has no root (ie. trying to output empty document).");
            }
            // 20-Jul-2004, TSa: Need to close the open sub-tree, if it exists...
            // First, do we have an open start element?
            if (mStartElementOpen) {
                closeStartElement(mEmptyElement);
            }
            // Then, one by one, need to close open scopes:
            while (mState != STATE_EPILOG) {
                writeEndElement();
            }
        }

        // Ok, fine, there's nothing specific to output...
    }

    public abstract void writeEndElement() throws XMLStreamException;

    public void writeEntityRef(String name)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        // Structurally, need to check we are not in prolog/epilog.
        if (mCheckStructure) {
            if (inPrologOrEpilog()) {
                throw new IllegalStateException("Trying to output an entity reference outside main element tree (in prolog or epilog)");
            }
        }
        if (mCheckContent) {
            checkNameValidity(name, true);
        }
        
        try {
            mWriter.write('&');
            mWriter.write(name);
            mWriter.write(';');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public abstract void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException;

    public void writeProcessingInstruction(String target)
        throws XMLStreamException
    {
        writeProcessingInstruction(target, null);
    }

    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        // Structurally, PIs are always ok. But content may need to be checked.
        if (mCheckContent) {
            checkNameValidity(target, true);
            if (data != null && data.length() > 1) {
                int ix = data.indexOf('?');
                if (ix >= 0) {
                    ix = data.indexOf("?>", ix);
                    if (ix >= 0) {
                        throw new XMLStreamException("Illegal input: processing instruction content has embedded '?>' in it (index "+ix+")");
                    }
                }
            }
        }

        try {
            mWriter.write("<?");
            mWriter.write(target);
            if (data != null && data.length() > 0) {
                /* 11-Nov-2004, TSa: Let's see if it starts with a space:
                 *  if so, no need to add extra space(s).
                 */
                if (data.charAt(0) > CHAR_SPACE) {
                    mWriter.write(' ');
                }
                mWriter.write(data);
            }
            mWriter.write("?>");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeStartDocument()
        throws XMLStreamException
    {
        writeStartDocument(null, null);
    }

    public void writeStartDocument(String version)
        throws XMLStreamException
    {
        writeStartDocument(null, version);
    }

    public void writeStartDocument(String encoding, String version)
        throws XMLStreamException
    {
        /* Not legal to output XML declaration if there has been ANY
         * output prior... that is, if we validate the structure.
         */
        if (mCheckStructure) {
            if (mAnyOutput) {
                throw new IllegalStateException("Can not output XML declaration, after other output has already been done.");
            }
        }

        mAnyOutput = true;

        if (mCheckContent) {
            // !!! 06-May-2004, TSa: Should validate version and encoding?
            if (encoding != null) {
            }
            if (version != null) {
            }
        }

        try {
            mWriter.write("<?xml version=\"");
            mWriter.write((version == null || version.length() == 0)
                          ? WstxOutputProperties.DEFAULT_XML_VERSION : version);
            mWriter.write('"');

            if (encoding != null && encoding.length() > 0) {
                mWriter.write(" encoding=\"");
                mWriter.write(encoding);
                mWriter.write('"');
            }
            mWriter.write(" ?>");
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public abstract void writeStartElement(String localName)
        throws XMLStreamException;

    public abstract void writeStartElement(String nsURI, String localName)
        throws XMLStreamException;

    public abstract void writeStartElement(String prefix, String localName,
                                           String nsURI)
        throws XMLStreamException;
    
    /*
    ////////////////////////////////////////////////////
    // Package methods (ie not part of public API)
    ////////////////////////////////////////////////////
     */

    /**
     * Method needed by {@link com.ctc.wstx.evt.WstxEventWriter}, when it needs/wants to
     * do direct output, without calling methods in this class (not often).
     */
    public Writer getWriter() {
        return mWriter;
    }

    /**
     * Convenience method needed by {@link com.ctc.wstx.evt.WstxEventWriter}, to use when
     * writing a start element, and possibly its attributes and namespace
     * declarations.
     */
    public abstract void writeStartElement(StartElement elem)
        throws XMLStreamException;

    /**
     * Method called by {@link com.ctc.wstx.evt.WstxEventWriter} (instead of the version
     * that takes no argument), so that we can verify it does match the
     * start element, if necessary
     */
    public abstract void writeEndElement(QName name)
        throws XMLStreamException;

    /**
     * Method called by {@link com.ctc.wstx.evt.WstxEventWriter} (instead of more generic
     * text output methods), so that we can verify (if necessary) that
     * this character output type is legal in this context. Specifically,
     * it's not acceptable to add non-whitespace content outside root
     * element (in prolog/epilog).
     */

    public void writeCharacters(Characters ch)
        throws XMLStreamException
    {
        /* Not legal outside main element tree, except if it's all
         * white space
         */
        if (mCheckStructure) {
            if (inPrologOrEpilog()) {
                if (!ch.isIgnorableWhiteSpace() && !ch.isWhiteSpace()) {
                    throw new IllegalStateException("Trying to output non-whitespace characters outside main element tree (in prolog or epilog)");
                }
            }
        }

        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }

        // Ok, let's just write it out:
        try {
            XMLQuoter.outputXMLText(mWriter, ch.getData());
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or attribute)
     * is being output; except for end element which is handled differently.
     */
    protected abstract void closeStartElement(boolean emptyElem)
        throws XMLStreamException;

    public boolean inPrologOrEpilog() {
        return (mState != STATE_TREE);
    }

    /**
     * Method called to verify that the name is a legal XML name.
     */
    public void checkNameValidity(String name, boolean allowColons)
    {
        // !!! TBI

        if (!allowColons && name.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Illegal name token '"+name+"'; colons not allowed inside names, only a single colon allowed to indicate fully-qualified name.");
        }

        // Needs to throw appropriate IllegalArgumentException for invalid names
    }

    public abstract String getTopElemName();

    /*
    ////////////////////////////////////////////////////
    // Package methods, logging, exception handling
    ////////////////////////////////////////////////////
     */

    protected void throwOutputError(String msg)
        throws XMLStreamException
    {
        throw new XMLStreamException(msg);
    }

    protected void throwOutputError(String format, Object arg)
        throws XMLStreamException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        throw new XMLStreamException(msg);
    }

    protected void throwFromIOE(IOException ioe)
        throws XMLStreamException
    {
        throw new WstxIOException(ioe);
    }
}
