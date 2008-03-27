/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

import java.io.*;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.io.Stax2Result;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.dom.DOMWrappingWriter;
import com.ctc.wstx.evt.WstxEventWriter;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.CharsetNames;
import com.ctc.wstx.io.UTF8Writer;
import com.ctc.wstx.sw.*;
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

    public XMLEventWriter createXMLEventWriter(OutputStream out)
        throws XMLStreamException
    {
        return createXMLEventWriter(out, null);
    }

    public XMLEventWriter createXMLEventWriter(OutputStream out, String enc)
         throws XMLStreamException
   {
       if (out == null) {
           throw new IllegalArgumentException("Null OutputStream is not a valid argument");
       }
       return new WstxEventWriter(createSW(out, null, enc, false));
    }

    public XMLEventWriter createXMLEventWriter(javax.xml.transform.Result result)
         throws XMLStreamException
    {
        return new WstxEventWriter(createSW(result));
    }

    public XMLEventWriter createXMLEventWriter(Writer w)
        throws XMLStreamException
    {
        if (w == null) {
            throw new IllegalArgumentException("Null Writer is not a valid argument");
        }
        return new WstxEventWriter(createSW(null, w, null, false));
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out)
        throws XMLStreamException
    {
        return createXMLStreamWriter(out, null);
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out, String enc)
        throws XMLStreamException
    {
        if (out == null) {
            throw new IllegalArgumentException("Null OutputStream is not a valid argument");
        }
        return createSW(out, null, enc, false);
    }

    public XMLStreamWriter createXMLStreamWriter(javax.xml.transform.Result result)
        throws XMLStreamException
    {
        return createSW(result);
    }

    public XMLStreamWriter createXMLStreamWriter(Writer w)
        throws XMLStreamException
    {
        if (w == null) {
            throw new IllegalArgumentException("Null Writer is not a valid argument");
        }
        return createSW(null, w, null, false);
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
    // StAX2 extensions
    /////////////////////////////////////////
     */

    // // // StAX2 additional (encoding-aware) factory methods

    public XMLEventWriter createXMLEventWriter(Writer w, String enc)
        throws XMLStreamException
    {
        return new WstxEventWriter(createSW(null, w, enc, false));
    }

    public XMLEventWriter createXMLEventWriter(XMLStreamWriter sw)
        throws XMLStreamException
    {
        return new WstxEventWriter(sw);
    }

    public XMLStreamWriter2 createXMLStreamWriter(Writer w, String enc)
        throws XMLStreamException
    {
        return createSW(null, w, enc, false);
    }

    // // // StAX2 "Profile" mutators

    public void configureForXmlConformance()
    {
        mConfig.configureForXmlConformance();
    }

    public void configureForRobustness()
    {
        mConfig.configureForRobustness();
    }

    public void configureForSpeed()
    {
        mConfig.configureForSpeed();
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
     *
     * @param autoCloseOutput Whether writer should automatically close the
     *   output stream or Writer, when close() is called on stream writer.
     */
    private BaseStreamWriter createSW(OutputStream out, Writer w, String enc,
                                     boolean autoCloseOutput)
        throws XMLStreamException
    {
        /* Need to ensure that the configuration object is not shared
         * any more; otherwise later changes via factory could be
         * visible half-way through output...
         */
        WriterConfig cfg = mConfig.createNonShared();
        XmlWriter xw;

        if (w == null) {
            if (enc == null) {
                enc = WstxOutputProperties.DEFAULT_OUTPUT_ENCODING;
            } else {
                enc = CharsetNames.normalize(enc);
            }

            try {
                if (enc == CharsetNames.CS_UTF8) {
                    /* 16-Aug-2006, TSa: Note: utf8 writer may or may not
                     *   need to close the stream it has, but buffering
                     *   xml writer must call close on utf8 writer. Thus:
                     */
                    w = new UTF8Writer(cfg, out, autoCloseOutput);
                    xw = new BufferingXmlWriter(w, cfg, enc, true, out);
                } else if (enc == CharsetNames.CS_ISO_LATIN1) {
                    xw = new ISOLatin1XmlWriter(out, cfg, autoCloseOutput);
                } else if (enc == CharsetNames.CS_US_ASCII) {
                    xw = new AsciiXmlWriter(out, cfg, autoCloseOutput);
                } else {
                    w = new OutputStreamWriter(out, enc);
                    xw = new BufferingXmlWriter(w, cfg, enc, autoCloseOutput, out);
                }
            } catch (IOException ex) {
                throw new XMLStreamException(ex);
            }
        } else {
            // we may still be able to figure out the encoding:
            if (enc == null) {
                enc = CharsetNames.findEncodingFor(w);
            }
            try {
                xw = new BufferingXmlWriter(w, cfg, enc, autoCloseOutput, out);
            } catch (IOException ex) {
                throw new XMLStreamException(ex);
            }
        }

        if (cfg.willSupportNamespaces()) {
            if (cfg.automaticNamespacesEnabled()) {
                return new RepairingNsStreamWriter(xw, enc, cfg);
            }
            return new SimpleNsStreamWriter(xw, enc, cfg);
        }
        return new NonNsStreamWriter(xw, enc, cfg);
    }

    private XMLStreamWriter createSW(Result res)
        throws XMLStreamException
    {
        OutputStream out = null;
        Writer w = null;
        String encoding = null;
        boolean autoclose;

        if (res instanceof Stax2Result) {
            Stax2Result sr = (Stax2Result) res;
            try {
                out = sr.constructOutputStream();
                if (out == null) {
                    w = sr.constructWriter();
                }
            } catch (IOException ioe) {
                throw new WstxIOException(ioe);
            }
            autoclose = true;
        } else if (res instanceof StreamResult) {
            StreamResult sr = (StreamResult) res;
            out = sr.getOutputStream();
            if (out == null) {
                w = sr.getWriter();
            }
            autoclose = false; // caller still owns it, no automatic close
        } else if (res instanceof SAXResult) {
            //SAXResult sr = (SAXResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a SAXResult -- not implemented.");
        } else if (res instanceof DOMResult) {
           return DOMWrappingWriter.createFrom(mConfig.createNonShared(), (DOMResult) res);
        } else {
            throw new IllegalArgumentException("Can not instantiate a writer for XML result type "+res.getClass()+" (unrecognized type)");
        }

        if (out != null) {
            return createSW(out, null, encoding, autoclose);
        }
        if (w != null) {
            return createSW(null, w, encoding, autoclose);
        }
        throw new XMLStreamException("Can not create StAX writer for passed-in Result -- neither writer nor output stream was accessible");
    }
}
