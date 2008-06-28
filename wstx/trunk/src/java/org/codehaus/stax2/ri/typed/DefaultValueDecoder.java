package org.codehaus.stax2.ri.typed;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * Default implementation that strives for correctness and reasonable
 * efficiency (at least for simple types). To simplify implementation,
 * decoders are stateful.
 *
 * @author Tatu Saloranta
 */
public class DefaultValueDecoder
{
    protected String mType;

    /**
     * Pointer to the next character to check, within lexical value
     */
    protected int mPtr;

    /**
     * Pointer to the pointer in lexical value <b>after</b> last
     * included character.
     */
    protected int mEnd;

    // // // Life-cycle

    public DefaultValueDecoder() { }

    /*
    ///////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////
     */

    // Unlimited precision numeric types

    public final BigInteger decodeInteger(String lexical) throws IllegalArgumentException {
        return decodeInteger(lexical, 0, lexical.length());
    }

    public BigInteger decodeInteger(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        mType = "integer";
        start = trimLeading(lexical, start, end);
        end = trimTrailing(lexical, start, end);
        if (start > 0 || end < lexical.length()) {
            lexical = lexical.substring(start, end);
        }
        try {
            return new BigInteger(lexical);
        } catch (NumberFormatException nex) {
            throw constructInvalidValue(lexical);
        }
    }

    public BigInteger decodeInteger(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        mType = "integer";
        start = trimLeading(lexical, start, end);
        end = trimTrailing(lexical, start, end);
        String lexicalStr = new String(lexical, start, (end-start));
        try {
            return new BigInteger(lexicalStr);
        } catch (NumberFormatException nex) {
            throw constructInvalidValue(lexicalStr);
        }
    }

    public final BigDecimal decodeDecimal(String lexical) throws IllegalArgumentException {
        return decodeDecimal(lexical, 0, lexical.length());
    }

    public BigDecimal decodeDecimal(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        mType = "decimal";
        start = trimLeading(lexical, start, end);
        end = trimTrailing(lexical, start, end);
        if (start > 0 || end < lexical.length()) {
            lexical = lexical.substring(start, end);
        }
        try {
            return new BigDecimal(lexical);
        } catch (NumberFormatException nex) {
            throw constructInvalidValue(lexical);
        }
    }

    public BigDecimal decodeDecimal(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        mType = "decimal";
        start = trimLeading(lexical, start, end);
        end = trimTrailing(lexical, start, end);
        int len = end-start;
        try {
            return new BigDecimal(lexical, start, len);
        } catch (NumberFormatException nex) {
            throw constructInvalidValue(new String(lexical, start, len));
        }
    }

    // Name types

    public QName decodeQName(char[] lexical, int start, int end,
                             NamespaceContext nsCtxt)
        throws IllegalArgumentException
    {
        mType = "QName";
        start = trimLeading(lexical, start, end);
        end = trimTrailing(lexical, start, end);

        int i = start;
        for (; i < end; ++i) {
            if (lexical[i] == ':') {
                return resolveQName(nsCtxt,
                                    new String(lexical, start, i-start),
                                    new String(lexical, i+1, end-i-1));
            }
        }
        return resolveQName(nsCtxt, new String(lexical, start, end-start));
    }

    public QName decodeQName(String lexical,
                             NamespaceContext nsCtxt)
        throws IllegalArgumentException
    {
        mType = "QName";
        lexical = lexical.trim();
        if (lexical.length() == 0) {
            throw constructMissingValue();
        }
        int ix = lexical.indexOf(':');
        if (ix >= 0) { // qualified name
            return resolveQName(nsCtxt,
                                lexical.substring(0, ix),
                                lexical.substring(ix+1));
        }
        return resolveQName(nsCtxt, lexical);
    }

    protected QName resolveQName(NamespaceContext nsCtxt,
                                 String localName)
        throws IllegalArgumentException
    {
        // No prefix -> default namespace ("element rules")
        String uri = nsCtxt.getNamespaceURI("");
        if (uri == null) { // some impls may return null
            uri = "";
        }
        return new QName(uri, localName, "");
    }

