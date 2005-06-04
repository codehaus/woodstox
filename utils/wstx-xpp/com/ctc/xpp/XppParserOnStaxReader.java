package com.ctc.xpp;

import java.io.*;
import java.util.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Wrapper/adapter class that allows using a StAX {@link XMLStreamReader}s using
 * XmlPull's {@link XmlPullParser} interface. StAX readers are constructed
 * using passed in factory ({@link XMLInputFactory}) instances.
 *<p>
 * Notes about problems with mapping XmlPull on StAX:
 * <ul>
 *  <li>Xmlpull expects fairly sophisticated event merging with all of
 *    nextXxx methods, except for nextToken(): StAX does not mandate such
 *    coalescing, and it's rather tricky to implement on top of StAX, except
 *    by maybe peek()ing next event... but then it'd have to use event reader.
 *    Doesn't seem to be worth the hassle?
 *    A related problem is Xmlpull requirement to report unexpanded entities
 *    with certain settings: this is similarly hard to do when otherwise
 *    mergings need to be done.
 *   </li>
 *  <li>Namespace context and scoping rules are different. XPP allows access
 *     at any point; StAX only gives access while START/END_ELEMENT events
 *     are current events. As a result, wrapper only gives access when
 *     START/END_TAG events are at top of the stack.
 *   </li>
 *  <li>StAX does not have a (standard) feature for manually defined entities;
 *    as a result, wrapper does not make use of them. Note that SOME StAX
 *    implementations (like Woodstox) do support the concept, so it would
 *    be possible to extend this wrapper for specific implementations?
 *   </li>
 *  <li>StAX does not have a (standard) feature that would allow reporting
 *    namespace declarations as attributes; except by disabled namespace
 *    support altogether. Individual implementations could define such
 *    support, if it seems useful... but generic wrapper can't easily support
 *    it.
 *   </li>
 *  <li>StAX does not have a (standard) feature that would allow checking
 *     whether a start element is an empty element.
 *   </li>
 * </ul>
 *
 * @author Tatu Saloranta
 */
