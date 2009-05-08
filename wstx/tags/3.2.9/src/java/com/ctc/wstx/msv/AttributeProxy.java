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

import org.codehaus.stax2.validation.ValidationContext;

/**
 * This is an implementation of SAX Attributes interface, that proxies
 * requests to the {@link ValidationContext}.
 * It is needed by some MSV components (specifically, W3C Schema Validator)
 * for limited access to attribute values during start element validation.
 */
public final class AttributeProxy
    implements org.xml.sax.Attributes
{
    /**
     * Static flag used to compile in/remove checks for calls that
     * are unimplemented (on assumption they are not needed)
     */
    private final static boolean PARANOID = true;

    private final ValidationContext mContext;

    public AttributeProxy(ValidationContext ctxt)
    {
        mContext = ctxt;
    }

    /*
    ///////////////////////////////////////////////
    // Attributes implementation
    ///////////////////////////////////////////////
    */

    public int getIndex(String qName)
    {
        if (PARANOID) { illegalAccess(); }
        return -1;
    }

    public int getIndex(String uri, String localName)
    {
        return mContext.findAttributeIndex(uri, localName);
    }

    public int getLength()
    {
        return mContext.getAttributeCount();
    }

    public String getLocalName(int index)
    {
        return mContext.getAttributeLocalName(index);
    }

    public String getQName(int index)
    {
        /* Shouldn't be called; could be implemented although
         * inefficiently (since StAX does not use such prefixed names)
         */
        if (PARANOID) { illegalAccess(); }
        return null;
    }

    public String getType(int index)
    {
        // Shouldn't be needed...
        if (PARANOID) { illegalAccess(); }
        return null;
    }

    public String getType(String qName)
    {
        // Shouldn't be needed...
        if (PARANOID) { illegalAccess(); }
        return null;
    }

    public String getType(String uri, String localName)
    {
        // Shouldn't be needed...
        if (PARANOID) { illegalAccess(); }
        return null;
    }

    public String getURI(int index)
    {
        return mContext.getAttributeNamespace(index);
    }

    public String getValue(int index)
    {
        return mContext.getAttributeValue(index);
    }

    public String getValue(String qName)
    {
        // Shouldn't be needed...
        if (PARANOID) { illegalAccess(); }
        return null;
    }

    public String getValue(String uri, String localName)     
    {
        return mContext.getAttributeValue(uri, localName);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////
    */

    private void illegalAccess()
    {
        throw new IllegalStateException("Unexpected call to AttributeProxy method that was assumed not to be needed");
    }
}


