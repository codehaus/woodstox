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

package com.ctc.wstx.msv;

import java.io.*;
import java.net.URL;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.*;

import org.xml.sax.InputSource;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.sun.msv.grammar.trex.TREXGrammar;
import com.sun.msv.reader.GrammarReaderController;
import com.sun.msv.reader.trex.ng.RELAXNGReader;
import com.sun.msv.verifier.regexp.REDocumentDeclaration;

import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.util.URLUtil;

/**
 * This is a StAX2 schema factory that can parse and create schema instances
 * for creating validators that validate documents to check their validity
 * against specific Relax NG specifications. It requires
 * Sun Multi-Schema Validator
 * (http://www.sun.com/software/xml/developers/multischema/)
 * to work, and acts as a quite thin wrapper layer (although not a completely
 * trivial one, since MSV only exports SAX API, some adapting is needed)
 */
public class RelaxNGSchemaFactory
    extends XMLValidationSchemaFactory
{
    protected final SAXParserFactory mSaxFactory;

    /**
     * For now, there's no need for fine-grained error/problem reporting
     * infrastructure, so let's just use a dummy controller.
     */
    protected final GrammarReaderController mDummyController =
        new com.sun.msv.reader.util.IgnoreController();       

    public RelaxNGSchemaFactory()
    {
        /* Let's get the SAX parser factory, to be used for creating
         * SAX parsers that the grammar reader needs
         */
        mSaxFactory = SAXParserFactory.newInstance();
        mSaxFactory.setNamespaceAware(true);    
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

    public XMLValidationSchema createSchema(InputStream in, String encoding,
                                           String publicId, String systemId)
        throws XMLStreamException
    {
        InputSource src = new InputSource(in);
        src.setEncoding(encoding);
        src.setPublicId(publicId);
        src.setSystemId(systemId);
        return loadSchema(src);
    }

    public XMLValidationSchema createSchema(Reader r, String publicId,
                                            String systemId)
        throws XMLStreamException
    {
        InputSource src = new InputSource(r);
        src.setPublicId(publicId);
        src.setSystemId(systemId);
        return loadSchema(src);
    }

    public XMLValidationSchema createSchema(URL url)
        throws XMLStreamException
    {
        try {
            InputStream in = URLUtil.optimizedStreamFromURL(url);
            InputSource src = new InputSource(in);
            src.setSystemId(url.toExternalForm());
            return loadSchema(src);
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
    }

    public XMLValidationSchema createSchema(File f)
        throws XMLStreamException
    {
        try {
            return createSchema(f.toURL());
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////
     */

    protected XMLValidationSchema loadSchema(InputSource src)
        throws XMLStreamException
    {
        /* !!! 28-Dec-2005, TSa: Sax factory is not guaranteed to be
         *   thread-safe... need to figure out suitable locking scheme
         *   (or just create new instances all the time)
         */

        /* Another thing; should we use a controller to get notified about
         * errors in parsing?
         */
        
        TREXGrammar grammar = RELAXNGReader.parse
            (src, mSaxFactory, mDummyController);
        return new RelaxNGSchema(grammar);
    }
}
