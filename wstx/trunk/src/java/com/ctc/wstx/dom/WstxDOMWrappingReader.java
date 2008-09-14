package com.ctc.wstx.dom;

import java.util.Collections;

import javax.xml.stream.*;
import javax.xml.transform.dom.DOMSource;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.ri.dom.DOMWrappingReader;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.exc.WstxParsingException;

public class WstxDOMWrappingReader
    extends DOMWrappingReader
{
    protected final ReaderConfig mConfig;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected WstxDOMWrappingReader(DOMSource src, ReaderConfig cfg)
        throws XMLStreamException
    {
        super(src, cfg.willSupportNamespaces(), cfg.willCoalesceText());
        mConfig = cfg;
    }

    public static WstxDOMWrappingReader createFrom(DOMSource src, ReaderConfig cfg)
        throws XMLStreamException
    {
        return new WstxDOMWrappingReader(src, cfg);
    }

    /*
    ///////////////////////////////////////////////////
    // Defined/Overridden config methods
    ///////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String name)
    {
        // !!! TBI: not all these properties are really supported
        return mConfig.isPropertySupported(name);
    }

    public Object getProperty(String name)
    {
        if (name.equals("javax.xml.stream.entities")) {
            // !!! TBI
            return Collections.EMPTY_LIST;
        }
        if (name.equals("javax.xml.stream.notations")) {
            // !!! TBI
            return Collections.EMPTY_LIST;
        }
        // [WSTX-162]: no way to cleanly enable name/nsURI interning
        if (XMLInputFactory2.P_INTERN_NAMES.equals(name)
            || XMLInputFactory2.P_INTERN_NS_URIS.equals(name)) {
            return Boolean.FALSE;
        }
        return mConfig.getProperty(name);
    }

    public boolean setProperty(String name, Object value)
    {
        /* Note: can not call local method, since it'll return false for
         * recognized but non-mutable properties
         */
        if (XMLInputFactory2.P_INTERN_NAMES.equals(name)
            || XMLInputFactory2.P_INTERN_NS_URIS.equals(name)) {
            /* [WTSX-162]: Name/Namespace URI interning seemingly enabled,
             *   isn't. Alas, not easy to enable it, so let's force it to
             *   always be disabled
             */
            if (!(value instanceof Boolean) || ((Boolean) value).booleanValue()) {
                throw new IllegalArgumentException("DOM-based reader does not support interning of names or namespace URIs");
            }
            return true;
        }
        return mConfig.setProperty(name, value);
    }

    /*
    ///////////////////////////////////////////////////
    // Defined/Overridden error reporting
    ///////////////////////////////////////////////////
     */

    // @Override
    protected void throwStreamException(String msg, Location loc)
        throws XMLStreamException
    {
        if (loc == null) {
            throw new WstxParsingException(msg);
        }
        throw new WstxParsingException(msg, loc);
    }
}
