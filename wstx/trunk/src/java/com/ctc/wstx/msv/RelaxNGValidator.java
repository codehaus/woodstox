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

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import com.sun.msv.grammar.Grammar;
import com.sun.msv.grammar.trex.TREXGrammar;

import com.ctc.wstx.exc.WstxIOException;

/**
 * Actual non-shareable validator instance, that is bound to an XML
 * document, or document subset.
 */
public class RelaxNGValidator
    extends XMLValidator
{
    final XMLValidationSchema mParentSchema;

    public RelaxNGValidator(XMLValidationSchema parent)
    {
        mParentSchema = parent;
    }

    /*
    ///////////////////////////////////////
    // XMLValidator implementation
    ///////////////////////////////////////
    */

    public XMLValidationSchema getSchema() {
        return mParentSchema;
    }

    /**
     * Method called to update information about the newly encountered (start)
     * element. At this point namespace information has been resolved, but
     * no DTD validation has been done. Validator is to do these validations,
     * including checking for attribute value (and existence) compatibility.
     */
    public void validateElementStart(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        // !!! TBI
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix, String value)
        throws XMLValidationException
    {
        // !!! TBI
        return null;
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix,
                                    char[] valueChars, int valueStart,
                                    int valueEnd)
        throws XMLValidationException
    {
        // !!! TBI
        return null;
    }
    
    public int validateElementAndAttributes()
        throws XMLValidationException
    {
        // !!! TBI
        return CONTENT_ALLOW_ANY_TEXT;
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int validateElementEnd(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        // !!! TBI
        return CONTENT_ALLOW_ANY_TEXT;
    }

    public void validateText(String text, boolean lastTextSegment)
        throws XMLValidationException
    {
        // !!! TBI
    }

    public void validateText(char[] cbuf, int textStart, int textEnd,
                             boolean lastTextSegment)
        throws XMLValidationException
    {
        // !!! TBI
    }

    public void validationCompleted(boolean eod)
        throws XMLValidationException
    {
        // Is there something we should do here...?
    }

    /*
    ///////////////////////////////////////
    // Attribute info access
    ///////////////////////////////////////
    */

    // // // Access to type info

    public String getAttributeType(int index)
    {
        // !!! TBI
        return null;
    }    

    public int getIdAttrIndex()
    {
        // !!! TBI
        return -1;
    }

    public int getNotationAttrIndex()
    {
        // !!! TBI
        return -1;
    }

}
