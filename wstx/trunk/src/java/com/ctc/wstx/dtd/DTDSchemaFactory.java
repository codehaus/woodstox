/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
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

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.XMLValidator;
import org.codehaus.stax2.validation.XMLValidatorFactory;
import org.codehaus.stax2.validation.XMLValidatorSchema;

/**
 * Schema factory that allows parsing of external DTD subsets; resulting
 * in DTD "schema" instances. These instances are shared blueprints that
 * can be used and reused to create actual DTD validators for documents.
 */
public class DTDSchemaFactory
    extends XMLValidatorFactory
{
    /**
     * By default, dtd instances are constructed to be used in
     * "namespace aware" mode (even though DTDs generally have some
     * issues with namespaces...)
     */
    protected final static boolean DEFAULT_NS_AWARE = true;

    /*
    ///////////////////////////////////////////////////
    // Factory configuration settings
    ///////////////////////////////////////////////////
     */

    protected boolean mNamespaceAware = DEFAULT_NS_AWARE;

    public DTDSchemaFactory() {
        super();
    }

    /*
    ///////////////////////////////////////////////////
    // Configuration methods
    ///////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String propName)
    {
        // !!! TBI
        return false;
    }

    /**
     * @param propName Name of property to set
     * @param value Value to set property to
     *
     * @return True if setting succeeded; false if property was recognized
     *   but could not be changed to specified value, or if it was not
     *   recognized but the implementation did not throw an exception.
     */
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
    ///////////////////////////////////////////////////
    // Factory method implementations
    ///////////////////////////////////////////////////
     */

    public XMLValidatorSchema createSchema(InputStream in, String encoding,
                                           String publicId, String systemId)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidatorSchema createSchema(Reader r, String publicId,
                                           String systemId)
    {
        // !!! TBI
        return null;
    }

    public XMLValidatorSchema createSchema(URL url)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidatorSchema createSchema(File f)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    /*
    ///////////////////////////////////////////////////
    // Extended Woodstox API (needed to parse internal
    // subsets, for example)
    ///////////////////////////////////////////////////
     */

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */
}