    protected QName resolveQName(NamespaceContext nsCtxt,
                                 String prefix, String localName)
        throws IllegalArgumentException
    {
        if (prefix.length() == 0 || localName.length() == 0) {
            // either prefix or local name is empty String, illegal
            throw constructInvalidValue(prefix+":"+localName);
        }
        /* Explicit prefix, must map to a bound namespace; and that
         * namespace can not be empty (only "no prefix", i.e. 'default
         * namespace' has empty URI)
         */
        String uri = nsCtxt.getNamespaceURI(prefix);
        if (uri == null || uri.length() == 0) {
            throw new IllegalArgumentException("Value \""+lexicalDesc(prefix+":"+localName)+"\" not a valid QName: prefix not bound to a namespace");
        }
        return new QName(uri, localName, prefix);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods, trimming/scanning
    ///////////////////////////////////////////////
     */

    /**
     * @param start First character of the lexical value to process
     * @param end Pointer character <b>after</b> last valid character
     *   of the lexical value
     *
     * @return First non-white space character from the String
     */
    protected char resetAndTrimLeading(String lexical, String type, int start, int end)
    {
        mType = type;
        mEnd = end;

        while (start < end) {
            char c = lexical.charAt(start++);
            if (!isSpace(c)) {
                mPtr = start;
                return c;
            }
        }
        throw constructMissingValue();
    }

    protected int trimLeading(String lexical, int start, int end)
    {
        while (start < end) {
            char c = lexical.charAt(start);
            if (!isSpace(c)) {
                return start;
            }
            ++start;
        }
        throw constructMissingValue();
    }

    protected int trimTrailing(String lexical, int start, int end)
    {
        while (--end > start && isSpace(lexical.charAt(end))) { }
        return end+1;
    }

    protected char resetAndTrimLeading(char[] lexical, String type, int start, int end)
    {
        mType = type;
        mEnd = end;

        while (start < end) {
            char c = lexical[start++];
            if (!isSpace(c)) {
                mPtr = start;
                return c;
            }
        }
        throw constructMissingValue();
    }

    protected int trimLeading(char[] lexical, int start, int end)
    {
        while (start < end) {
            char c = lexical[start];
            if (!isSpace(c)) {
                return start;
            }
            ++start;
        }
        throw constructMissingValue();
    }

    protected int trimTrailing(char[] lexical, int start, int end)
    {
        while (--end > start && isSpace(lexical[end])) { }
        return end+1;
    }

    /**
     * Method called to check that remaining String consists of zero or
     * more digits, followed by zero or more white space, and nothing else;
     * and to trim trailing white space, if any.
     *
     * @return Number of valid digits found; or -1 to indicate invalid
     *   input
     */
    protected int trimTrailingAndCheckDigits(String lexical)
    {
        // Let's start from beginning
        int ptr = mPtr;

        // Note: caller won't call this with empty String
        char ch = lexical.charAt(ptr);

        // First, skim through digits
        while (ch <= '9' && ch >= '0') {
            if (++ptr >= mEnd) { // no trailing white space, valid
                return (ptr - mPtr);
            }
            ch = lexical.charAt(ptr);
        }

        // And then see what follows digits...
        int len = ptr - mPtr;
        while (true) {
            if (!isSpace(ch)) { // garbage following white space (or digits)
                return -1;
            }
            if (++ptr >= mEnd) {
                return len;
            }
            ch = lexical.charAt(ptr);
        }
    }

    protected int trimTrailingAndCheckDigits(char[] lexical)
    {
        // Let's start from beginning
        int ptr = mPtr;

        // Note: caller won't call this with empty String
        char ch = lexical[ptr];

        // First, skim through digits
        while (ch <= '9' && ch >= '0') {
            if (++ptr >= mEnd) { // no trailing white space, valid
                return (ptr - mPtr);
            }
            ch = lexical[ptr];
        }

        // And then see what follows digits...
        int len = ptr - mPtr;
        while (true) {
            if (!isSpace(ch)) { // garbage following white space (or digits)
                return -1;
            }
            if (++ptr >= mEnd) {
                return len;
            }
            ch = lexical[ptr];
        }
    }

    protected final boolean allWhitespace(String lexical, int start, int end)
    {
        while (start < end) {
            if (!isSpace(lexical.charAt(start++))) {
                return false;
            }
        }
        return true;
    }

    protected final boolean allWhitespace(char[] lexical, int start, int end)
    {
        while (start < end) {
            if (!isSpace(lexical[start++])) {
                return false;
            }
        }
        return true;
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods, error reporting
    ///////////////////////////////////////////////
     */

    protected IllegalArgumentException constructMissingValue()
    {
        throw new IllegalArgumentException("Empty value (all white space) not a valid lexical representation of "+mType);
    }

    protected IllegalArgumentException constructInvalidValue(String lexical)
    {
        // !!! Should we escape ctrl+chars etc?
        return new IllegalArgumentException("Value \""+lexical+"\" not a valid lexical representation of "+mType);
    }

    protected IllegalArgumentException constructInvalidValue(String lexical, int startOffset)
    {
        return new IllegalArgumentException("Value \""+lexicalDesc(lexical, startOffset)+"\" not a valid lexical representation of "+mType);
    }

    protected IllegalArgumentException constructInvalidValue(char[] lexical, int startOffset)
    {
        return new IllegalArgumentException("Value \""+lexicalDesc(lexical, startOffset)+"\" not a valid lexical representation of "+mType);
    }

    protected String lexicalDesc(char[] lexical, int startOffset)
    {
        int len = mEnd-startOffset;
        String str = new String(lexical, startOffset, len);
        // !!! Should we escape ctrl+chars etc?
        return str.trim();
    }

    protected String lexicalDesc(String lexical, int startOffset)
    {
        String str = lexical.substring(startOffset, mEnd);
        // !!! Should we escape ctrl+chars etc?
        return str.trim();
    }

    protected String lexicalDesc(String lexical)
    {
        // !!! Should we escape ctrl+chars etc?
        return lexical.trim();
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods, low-level string manipulation
    ///////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: final for efficient inlining by JVM/HotSpot. Means
     * one can not redefine it; and instead need to override
     * callers (which just means methods above used for trimming
     * white space)
     *<p>
     * Note, too, that it is assumed that any "weird" white space
     * (xml 1.1 LSEP and NEL) have been replaced by canonical
     * alternatives (linefeed for element content, regular space
     * for attributes)
     */
    private final boolean isSpace(char c)
    {
        return ((int) c) <= 0x0020;
    }
}
