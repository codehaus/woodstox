package org.codehaus.stax2.ri;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;
import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;
import org.codehaus.stax2.util.StreamReader2Delegate;

/**
 * Simple straight-forward implementation of a filtering stream reader,
 * which can fully adapt Stax2 stream reader 
 * ({@link XMLStreamReader2}).
 */
public class Stax2FilteredStreamReader
    extends StreamReader2Delegate
    implements XMLStreamConstants
{
    final StreamFilter mFilter;

    public Stax2FilteredStreamReader(XMLStreamReader r, StreamFilter f)
    {
        super(Stax2ReaderAdapter.wrapIfNecessary(r));
        mFilter = f;
    }

    /*
    //////////////////////////////////////////////////////
    // XMLStreamReader method overrides that we need
    //////////////////////////////////////////////////////
     */

    public int next()
        throws XMLStreamException
    {
        int type;
        do {
            type = mDelegate2.next();
            if (mFilter.accept(this)) {
                break;
            }
        } while (type != END_DOCUMENT);

        return type;
    }

    public int nextTag()
        throws XMLStreamException
    {
        int type;
        // Can be implemented very much like next()
        while (true) {
            type = mDelegate2.nextTag();
            if (mFilter.accept(this)) {
                break;
            }
        }
        return type;
    }
}

