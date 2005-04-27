/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.stax;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.evt.WstxEventWriter;
import com.ctc.wstx.sw.BaseStreamWriter;
import com.ctc.wstx.sw.NonNsStreamWriter;
import com.ctc.wstx.sw.RepairingNsStreamWriter;
import com.ctc.wstx.sw.SimpleNsStreamWriter;
import com.ctc.wstx.util.ArgUtil;

/**
 * Implementation of {@link XMLOutputFactory} for Wstx.
 *<p>
 * TODO:
 *<ul>
 * <li>Implement outputter that creates SAX events
 *  </li>
 * <li>Implement outputter that builds DOM trees
 *  </li>
 *</ul>
 */
public final class WstxOutputFactory
    extends XMLOutputFactory2
    implements OutputConfigFlags
{
    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    protected final WriterConfig mConfig;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////
     */

    public WstxOutputFactory() {
        mConfig = WriterConfig.createFullDefaults();
    }

    /*
    /////////////////////////////////////////////////////
    // XMLOutputFactory API
    /////////////////////////////////////////////////////
     */

    public XMLEventWriter createXMLEventWriter(OutputStream out) {
        return createXMLEventWriter(new OutputStreamWriter(out));
    }

    public XMLEventWriter createXMLEventWriter(OutputStream out, String enc)
         throws XMLStreamException
   {
        try {
            return createXMLEventWriter(new OutputStreamWriter(out, enc));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public XMLEventWriter createXMLEventWriter(javax.xml.transform.Result result)
         throws XMLStreamException
    {
        return new WstxEventWriter(createSW(result));
    }

    public XMLEventWriter createXMLEventWriter(Writer w) {
        return new WstxEventWriter(createSW(w));
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out)
    {
        return createXMLStreamWriter(new OutputStreamWriter(out));
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out, String enc)
        throws XMLStreamException
    {
        try {
            return createXMLStreamWriter(new OutputStreamWriter(out, enc));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public XMLStreamWriter createXMLStreamWriter(javax.xml.transform.Result result)
        throws XMLStreamException
    {
        return createSW(result);
    }

    public XMLStreamWriter createXMLStreamWriter(Writer w) {
        return createSW(w);    
    }
    
    public Object getProperty(String name)
    {
        return mConfig.getProperty(name);
    }
    
    public boolean isPropertySupported(String name) {
        return mConfig.isPropertySupported(name);
    }
    
    public void setProperty(String name, Object value)
    {
        mConfig.setProperty(name, value);
    }

    /*
    /////////////////////////////////////////
    // Woodstox-specific configuration access
    /////////////////////////////////////////
     */

    public WriterConfig getConfig() {
        return mConfig;
    }

    /*
    /////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////
     */

    /**
     * Bottleneck factory method used internally; needs to take care of passing
     * proper settings to stream writer.
     */
    private BaseStreamWriter createSW(Writer w)
    {
        /* Need to ensure that the configuration object is not shared
         * any more; otherwise later changes via factory could be
         * visible half-way through output...
         */
        WriterConfig cfg = mConfig.createNonShared();
        if (cfg.willSupportNamespaces()) {
            if (cfg.automaticNamespacesEnabled()) {
                return new RepairingNsStreamWriter(w, cfg);
            }
            return new SimpleNsStreamWriter(w, cfg);
        }
        return new NonNsStreamWriter(w, cfg);
    }

    private BaseStreamWriter createSW(Result res)
        throws XMLStreamException
    {
        if (res instanceof StreamResult) {
            StreamResult sr = (StreamResult) res;
            Writer w = sr.getWriter();
            if (w == null) {
                OutputStream out = sr.getOutputStream();
                if (out == null) {
                    throw new XMLStreamException("Can not create StAX writer for a StreamResult -- neither writer nor output stream was set.");
                }
                // ... any way to define encoding?
                w = new OutputStreamWriter(out);
            }
            return createSW(w);
        }

        if (res instanceof SAXResult) {
            SAXResult sr = (SAXResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a SAXResult -- not (yet) implemented.");
        }

        if (res instanceof DOMResult) {
            DOMResult sr = (DOMResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a DOMResult -- not (yet) implemented.");
        }

        throw new IllegalArgumentException("Can not instantiate a writer for XML result type "+res.getClass()+" (unknown type)");
    }

    /*
    /////////////////////////////////////////////////////
    // Trivial test driver, to check loading of the
    // class and instance creation work (mostly to check
    // j2me subset works)
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        WstxOutputFactory f = new WstxOutputFactory();

        // !!! TODO: Test it somehow?

        System.out.println("Writer factory created ok.");
    }
}
