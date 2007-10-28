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

import org.relaxng.datatype.Datatype;
import com.sun.msv.verifier.regexp.StringToken;

import org.codehaus.stax2.validation.*;

/**
 * This is a wrapper/adapter class used to connect MSV to the Stax
 * validation context. Since functionality offered by stax, and needed
 * by MSV are very similar, there is no extensive logic involved.
 */
public class MSVContextProvider
    implements com.sun.msv.grammar.IDContextProvider2
{
    /**
     * Stax validation context accessed to provide functionality MSV requires.
     */
    final ValidationContext mContext;

    public MSVContextProvider(ValidationContext ctxt)
    {
        mContext = ctxt;
    }

    /*
    ///////////////////////////////////////////////////////////
    // Core RelaxNG ValidationContext implementation
    // (org.relaxng.datatype.ValidationContext, base interface
    // of the id provider context)
    ///////////////////////////////////////////////////////////
     */

    public String getBaseUri()
    {
        return mContext.getBaseUri();
    }

    public boolean isNotation(String notationName)
    {
        return mContext.isNotationDeclared(notationName);
    }

    public boolean isUnparsedEntity(String entityName)
    {
        return mContext.isUnparsedEntityDeclared(entityName);
    }

    public String resolveNamespacePrefix(String prefix) 
    {
        return mContext.getNamespaceURI(prefix);
    }

    /*
    ///////////////////////////////////////////////////////////
    // IdProviderContext2 extension implementation
    ///////////////////////////////////////////////////////////
    */

	public void onID(Datatype datatype, StringToken idToken)
    {
//System.err.println("WARNING: dt -> "+datatype+", literal -> "+literal.literal);
        int idType = datatype.getIdType();
        if (idType == Datatype.ID_TYPE_ID) {
            String id = idToken.literal.trim();
            /*
            StringToken existing = (StringToken)ids.get(literal);
            if( existing==null ) {
                // the first time this ID is used
                ids.put(literal,token);
            } else
            if( existing!=token ) {
                // duplicate id value
                onDuplicateId(literal);
            }
            */
        } else if (idType == Datatype.ID_TYPE_IDREF) {
            //idrefs.add(token.literal.trim());
        } else if (idType == Datatype.ID_TYPE_IDREFS) {
            /*
            StringTokenizer tokens = new StringTokenizer(token.literal);
            while (tokens.hasMoreTokens())
                idrefs.add(tokens.nextToken());
            */
        } else {
            throw new Error("Internal error: unexpected ID datatype: "+datatype);
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
    */
}
