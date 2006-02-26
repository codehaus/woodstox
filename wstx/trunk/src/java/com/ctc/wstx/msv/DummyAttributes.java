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

/**
 * This is a NOP implementation of SAX Attributes interface. It is used
 * in places where a placeholder is needed, but where no actual calls
 * are expected (which is the case with most of usage by MSV it seems).
 */
public class DummyAttributes
    implements org.xml.sax.Attributes
{
    /**
     * In paranoid mode, we'll throw RuntimeException if access methods
     * are actually called. This may be necessary to verify actual usage
     * of the singleton instance
     */
    final static boolean PARANOID = true;

    private final static DummyAttributes sInstance = new DummyAttributes();

    private DummyAttributes() { }

    public static DummyAttributes getInstance() { return sInstance; }

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
        if (PARANOID) { illegalAccess(); }
        return -1;
    }

    public int getLength()
    {
        if (PARANOID) { illegalAccess(); }
        return 0;
    }

    public String getLocalName(int index)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getQName(int index)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getType(int index)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getType(String qName)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getType(String uri, String localName)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getURI(int index)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getValue(int index)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getValue(String qName)
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    public String getValue(String uri, String localName)     
    {
        // No exceptions thrown; null to be returned for illegal indexes
        return null;
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////
    */

    private void illegalAccess()
    {
        throw new IllegalStateException("Unexpected call to DummyAttributes -- should not occur");
    }
}


