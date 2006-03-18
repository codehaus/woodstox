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

package com.ctc.wstx.dtd;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.stax.ImplInfo;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.URLUtil;

/**
 * Factory for creating DTD validator schema objects (shareable stateless
 * "blueprints" for creating actual validators).
 *<p>
 * Due to close coupling of XML and DTD, some of the functionality
 * implemented (like that of reading internal subsets embedded in XML
 * documents) is only accessible by core Woodstox. The externally
 * accessible
 */
public class DTDValidatorFactory
    extends XMLValidationSchemaFactory
{
    /*
    /////////////////////////////////////////////////////
    // Objects shared by actual parsers
    /////////////////////////////////////////////////////
     */

    /**
     * 'Root' symbol table, used for creating actual symbol table instances,
     * but never as is.
     */
    final static SymbolTable mRootSymbols = DefaultXmlSymbolTable.getInstance();
    static {
        mRootSymbols.setInternStrings(true);
    }

    /**
     * This configuration object is used (instead of a more specific one)
     * since the actual DTD reader uses such configuration object.
     */
    protected final ReaderConfig mConfig;

    public DTDValidatorFactory() {
        mConfig = ReaderConfig.createFullDefaults();
    }

    /*
    ////////////////////////////////////////////////////////////
    // Stax2, Configuration methods
    ////////////////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String propName)
    {
        return propName.equals(XMLStreamProperties.XSP_IMPLEMENTATION_NAME)
            || propName.equals(XMLStreamProperties.XSP_IMPLEMENTATION_VERSION);
    }

    public boolean setProperty(String propName, Object value)
    {
        // Nothing to set, yet?
        return false;
    }

    public Object getProperty(String propName)
    {
        if (propName.equals(XMLStreamProperties.XSP_IMPLEMENTATION_NAME)) {
            return ImplInfo.getImplName();
        }
        if (propName.equals(XMLStreamProperties.XSP_IMPLEMENTATION_VERSION)) {
            return ImplInfo.getImplVersion();
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Stax2, Factory methods
    ////////////////////////////////////////////////////////////
     */

    public XMLValidationSchema createSchema(InputStream in, String encoding,
                                           String publicId, String systemId)
        throws XMLStreamException
    {
        return doCreateSchema(StreamBootstrapper.getInstance
                              (in, publicId, systemId, mConfig.getInputBufferLength()),
                              publicId, systemId, null);
    }

    public XMLValidationSchema createSchema(Reader r, String publicId,
                                           String systemId)
        throws XMLStreamException
    {
        return doCreateSchema(ReaderBootstrapper.getInstance(r, publicId, systemId, null),
                              publicId, systemId, null);
    }

    public XMLValidationSchema createSchema(URL url)
        throws XMLStreamException
    {
        try {
            InputStream in = URLUtil.optimizedStreamFromURL(url);
            return doCreateSchema(StreamBootstrapper.getInstance
                                  (in, null, null, mConfig.getInputBufferLength()),
                                  null, url.toExternalForm(), url);
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
    }

    public XMLValidationSchema createSchema(File f)
        throws XMLStreamException
    {
        try {
            URL url = f.toURL();
            return doCreateSchema(StreamBootstrapper.getInstance
                                  (new FileInputStream(f), null, null, mConfig.getInputBufferLength()),
                                  null, url.toExternalForm(), url);
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Woodstox-specific API
    ////////////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * The main validator construction method, called by all externally
     * visible methods.
     */
    protected XMLValidationSchema doCreateSchema
        (InputBootstrapper bs, String publicId, String systemId, URL ctxt)
        throws XMLStreamException
    {
        ReaderConfig cfg = mConfig.createNonShared(mRootSymbols.makeChild());

        try {
            Reader r = bs.bootstrapInput(false, cfg.getXMLReporter(), XmlConsts.XML_V_UNKNOWN);
            if (ctxt == null) { // this is just needed as context for param entity expansion
                ctxt = URLUtil.urlFromCurrentDir();
            }
            /* Note: need to pass unknown for 'xmlVersion' here (as well as
             * above for bootstrapping), since this is assumed to be the main
             * level parsed document and no xml version compatibility checks
             * should be done.
             */
            WstxInputSource src = InputSourceFactory.constructEntitySource
                (null, null, bs, publicId, systemId, XmlConsts.XML_V_UNKNOWN, ctxt, r);

            /* true -> yes, fully construct for validation
             * (does not mean it has to be used for validation, but required
             * if it is to be used for that purpose)
             */
            return FullDTDReader.readExternalSubset(src, cfg, /*int.subset*/null, true, bs.getDeclaredVersion());
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
    }
}
