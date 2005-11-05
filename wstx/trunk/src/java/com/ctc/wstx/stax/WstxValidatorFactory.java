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
import java.net.URL;
import java.util.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.ctc.wstx.exc.WstxIOException;

/**
 * Factory for creating validator schema and validator objects (currently
 * just DTD subsets).
 */
public final class WstxValidatorFactory
    extends XMLValidatorFactory
{
    public WstxValidatorFactory() {
    }

    /*
    ////////////////////////////////////////////////////////////
    // Configuration methods
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
    // Factory methods
    ////////////////////////////////////////////////////////////
     */

    public XMLValidatorSchema createSchema(InputStream in, String encoding,
                                           String publicId, String systemId)
        throws XMLStreamException
    {
        return null;
    }

    public XMLValidatorSchema createSchema(Reader r, String publicId,
                                           String systemId)
    {
        return null;
    }

    public XMLValidatorSchema createSchema(URL url)
        throws XMLStreamException
    {
        /*
        try {
            return createSchema(URL.openStream(), null, url.toExternalForm());
        } catch (IOException ioe) {
            throw new WstxIOException(ioe);
        }
        */
        return null;
    }

    public XMLValidatorSchema createSchema(File f)
        throws XMLStreamException
    {
        return null;
    }
}
