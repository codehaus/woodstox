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
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
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
public final class DTDValidatorFactory
    extends XMLValidatorFactory
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
        // !!! TBI
        return false;
    }

    public boolean setProperty(String propName, Object value)
    {
        // !!! TBI
        return false;
    }

    public Object getProperty(String propName)
    {
        // !!! TBI
        return null;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Stax2, Factory methods
    ////////////////////////////////////////////////////////////
     */

    public XMLValidatorSchema createSchema(InputStream in, String encoding,
                                           String publicId, String systemId)
        throws XMLStreamException
    {
        return doCreateSchema(StreamBootstrapper.getInstance(in, publicId, systemId, mConfig.getInputBufferLength()),
                              publicId, systemId, null);
    }

    public XMLValidatorSchema createSchema(Reader r, String publicId,
                                           String systemId)
        throws XMLStreamException
    {
        return doCreateSchema(ReaderBootstrapper.getInstance(r, publicId, systemId, mConfig.getInputBufferLength(), null),
                              publicId, systemId, null);
    }

    public XMLValidatorSchema createSchema(URL url)
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

    public XMLValidatorSchema createSchema(File f)
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
    protected XMLValidatorSchema doCreateSchema
        (InputBootstrapper bs, String publicId, String systemId, URL ctxt)
        throws XMLStreamException
    {
        ReaderConfig cfg = mConfig.createNonShared(mRootSymbols.makeChild());

        try {
            Reader r = bs.bootstrapInput(false, cfg.getXMLReporter());
            if (ctxt == null) { // this is just needed as context for param entity expansion
                ctxt = URLUtil.urlFromCurrentDir();
            }
            WstxInputSource src = InputSourceFactory.constructEntitySource
                (null, null, bs, publicId, systemId, ctxt, r);
            
            DTDSubset extSubset = FullDTDReader.readExternalSubset
                (src, cfg, /*int. subset*/ null);
            
            // !!! TBI
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }

        return null;
    }
}
