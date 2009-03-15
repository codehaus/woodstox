/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.sr;

import java.io.IOException;

import javax.xml.stream.Location;
import javax.xml.namespace.QName;

/**
 * Container for information collected regarding a single element
 * attribute instance. Used for both regular explicit attributes
 * and values added via attribute value defaulting.
 *<p>
 * This class is not exposed outside of the package and is considered
 * part of internal implementation.
 */
final class Attribute
{
    // // // Name information

    protected String mLocalName;

    protected String mPrefix;

    protected String mNamespaceURI;

    // // // Value information

    /**
     * Numeric offset within text builder that denotes end
     * (last charater + 1) for the current value. Start
     * is not stored here but in the collector that manages
     * this attribute
     */
    protected int mValueEndOffset;

    /**
     * Value as a String iff it has been requested once; stored
     * here in case it will be accessed again.
     */
    protected String mReusableValue;

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public Attribute() { }

    /*
    //////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////
     */

    /*
    //////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////
     */
}
