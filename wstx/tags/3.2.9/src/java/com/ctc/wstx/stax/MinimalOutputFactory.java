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

import java.io.*;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import javax.xml.stream.*;

import org.codehaus.stax2.io.Stax2Result;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.CharsetNames;
import com.ctc.wstx.io.UTF8Writer;
import com.ctc.wstx.sw.*;
import com.ctc.wstx.util.ArgUtil;

/**
 * Minimalistic input factory, which implements the suggested J2ME
 * subset of {@link javax.xml.stream.XMLOutputFactory} API: basically
 * just the cursor-based iteration, and classes it needs.
 *<p>
 * Unfortunately, the way StAX 1.0 is defined, this class can NOT be
 * the base class of the full input factory, without getting references
 * to most of StAX event classes. It does however have lots of shared
 * (cut'n pasted code) with {@link com.ctc.wstx.stax.WstxOutputFactory}.
 * Hopefully in future this problem can be resolved.
 */
public class MinimalOutputFactory
    //extends XMLOutputFactory
    implements OutputConfigFlags
{
    /**
     * Flag used to distinguish "real" minimal implementations and
     * extending non-minimal ones (currently there's such distinction
     * for input factories, for minimal <= validating <= event-based,
     * but not for ouput)
     */
    protected final boolean mIsMinimal;

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

    protected MinimalOutputFactory(boolean isMinimal) {
        mIsMinimal = isMinimal;
        mConfig = WriterConfig.createJ2MEDefaults();
    }

    /**
     * Need to add this method, since we have no base class to do it...
     */
    public static MinimalOutputFactory newMinimalInstance() {
        return new MinimalOutputFactory(true);
    }

    /*
    /////////////////////////////////////////////////////
    // XMLOutputFactory API
    /////////////////////////////////////////////////////
     */

    //public XMLEventWriter createXMLEventWriter(OutputStream out);

    public XMLStreamWriter createXMLStreamWriter(OutputStream out)
        throws XMLStreamException
    {
        return createSW(out, null, null, false);
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out, String enc)
        throws XMLStreamException
    {
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
        return createSW(null, w, null, false);    
    }

    public XMLStreamWriter createXMLStreamWriter(Writer w, String enc)
        throws XMLStreamException
    {
        return createSW(null, w, enc, false);
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
     * Factory method used internally; needs to take care of passing
     * proper settings to stream writer.
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
                    //xw = new ISOLatin1XmlWriter(out, cfg);
                } else if (enc == CharsetNames.CS_ISO_LATIN1) {
                    xw = new ISOLatin1XmlWriter(out, cfg, autoCloseOutput);

                    /* 15-Dec-2006, TSa: For now, let's just use the
                     *   default buffering writer for Ascii: while it's
                     *   easy to create one from Latin1, it should be
                     *   done after some refactorings are done to Latin1,
                     *   not right now.
                     */
                    /*
                } else if (enc == CharsetNames.CS_US_ASCII) {
                    // !!! TBI
                    xw = new ISOLatin1XmlWriter(out, cfg, autoCloseOutput);
                    */
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

    private BaseStreamWriter createSW(Result res)
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
            //DOMResult sr = (DOMResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a DOMResult -- not (yet?) implemented.");
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
