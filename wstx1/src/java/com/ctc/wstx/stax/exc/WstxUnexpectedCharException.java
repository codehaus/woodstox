package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Generic exception type that indicates that tokenizer/parser encountered
 * unexpected (but not necessarily invalid per se) character, which is not
 * legal in current context. Could happen, for example, if white space
 * was missing between attribute value and name of next attribute.
 */
public class WstxUnexpectedCharException
    extends WstxParsingException
{
    final char mChar;

    public WstxUnexpectedCharException(String msg, Location loc, char c) {
        super(msg, loc);
        mChar = c;
    }

    public char getChar() {
        return mChar;
    }
}
