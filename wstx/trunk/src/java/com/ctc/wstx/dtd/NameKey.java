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

/**
 * Simple key Object to be used for storing/accessing of potentially namespace
 * scoped element and attribute names.
 *<p>
 * One important note about usage is that the name components (prefix and
 * local name) HAVE to have been interned some way, as all comparisons
 * are done using identity comparison.
 *<p>
 * Note that the main reason this class is mutable -- unlike most key classes
 * -- is that this allows reusing key objects for access, as long as the code
 * using it knows ramifications of trying to modify a key that's used
 * in a data structure.
 *<p>
 * Note, too, that the hash code is cached as this class is mostly used as
 * a Map key, and hash code is used a lot.
 */
public final class NameKey
    implements Comparable // to allow alphabetic ordering
{
    private String mPrefix, mLocalName;

    volatile int mHash = 0;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public NameKey(String prefix, String localName)
    {
        mPrefix = prefix;
        mLocalName = localName;
    }

    public void reset(String prefix, String localName)
    {
        mPrefix = prefix;
        mLocalName = localName;
        mHash = 0;
    }

    /*
    ///////////////////////////////////////////////////
    // Accessors:
    ///////////////////////////////////////////////////
     */

    public String getPrefix() { return mPrefix; }

    public String getLocalName() { return mLocalName; }

    /*
    ///////////////////////////////////////////////////
    // Overridden standard methods:
    ///////////////////////////////////////////////////
     */
    
    public String toString() {
        if (mPrefix == null) {
            return mLocalName;
        }
        StringBuffer sb = new StringBuffer(mPrefix.length() + 1 + mLocalName.length());
        sb.append(mPrefix);
        sb.append(':');
        sb.append(mLocalName);
        return sb.toString();
    }

    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NameKey)) { // also filters out nulls
            return false;
        }
        NameKey other = (NameKey) o;

        return (mLocalName == other.mLocalName)
            && (mPrefix == other.mPrefix);

        /*
        String n = other.mLocalName;
        if (n != mLocalName && !n.equals(mLocalName)) {
            return false;
        }
        n = other.mPrefix;
        if (n == mPrefix) {
            return true;
        }
        return (n != null) && n.equals(mPrefix);
        */
    }

    public int hashCode() {
        int hash = mHash;

        if (hash == 0) {
            hash = mLocalName.hashCode();
            if (mPrefix != null) {
                hash ^= mPrefix.hashCode();
            }
            mHash = hash;
        }
        return hash;
    }

    public int compareTo(Object o)
    {
        NameKey other = (NameKey) o;

        // First, by prefix, then by local name:
        String op = other.mPrefix;

        // Missing prefix is ordered before existing prefix
        if (op == null || op.length() == 0) {
            if (mPrefix != null && mPrefix.length() > 0) {
                return 1;
            }
        } else if (mPrefix == null || mPrefix.length() == 0) {
            return -1;
        } else {
            int result = mPrefix.compareTo(op);
            if (result != 0) {
                return result;
            }
        }

        return mLocalName.compareTo(other.mLocalName);
    }
}
