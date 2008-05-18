package org.codehaus.stax2.typed;

/**
 * Interface that defines generic typed value decoder API used
 * by {@link TypedXMLStreamReader} to allow for customized decoders
 * to be used efficiently.
 */
public interface TypedValueDecoder
{
    public Object decode(String input) throws IllegalArgumentException;

    public Object decode(char[] buffer, int start, int len) throws IllegalArgumentException;
}

