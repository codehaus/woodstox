package org.codehaus.stax2.typed;

import javax.xml.stream.XMLStreamReader;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.namespace.QName;

/**
 * This abstract class defines API that can be used by typed
 * readers to externalize decoding of "native" (Java) values
 * from W3C Schema datatype defined textual representations.
 * Idea is that there can be both default shared decoder
 * implementations as well as custom ones, and that the typed
 * reader itself can be simplified as well as made customizable.
 *<p>
 * Set of methods is defined to both cover needs of typed
 * readers and to allow for highly efficient operation.
 * Latter is the reason why there are generally three variants
 * of most methods. One imp
 *
 * @author Tatu Saloranta
 */
public abstract class ValueDecoder
{
    // // // Simple numeric types

    // Integral types

    public int decodeInt(String lexical) throws IllegalArgumentException {
        return decodeInt(lexical, 0, lexical.length());
    }
    public abstract int decodeInt(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract int decodeInt(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    /*
    public long decodeLong(String lexical) throws IllegalArgumentException {
        return decodeLong(lexical, 0, lexical.length());
    }
    public abstract long decodeLong(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract long decodeLong(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    // Fixed-length floating-point types

    public float decodeFloat(String lexical) throws IllegalArgumentException {
        return decodeFloat(lexical, 0, lexical.length());
    }
    public abstract float decodeFloat(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract float decodeFloat(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    public double decodeDouble(String lexical) throws IllegalArgumentException {
        return decodeDouble(lexical, 0, lexical.length());
    }
    public abstract double decodeDouble(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract double decodeDouble(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    // Unlimited precision integer floating-point types

    public BigInteger decodeBigInteger(String lexical) throws IllegalArgumentException {
        return decodeBigInteger(lexical, 0, lexical.length());
    }

    public abstract BigInteger decodeBigInteger(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract BigDecimal decodeBigInteger(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    public BigDecimal decodeDecimal(String lexical) throws IllegalArgumentException {
        return decodeDecimal(lexical, 0, lexical.length());
    }

    public abstract BigDecimal decodeDecimal(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract BigDecimal decodeDecimal(char[] lexical, int first, int last)
        throws IllegalArgumentException;
    */

    // Enumerated types

    public boolean decodeBoolean(String lexical) throws IllegalArgumentException {
        return decodeBoolean(lexical, 0, lexical.length());
    }
    public abstract boolean decodeBoolean(String lexical, int first, int last)
        throws IllegalArgumentException;
    public abstract boolean decodeBoolean(char[] lexical, int first, int last)
        throws IllegalArgumentException;

    // // // Simple numeric types
}