public class XppParserOnStaxReader
    implements XmlPullParser
{
    final char CHAR_SPACE = ' ';

    /*
    ////////////////////////////////////////////////////
    // Properties (optional) XmlPull parsers can implement
    ////////////////////////////////////////////////////
     */

    // // // Accessing DOCTYPE declaration information:

    final static String XPP_XMLDECL_VERSION = 
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    final static String XPP_XMLDECL_STANDALONE = 
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";
    final static String XPP_XMLDECL_CONTENT = 
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-content";

    // // // Accesing location


    final static String XPP_LOCATION = 
        "http://xmlpull.org/v1/doc/properties.html#location";

    /*
    ////////////////////////////////////////////////////
    // StAX objects needed to implement xmlpull API
    ////////////////////////////////////////////////////
     */

    /**
     * Factory used to instantiate StAX readers when needed.
     */
    final XMLInputFactory mStaxFactory;

    XMLStreamReader mStaxReader;

    /*
    ///////////////////////////////////////////
    // Configuration data:
    ///////////////////////////////////////////
     */

    InputStream mInputStream = null;

    Reader mInputReader = null;

    String mInputEncoding = null;

    Map mCustomEntities = null;

    /*
    ///////////////////////////////////////////
    // Parsing state:
    ///////////////////////////////////////////
     */

    int mCurrEvent = START_DOCUMENT;

    /**
     *<p>
     * Note: needs to be cleared by read methods whenever reading in a
     * new event; can then be lazy-loaded as needed and reused until
     * next event.
     */
    Location mCurrLocation;

    String mLocationDesc = null;

    /**
     * Depth of element nesting.
     */
    int mDepth = 0;

    /**
     *<p>
     * Note: needs to be updated by read methods, to be valid when current
     * event is START_TAG.
     */
    int mAttrCount = 0;

    /**
     *<p>
     * Note: needs to be updated by read methods when handling START_TAG
     * and END_TAG events.
     */
    int mNsCount = 0;

    /*
    ///////////////////////////////////////////
    // String recombination...
    ///////////////////////////////////////////
     */

    /**
     * Unfortunately, we do need to sometimes store specifically combined
     * text String. This 
     */
    String mCurrText;

    char[] mCurrTextBuffer;

    /*
    ///////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////
     */

    public XppParserOnStaxReader(boolean nsAware, boolean dtdSupport, boolean validating,
                                 XMLInputFactory staxFactory)
    {
        mStaxFactory = staxFactory;

        /* Need to make sure defaults are XPP defaults, not StAX defaults:
         */
        if (staxFactory.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
            mStaxFactory.setProperty(XMLInputFactory.SUPPORT_DTD,
                                     dtdSupport ? Boolean.TRUE : Boolean.FALSE);
        }

        setNamespaceAware(staxFactory, nsAware);
        setValidating(staxFactory, validating);

        /* 16-Jul-2004, TSa: Additionally let's enable text coalescing: this
         *   is the only easy way to ensure that next() method can be made
         *   to coalesce text... Not nice, but should work.
         */
        mStaxFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    }

    public XppParserOnStaxReader(boolean nsAware, boolean dtdSupport, boolean validating)
    {
        this(nsAware, dtdSupport, validating,
             XMLInputFactory.newInstance());
    }

    protected void resetInput()
    {
        if (mStaxReader != null) {
            XMLStreamReader sr = mStaxReader;
            mStaxReader = null;
            try {
                sr.close();
            } catch (XMLStreamException wex) { // should never happen...
                throw new RuntimeException(wex.toString());
            }
        }
        mInputStream = null;
        mInputReader = null;
        mInputEncoding = null;
        mCustomEntities = null;
        mCurrEvent = START_DOCUMENT;
        mDepth = 0;
    }

    /**
     * Method called to create and initialize StAX reader.
     */
    protected void initForReading()
        throws IOException, XmlPullParserException
    {
        mAttrCount = 0;
        mNsCount = 0;

        XMLStreamReader sr = null;
        try {
            if (mInputReader != null) {
                sr = mStaxFactory.createXMLStreamReader(mInputReader);
            } else if (mInputStream != null) {
                if (mInputEncoding == null) {
                    sr = mStaxFactory.createXMLStreamReader(mInputStream);
                } else {
                    sr = mStaxFactory.createXMLStreamReader(mInputStream, mInputEncoding);
                }
            } else {
                /* According to conformance tests, needs to throw
                 * XmlPullParserException, not any other type
                 */
                throw new XmlPullParserException("No input stream or reader specified for the parser.", this, null);
            }
        } catch (XMLStreamException wex) {
            throw new XmlPullParserException(wex.toString(), this, wex);
        }
        mStaxReader = sr;
    }

    /*
    ////////////////////////////////////////////////////
    // XmlPullParser implementation
    ////////////////////////////////////////////////////
     */

    public void defineEntityReplacementText(String entityName, String replacementText)
    {
        if (mStaxReader != null) {
            throwIllegalConfigState();
        }
        if (mCustomEntities == null) {
            mCustomEntities = new HashMap();
        }
        mCustomEntities.put(entityName, replacementText);
    }

    public int getAttributeCount()
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            return -1;
        }
        return mStaxReader.getAttributeCount();
    }

    public String getAttributeName(int index)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributeName", index, -1);
        }
        if (index < 0 || index >= mAttrCount) {
            throwIndexOutOfBounds("getAttributeName", index, -1);
        }
        return mStaxReader.getAttributeLocalName(index);
    }

    public String getAttributeNamespace(int index)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributeNamespace", index, -1);
        }
        if (index < 0 || index >= mAttrCount) {
            throwIndexOutOfBounds("getAttributeNamespace", index, -1);
        }
        return mStaxReader.getAttributeNamespace(index);
    }

    public String getAttributePrefix(int index)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributePrefix", index, -1);
        }
        if (index < 0 || index >= mAttrCount) {
            throwIndexOutOfBounds("getAttributePrefix", index, -1);
        }
        /* Hmmh. Xmlpull requires returning NULL for default namespace
         * prefix... and depending on reading of specs, StAX implementations
         * might return empty String instead?
         */
        String prefix = mStaxReader.getAttributePrefix(index);
        return (prefix == null || prefix.length() > 0) ?
            prefix : null;
    }

    public String getAttributeType(int index)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributeType", index, -1);
        }
        if (index < 0 || index >= mAttrCount) {
            throwIndexOutOfBounds("getAttributeType", index, -1);
        }
        return mStaxReader.getAttributeType(index);
    }

    public String getAttributeValue(int index)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributeValue", index, -1);
        }
        if (index < 0 || index >= mAttrCount) {
            throwIndexOutOfBounds("getAttributeValue", index, -1);
        }
        return mStaxReader.getAttributeValue(index);
    }

    public String getAttributeValue(String ns, String localName)
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            throwIndexOutOfBounds("getAttributeValue", 0, -1);
        }
        return mStaxReader.getAttributeValue(ns, localName);
    }

    public int getColumnNumber()
    {
        Location loc = getLocation();
        return (loc == null) ? -1 : mCurrLocation.getColumnNumber();
    }

    public int getDepth()
    {
        return mDepth;
    }

    public int getEventType()
    {
        return mCurrEvent;
    }

    public boolean getFeature(String name)
    {
        Object o = null;

        if (name.equals(FEATURE_PROCESS_DOCDECL)) {
            o = mStaxFactory.getProperty(XMLInputFactory.SUPPORT_DTD);
        } else if (name.equals(FEATURE_PROCESS_NAMESPACES)) {
            o = mStaxFactory.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE);
        } else if (name.equals(FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            // !!! TBI
        } else if (name.equals(FEATURE_VALIDATION)) {
            o = mStaxFactory.getProperty(XMLInputFactory.IS_VALIDATING);
        }
        return (o == null || !(o instanceof Boolean)) ?
            false : ((Boolean) o).booleanValue();
    }

    public String getInputEncoding()
    {
        return mInputEncoding;
    }

    public int getLineNumber()
    {
        Location loc = getLocation();
        return (loc == null) ? -1 : mCurrLocation.getLineNumber();
    }

    public String getName()
    {
        return (mStaxReader == null) ? null : mStaxReader.getLocalName();
    }

    public String getNamespace()
    {
        if (mStaxReader == null) {
            return null;
        }
        if (mCurrEvent == START_TAG || mCurrEvent == END_TAG) {
            return mStaxReader.getNamespaceURI();
        }
        return null;
    }

    public String getNamespace(String prefix)
    {
        if (mStaxReader == null) {
            return NO_NAMESPACE;
        }
        // StAX expects empty string, XmlPull allows using null...
        if (prefix == null) {
            prefix = "";
        }
        if (mCurrEvent == START_TAG || mCurrEvent == END_TAG) {
            return mStaxReader.getNamespaceURI(prefix);
        }
        NamespaceContext nsCtxt = mStaxReader.getNamespaceContext();
        return (nsCtxt == null) ? NO_NAMESPACE : nsCtxt.getNamespaceURI(prefix);
    }

    public int getNamespaceCount(int depth)
    {
        /* 16-Jul-2004, TSa: StAX does not have similar concept of allow
         *    access to nested namespace scoping... so let's just fake
         *    and only return count for current scope.
         */
        if (mStaxReader == null || depth != mDepth) {
            return 0;
        }
        return mNsCount;
    }

    public String getNamespacePrefix(int pos)
    {
        if (mStaxReader == null ||
            (mCurrEvent != START_TAG && mCurrEvent != END_TAG)) {
            return null;
        }
        return mStaxReader.getNamespacePrefix(pos);
    }

    public String getNamespaceUri(int pos)
    {
        if (mStaxReader == null ||
            (mCurrEvent != START_TAG && mCurrEvent != END_TAG)) {
            return null;
        }
        return mStaxReader.getNamespaceURI(pos);
    }

    public String getPositionDescription()
    {
        return (mCurrLocation == null) ? "[unknown]" : mCurrLocation.toString();
    }

    public String getPrefix()
    {
        // StAX returns null if not valid event...
        if (mStaxReader == null) {
            return null;
        }
        String prefix = mStaxReader.getPrefix();
        /* Hmmh. Xmlpull requires returning NULL for default namespace
         * prefix... and depending on reading of specs, StAX implementations
         * might return empty String instead?
         */
        return (prefix == null || prefix.length() > 0) ?
            prefix : null;
    }

    public Object getProperty(String name)
    {
        if (name.equals(XPP_XMLDECL_VERSION)) {
            if (mStaxReader != null) {
                return mStaxReader.getVersion();
            }
        } else if (name.equals(XPP_XMLDECL_STANDALONE)) {
            if (mStaxReader != null) {
                if (mStaxReader.standaloneSet()) {
                    return mStaxReader.isStandalone()
                        ? Boolean.TRUE : Boolean.FALSE;
                }
            }
        } else if (name.equals(XPP_LOCATION)) {
            if (mLocationDesc != null) {
                return mLocationDesc;
            }
            Location loc = getLocation();
            return (loc == null) ? null : loc.toString();
        }

        return null;
    }

    public String getText()
    {
        if (mStaxReader == null) {
            return null;
        }
        if (mCurrText != null) {
            return mCurrText;
        }

        String text = null;

        if (mCurrTextBuffer != null) {
            text = new String(mCurrTextBuffer);
        } else {
            switch (mCurrEvent) {
            case CDSECT:
            case COMMENT:
            case ENTITY_REF:
            case IGNORABLE_WHITESPACE:
            case TEXT:
                // default should be fine:
                text = mStaxReader.getText();
                break;

            case PROCESSING_INSTRUCTION:
                // hmmmmmh.
                {
                    String target = mStaxReader.getPITarget();
                    String content = mStaxReader.getPIData();
                    if (content == null) {
                        text = target;
                    } else {
                        StringBuffer sb = new StringBuffer(target.length() + 1 + content.length());
                        sb.append(target);
                        /* Dirty hack: append space between contents, iff data
                         * does not start with white space char:
                         */
                        if (content.length() > 0 && content.charAt(0) > CHAR_SPACE) {
                            sb.append(' ');
                        }
                        sb.append(content);
                        text = sb.toString();
                    }
                }
                break;

            case DOCDECL:
                /* Seems like Xmlpull wants "<!DOCTYPE" (and matching closing
                 * ">") stripped out... so let's see:
                 */
                text = mStaxReader.getText();
                {
                    final String PREFIX = "<!DOCTYPE";
                    if (text.startsWith(PREFIX)) {
                        text = text.substring(PREFIX.length());
                        if (text.endsWith(">")) {
                            text = text.substring(0, text.length()-1);
                        }
                        // probably has at least leading spaces now
                        //text = text.trim();
                    }
                }

                break;
            }
        }
        mCurrText = text;
        return text;
    }

    public char[] getTextCharacters(int[] offsetAndLen)
    {
        if (mStaxReader == null) {
            return null;
        } 
        if (mCurrTextBuffer != null) {
            return mCurrTextBuffer;
        }
        if (mCurrText !=  null) {
            char[] c = mCurrText.toCharArray();
            mCurrTextBuffer = c;
            offsetAndLen[0] = 0;
            offsetAndLen[1] = c.length;
            return c;
        }

        switch (mCurrEvent) {
        case CDSECT:
        case DOCDECL:
        case ENTITY_REF:
        case IGNORABLE_WHITESPACE:
        case TEXT:
            break; // let's do real processing later on

        case PROCESSING_INSTRUCTION:
            // hmmmmmh.
            {
                offsetAndLen[0] = 0;
                String target = mStaxReader.getPITarget();
                String content = mStaxReader.getPIData();

                if (content == null) {
                    char[] ret = target.toCharArray();
                    offsetAndLen[1] = ret.length;
                    return ret;
                }
                StringBuffer sb = new StringBuffer(target.length() + 1 + content.length());
                sb.append(target);
                sb.append(' ');
                sb.append(content);
                offsetAndLen[1] = sb.length();
                String str = sb.toString();
                mCurrText = str;
                char[] c = str.toCharArray();
                mCurrTextBuffer = c;
                return c;
            }
        default:
            return null;
        }

        // Let's not cache it if reader shares its internal buffer...
        int start = mStaxReader.getTextStart();
        offsetAndLen[0] = start;
        offsetAndLen[1] = start + mStaxReader.getTextLength();
        return mStaxReader.getTextCharacters();
    }

    public boolean isAttributeDefault(int index)
    {
         if (mStaxReader == null || mCurrEvent != START_TAG) {
            return false;
        }
        return !mStaxReader.isAttributeSpecified(index);
    }

    public boolean isEmptyElementTag()
        throws XmlPullParserException
    {
        if (mStaxReader == null || mCurrEvent != START_TAG) {
            /* As per specs, need to throw an exception... further; although
             * API docs don't say it, compatibility tests imply exception
             * also HAS to be of type XmlPullParserException, not runtime
             * exception.
             */
            throw new XmlPullParserException("parser must be on START_TAG to call isEmptyElementTag", this, null);
        }
        /* 16-Jul-2004, TSa: StAX doesn't really have support for accessing
         *   this information...
         */
        // !!! TBI
        return false;
    }

    public boolean isWhitespace()
        throws XmlPullParserException
    {
        if (mStaxReader == null) {
            ;
        } else if (mCurrEvent == IGNORABLE_WHITESPACE) {
            return true;
        } else if (mCurrEvent == TEXT || mCurrEvent == CDSECT) {
            return mStaxReader.isWhiteSpace();
        }
        // conformance tests say it has to be of this type...
        throw new XmlPullParserException("Can not call 'isWhitespace()' for "
                                         +getTypeDesc(mCurrEvent));
    }

    public int next()
        throws IOException, XmlPullParserException
    {
        mCurrText = null;
        mCurrTextBuffer = null;

        while (true) {
            int type = nextToken();

            if (type == IGNORABLE_WHITESPACE || type == CDSECT) {
                /* 16-Jul-2004, TSa: Not sure what would be the right way,
                 *   whether to really change the underlying type... or
                 *   just return different value. Let's do former, for now?
                 */
                mCurrEvent = type = TEXT;
                return type;
            }
            if (type == TEXT || type == START_TAG || type == END_TAG
                || type == START_DOCUMENT || type == END_DOCUMENT) {
                return type;
            }
            // ok, let's loop and read in more events
        }
    }

    public int nextTag()
        throws IOException, XmlPullParserException
    {
        // Calling next clears up current text value
        int type = next();
        while (type == TEXT && isWhitespace()) {   // skip whitespace
            type = next();
        }

        if (type != START_TAG && type != END_TAG) {
            throw new XmlPullParserException("nextTag(): expected start or end tag (but got "
                                             +getTypeDesc(type)+")", this, null);
        }
        return type;
    }

    public String nextText()
        throws IOException, XmlPullParserException
    {
        if (mCurrEvent != START_TAG) {
            throw new XmlPullParserException("parser must be on START_TAG to read next text (not "
                                             +getTypeDesc(mCurrEvent)+")", this, null);
        }
        // Calling next clears up current text value
        int eventType = next();
        /* 21-Jul-2004, TSa: One problem: since next() may filter out things
         *   like comments, we may still need to re-combine text sections
         *   separated by such 'invisible' nodes.
         */
        if (eventType == TEXT) {
            String result = getText();
            StringBuffer sb = null;
            eventType = next();
            for (; eventType != END_TAG; eventType = next()) {
                if (eventType != TEXT) {
                    throw new XmlPullParserException("nextText(): event TEXT must be immediately followed by END_TAG (not "
                                                     +getTypeDesc(eventType)+")");
                }
                if (sb == null) {
                    String newText = getText();
                    sb = new StringBuffer(result.length() + newText.length() + 16);
                    sb.append(result);
                    sb.append(newText);
                } else {
                    sb.append(getText());
                }
            }
            /* Let's also make it so that this combined text is the new
             * text content; not just segment last read in.
             */
            mCurrText = (sb == null) ? result : sb.toString();
            return mCurrText;
        } else if (eventType == END_TAG) {
            return "";
        }

        throw new XmlPullParserException("START_TAG must be followed by TEXT (not "
                                         +getTypeDesc(eventType)+")", this, null);
    }

    /**
     * This is the main parsing method all other methods eventually call,
     * to get the next parsing event.
     */
    public int nextToken()
        throws IOException, XmlPullParserException
    {
        mCurrText = null;
        mCurrTextBuffer = null;

        return doGetNextToken();
    }

    public void require(int type, String ns, String name)
        throws XmlPullParserException
    {
        if (type != getEventType()) {
            throw new XmlPullParserException( "Required "+ getTypeDesc(type)
                                              +", got "+getTypeDesc(getEventType()),
                                              this, null);
        }
        if (ns != null && !ns.equals(getNamespace())) {
            throw new XmlPullParserException( "Required namespace URI '"
                                              +ns+"', got '"+getNamespace()+"'");
        }
        if (name != null && name.length() > 0) {
            if (!name.equals(getName())) {
                throw new XmlPullParserException( "expected name '"+name+"', got '"+getName()+"'");
            }
        }
    }

    public void setFeature(String name, boolean state)
        throws XmlPullParserException
    {
        if (mStaxReader != null) {
            throwIllegalConfigState();
        }
        if (name.equals(FEATURE_PROCESS_DOCDECL)) {
            setSupportDTD(mStaxFactory, state);
            // Need to turn off validation too
            if (!state) {
                setValidating(mStaxFactory, false);
            }
        } else if (name.equals(FEATURE_PROCESS_NAMESPACES)) {
            setNamespaceAware(mStaxFactory, state);
        } else if (name.equals(FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            /* 22-Jul-2004, TSa: Let's actually throw an exception, to
             *   indicate we do not support the feature. This helps with
             *   Xmlpull conformance tests...
             */
            throw new XmlPullParserException("Feature '"+name+"' not supported by StAX-based xmlpull implementation.", this, null);
        } else if (name.equals(FEATURE_VALIDATION)) {
            setValidating(mStaxFactory, state);
            // Also need to turn on dtd support, if validation turned on:
            if (state) {
                setSupportDTD(mStaxFactory, true);
            }
        } else {
            throw new XmlPullParserException("Unrecognized feature '"+name+"'.",
                                             this, null);
        }
    }

    public void setInput(InputStream inputStream, String inputEncoding)
    { 
        resetInput();

        /* 20-Jul-2004, TSa: Conformance tests imply that null is not
         *   acceptable here... (which is weird as it is ok for Reader)
         */
        if (inputStream == null) {
            throw new IllegalArgumentException("Can not pass null InputStream for setInput()");
        }
        
        mInputStream = inputStream;
        mInputEncoding = inputEncoding;
    }

    /**
     * @param r Reader to use for reading input. Apparently it is ok
     *   to pass in null; doing so will reset parsing.
     */
    public void setInput(Reader r)
    {
        resetInput();
        mInputReader = r;
    }

    public void setProperty(String name, Object value) 
    {
        if (name.equals(XPP_LOCATION)) {
            mLocationDesc = (value == null) ? null : value.toString();
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods:
    ////////////////////////////////////////////////////
    */

    protected int doGetNextToken()
        throws IOException, XmlPullParserException
    {
        if (mStaxReader == null) { // need to initialize?
            initForReading();
        }
        /* First: if current event was END_TAG, now we need to do couple
         * of things; reduce depth, and clear out ns/attr settings:
         */
        if (mCurrEvent == END_TAG) {
            --mDepth;
        }

        // And then other basic cleanup:
        mCurrLocation = null; // will be lazily constructed if/as needed

        try {
            int type = xppTypeFrom(mStaxReader.next());
            mCurrEvent = type;
            if (type == START_TAG) {
                ++mDepth;
                mAttrCount = mStaxReader.getAttributeCount();
                mNsCount = mStaxReader.getNamespaceCount();
            } else if (type == END_TAG) {
                mAttrCount = 0; // no way to access them...
                mNsCount = mStaxReader.getNamespaceCount();
            } else if (type == DOCDECL) {
                // Any data from DOCTYPE decl we need?
                // !!! TBI
            }
            return type;
        } catch (XMLStreamException strex) {
            throwFromXSEx(strex);
            return -1; // never gets here
        }
    }

    protected static int staxTypeFrom(int xppType)
    {
        switch (xppType) {
        case CDSECT:
            return XMLStreamConstants.CDATA;
        case COMMENT:
            return XMLStreamConstants.COMMENT;
        case DOCDECL:
            return XMLStreamConstants.DTD;
        case END_DOCUMENT:
            return XMLStreamConstants.END_DOCUMENT;
        case END_TAG:
            return XMLStreamConstants.END_ELEMENT;
        case ENTITY_REF:
            return XMLStreamConstants.ENTITY_REFERENCE;
        case IGNORABLE_WHITESPACE:
            return XMLStreamConstants.SPACE;
        case PROCESSING_INSTRUCTION:
            return XMLStreamConstants.PROCESSING_INSTRUCTION;
        case START_DOCUMENT:
            return XMLStreamConstants.START_DOCUMENT;
        case START_TAG:
            return XMLStreamConstants.START_ELEMENT;
        case TEXT:
            return XMLStreamConstants.CHARACTERS;
        }
        // shouldn't really have any unknown ones.... ?
        return -1;
    }

    protected static int xppTypeFrom(int staxType)
    {
        switch (staxType) {
        case XMLStreamConstants.CDATA:
            return CDSECT;
        case XMLStreamConstants.COMMENT:
            return COMMENT;
        case XMLStreamConstants.DTD:
            return DOCDECL;
        case XMLStreamConstants.END_DOCUMENT:
            return END_DOCUMENT;
        case XMLStreamConstants.END_ELEMENT:
            return END_TAG;
        case XMLStreamConstants.ENTITY_REFERENCE:
            return ENTITY_REF;
        case XMLStreamConstants.SPACE:
            return IGNORABLE_WHITESPACE;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
            return PROCESSING_INSTRUCTION;
        case XMLStreamConstants.START_DOCUMENT:
            return START_DOCUMENT;
        case XMLStreamConstants.START_ELEMENT:
            return START_TAG;
        case XMLStreamConstants.CHARACTERS:
            return TEXT;
        }
        /* There are a few values for which there are no counterparts;
         * entity and notation declarations, attributes, etc.
         */
        return -1;
    }

    protected String getTypeDesc(int type) {
        if (type < 0 || type >= TYPES.length) {
            return (type == -1) ? "[UNDEFINED]" : "[unknown]";
        }
        return TYPES[type];
    }

    /**
     * Method called to throw an exception to indicate that configuration
     * failed since reader wrapper is in wrong state; generally that
     * reader has already been initialized.
     */
    protected void throwIllegalConfigState() {
        throw new IllegalStateException("Can not configure parser when parsing has been started.");
    }

    protected void throwIllegalState(String method) {
        throw new IllegalStateException("Illegal state "
                                        +getTypeDesc(mCurrEvent)
                                        +": can't call '"+method+"'.");

    }

    protected void throwIndexOutOfBounds(String method, int index, int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("Illegal state "
                                              +getTypeDesc(mCurrEvent)
                                              +": can't call '"+method+"'.");

        }
        throw new IndexOutOfBoundsException("Illegal index "+index
                                          +"; min index 0, max index "+(count-1)
                                          +" (method "+method+")");
    }

    protected void throwFromXSEx(XMLStreamException strex)
        throws XmlPullParserException
    {
        throw new XmlPullParserException(strex.toString(), this, strex);
    }

    protected static void setNamespaceAware(XMLInputFactory factory, boolean state)
    {
        if (factory.isPropertySupported(XMLInputFactory.IS_NAMESPACE_AWARE)) {
            /* Let's not fail on this, as support for non-ns aware mode
             * is optional
             */
            try {
                factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,
                                    state ? Boolean.TRUE : Boolean.FALSE);
                
            } catch (IllegalArgumentException iex) {
                // Hmmh.... need better logging:
                System.err.println("Warning: couldn't set namespace support to "
                                   +state+": "+iex);
            }
        }
    }

    protected static void setSupportDTD(XMLInputFactory factory, boolean state) {
        factory.setProperty(XMLInputFactory.SUPPORT_DTD,
                            state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setValidating(XMLInputFactory factory, boolean state)
    {
        if (factory.isPropertySupported(XMLInputFactory.IS_VALIDATING)) {
            /* Validation is an optional property to support (at least 'true'
             * setting).
             */
            try {
                factory.setProperty(XMLInputFactory.IS_VALIDATING,
                                    state ? Boolean.TRUE : Boolean.FALSE);
            } catch (IllegalArgumentException iex) {
                // Hmmh.... need better logging:
                System.err.println("Warning: couldn't turn DTD support to "
                                   +state+": "+iex);
            }
        }
    }

    protected Location getLocation() {
        if (mCurrLocation == null) {
            if (mStaxReader != null) {
                mCurrLocation = mStaxReader.getLocation();
            }
        }
        return mCurrLocation;
    }
}

