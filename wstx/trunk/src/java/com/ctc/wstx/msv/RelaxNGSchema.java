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
 * This is a validation schema instance based on a RELAX NG schema. It
 * serves as a shareable "blueprint" for creating actual validator instances.
 */
public class RelaxNGSchema
    implements XMLValidationSchema
{
    protected final TREXGrammar mGrammar;

    public RelaxNGSchema(TREXGrammar grammar)
    {
        mGrammar = grammar;
    }

    public String getSchemaType() {
        return XMLValidationSchema.SCHEMA_ID_RELAXNG;
    }

    public XMLValidator createValidator(ValidationContext ctxt)
        throws XMLStreamException
    {
        return new RelaxNGValidator(this);
    }
}
