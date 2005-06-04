package com.ctc.xpp;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Wrapper/adapter class that allows using a StAX {@link XMLStreamWriter}s
 * using XmlPull's {@link XmlSerializer} interface. StAX writers are
 * constructed using passed in factory ({@link XMLOutputFactory}) instances.
 *<p>
 * Notes about problems with mapping XmlPull on StAX:
 * <ul>
 *  <li>Arguments to START_DOCUMENT are slightly different (version vs.
 *    stand-alone attribute)
 *   </li>
 * </ul>
 *
 *
 * @author Tatu Saloranta
 */
public class XppSerializerOnStaxWriter
    implements XmlSerializer
{
    final static String PROPERTY_SERIALIZER_INDENTATION =
        "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";
    final static String PROPERTY_SERIALIZER_LINE_SEPARATOR =
        "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator";
    final static String FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE =
        "http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe";

    final static String DEFAULT_XML_VERSION = "1.0";

    /**
     * Factory used to instantiate StAX writer when needed.
     */
    final XMLOutputFactory mStaxFactory;

    XMLStreamWriter mStaxWriter;

    /*
    ///////////////////////////////////////////
    // Configuration data:
    ///////////////////////////////////////////
     */

    protected OutputStream mOutputStream = null;

    protected Writer mOutputWriter = null;

    protected String mOutputEncoding = null;

    /*
    ///////////////////////////////////////////
    // Output state:
    ///////////////////////////////////////////
     */

    /**
     * Stack that contains ns/prefix pairs for currently open elements.
     */
    protected ArrayList mElementNames;

    /**
     * List of namespace declarations to add to next start element.
     */
    protected ArrayList mNsMappings;

    /*
    ///////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////
     */

    public XppSerializerOnStaxWriter(XMLOutputFactory staxFactory)
    {
        mStaxFactory = staxFactory;
    }

    public XppSerializerOnStaxWriter()
    {
        this(XMLOutputFactory.newInstance());
    }
    
    protected void resetOutput()
    {
        if (mStaxWriter != null) {
            XMLStreamWriter sw = mStaxWriter;
            mStaxWriter = null;
            try {
                sw.close();
            } catch (XMLStreamException wex) { // should never happen...
                throw new RuntimeException(wex.toString());
            }
        }
        mOutputStream = null;
        mOutputWriter = null;
        mOutputEncoding = null;
    }

    protected void initForWriting() 
        throws IOException
    {
        XMLStreamWriter sw = null;

        mElementNames = new ArrayList();
        mNsMappings = null;

        try {
            if (mOutputWriter != null) {
                sw = mStaxFactory.createXMLStreamWriter(mOutputWriter);
            } else if (mOutputStream != null) {
                if (mOutputEncoding == null) {
                    sw = mStaxFactory.createXMLStreamWriter(mOutputStream);
                } else {
                    sw = mStaxFactory.createXMLStreamWriter(mOutputStream, mOutputEncoding);
                }
            } else {
                throw new IllegalStateException("No output stream or writer specified for the serializer.");
            }
        } catch (XMLStreamException wex) {
            throw new IOException(wex.toString());
        }
        mStaxWriter = sw;
    }

    /*
    ///////////////////////////////////////////
    // XmlSerializer API
    ///////////////////////////////////////////
     */

    public XmlSerializer attribute(String ns, String name, String value)
        throws IOException
    {
        if (mStaxWriter == null) {
            throwUnitialized("attribute");
        }
        try {
            mStaxWriter.writeAttribute(ns, name, value);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
        return this;
    }

    public void cdsect(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.writeCData(text);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public void comment(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.writeComment(text);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public void docdecl(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        /* 21-Jul-2004, TSa: Xmlpull seems to leave "<!DOCTYPE" part out...
         *    whereas StAX requires it (which it probably shouldn't but
         *    that's another discussion)
         */
        try {
            final String preamble = "<!DOCTYPE";
            if (!text.startsWith(preamble)) {
                StringBuffer sb = new StringBuffer(text.length() + 16);
                sb.append(preamble);
                sb.append(' ');
                sb.append(text);
                sb.append(">");
                text = sb.toString();
            }
            mStaxWriter.writeDTD(text);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    /**
     *<p>
     * Note: Whereas StAX requier
     */
    public void endDocument()
        throws IOException
    {
        if (mStaxWriter == null) {
            throwUnitialized("endDocument");
        }

        mElementNames.clear();
        try {
            mStaxWriter.writeEndDocument();
            mStaxWriter.flush();
            mStaxWriter.close();
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public XmlSerializer endTag(String namespace, String name)
        throws IOException
    {
        if (mStaxWriter == null) {
            throwUnitialized("endTag");
        }

        int size = mElementNames.size();
        mElementNames.remove(size-1);
        mElementNames.remove(size-2);

        try {
            mStaxWriter.writeEndElement();
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }

        return this;
    }

    public void entityRef(String refId)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.writeEntityRef(refId);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public void flush()
        throws IOException
    {
        if (mStaxWriter == null) {
            throwUnitialized("flush");
        }
        try {
            mStaxWriter.flush();
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public int getDepth()
    {
        return (mElementNames == null) ? 0 : (mElementNames.size() >> 1);
    }

    public boolean getFeature(String name)
    {
        // !!! TBI
        return false;
    }

    public String getName()
    {
        if (mElementNames == null) {
            return null;
        }
        int size = mElementNames.size();
        return (size < 2) ? null : (String) mElementNames.get(size-1);
    }

    public String getNamespace()
    {
        if (mElementNames == null) {
            return null;
        }
        int size = mElementNames.size();
        return (size < 2) ? null : (String) mElementNames.get(size-2);
    }

    public String getPrefix(String ns, boolean generatePrefix)
    {
        if (mStaxWriter == null) {
            throwUnitialized("getPrefix");
        }
        /* 18-Jul-2004, TSa: Prefix may or may not be generated, depending
         *  on settings of the StAX factory that created the writer: no way
         *  to dynamically to request such generation.
         */
        try {
            return mStaxWriter.getPrefix(ns);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
            return null; // never gets here
        }
    }

    public Object getProperty(String name)
    {
        // !!! TBI
        return null;
    }

    public void ignorableWhitespace(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            /* No specific method with StAX, as whether it's ignorable depends
             * on context, DTD etc.
             */
            mStaxWriter.writeCharacters(text);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public void processingInstruction(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            /* 18-Jul-2004, TSa: StAX requires it to be properly split;
             *   quick hack: let's split on white space, if any:
             */
            int i = 1;
            int len = text.length();

            while (i < len && text.charAt(i) > 0x0020) {
                ++i;
            }
            if (i < len) {
                mStaxWriter.writeProcessingInstruction(text.substring(0, i),
                                                       text.substring(i+1));
            } else {
                mStaxWriter.writeProcessingInstruction(text);
            }
            
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public void setFeature(String name, boolean state)
    {
        throw new IllegalStateException("Feature '"+name+"' not supported by this Xmlpull serializer implementation.");
    }

    public void setOutput(OutputStream os, String encoding)
        throws IOException
    {
        // Is null legal or not?
        if (encoding == null) {
            throw new IllegalArgumentException("Trying to set null encoding");
        }

        resetOutput();
        mOutputStream = os;
        mOutputEncoding = encoding;
    }

    public void setOutput(Writer writer)
        throws IOException
    {
        resetOutput();
        mOutputWriter = writer;
    }

    public void setPrefix(String prefix, String ns)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.setPrefix(prefix, ns);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
        if (mNsMappings == null) {
            mNsMappings = new ArrayList();
        }
        mNsMappings.add(prefix);
        mNsMappings.add(ns);
    }

    public void setProperty(String name, Object value)
    {
        throw new IllegalStateException("Property '"+name+"' not supported by this Xmlpull serializer implementation.");
    }

    public void startDocument(String encoding, Boolean standalone)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            /* !!! 18-Jul-2004, TSa: No way to pass standalone setting...
             *   plus, have to default some version like 1.0?
             */
            if (mOutputEncoding == null || mOutputEncoding.length() == 0) {
                mStaxWriter.writeStartDocument();
            } else {
                mStaxWriter.writeStartDocument(mOutputEncoding, DEFAULT_XML_VERSION);
            }
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }
    }

    public XmlSerializer startTag(String ns, String name)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        mElementNames.add(ns);
        mElementNames.add(name);

        try {
            // Let's first clear prefixes, just in case
            ArrayList nslist = mNsMappings;
            mNsMappings = null;

            mStaxWriter.writeStartElement(ns, name);

            /* XmlPull doesn't require more than just declaring prefixes;
             * StAX requires both declaration and output... so here:
             */
            if (nslist != null && nslist.size() > 0) {
                for (int i = 0, len = nslist.size(); i < len; i += 2) {
                    String prefix = (String) nslist.get(i);
                    mStaxWriter.writeNamespace(prefix, (String) nslist.get(i+1));
                }
            }
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }

        return this;
    }

    public XmlSerializer text(char[] buf, int start, int len)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.writeCharacters(buf, start, len);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }

        return this;
    }

    public XmlSerializer text(String text)
        throws IOException
    {
        if (mStaxWriter == null) {
            initForWriting();
        }

        try {
            mStaxWriter.writeCharacters(text);
        } catch (XMLStreamException sx) {
            throwFromXSEx(sx);
        }

        return this;
    }

    /*
    ///////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////
     */

    protected void throwUnitialized(String method)
    {
        throw new IllegalStateException("Can not call '"+method+"()': serializer not correctly set up (need to call startDocument() or startTag() first).");
    }

    protected void throwFromXSEx(XMLStreamException strex)
        throws IllegalStateException
    {
        throw new IllegalStateException(strex.toString());
    }
}
