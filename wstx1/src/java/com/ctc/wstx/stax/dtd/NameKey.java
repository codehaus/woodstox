package com.ctc.wstx.stax.dtd;

/**
 * Simple key Object to be used for storing/accessing of potentially namespace
 * scoped element and attribute names.
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
        if (!(o instanceof NameKey)) {
            return false;
        }
        NameKey other = (NameKey) o;
        String n = other.mLocalName;

        if (n != mLocalName && !n.equals(mLocalName)) {
            return false;
        }
        n = other.mPrefix;
        if (n == mPrefix) {
            return true;
        }
        return (n != null) && n.equals(mPrefix);
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
}
