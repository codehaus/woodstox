/* StAX2 API.
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under BSD license.
 */

package org.codehaus.stax2;

import java.net.URL;

/**
 * Simple data class that encapsulates concept of an external reference
 * from an XML document (reference to DTD, external entities, notations).
 * It is used both as an argument to indicate location of external
 * entities to fetch, and as a key for caching purposes.
 *<p>
 * Note about equality comparisons: public id is considered to have a
 * precedence -- if it is non-null, it is used as the only part to
 * compare. Otherwise the system is used for comparisons. In both cases,
 * comparisons are only done for matching parts.
 */
public class ExternalId
{
    protected final String mPublicId;

    protected final URL mSystemId;

    /*
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    protected ExternalId(String publicId, URL systemId)
    {
        mPublicId = publicId;
        mSystemId = systemId;
    }

    protected ExternalId(ExternalId orig)
    {
        mPublicId = orig.mPublicId;
        mSystemId = orig.mSystemId;
    }

    public static ExternalId constructFromPublicId(String publicId)
    {
        if (publicId == null || publicId.length() == 0) {
            throw new IllegalArgumentException("Empty/null public id.");
        }
        return new ExternalId(publicId, null);
    }

    public static ExternalId constructFromSystemId(URL systemId)
    {
        if (systemId == null) {
            throw new IllegalArgumentException("Null system id.");
        }
        return new ExternalId(null, systemId);
    }

    public static ExternalId construct(String publicId, URL systemId)
    {
        if (publicId != null && publicId.length() > 0) {
            return new ExternalId(publicId, null);
        }
        if (systemId == null) {
            throw new IllegalArgumentException("Illegal arguments; both public and system id null/empty.");
        }
        return new ExternalId(null, systemId);
    }

    /*
    ///////////////////////////////////////////////
    // Public API, accessors:
    ///////////////////////////////////////////////
     */

    public String getPublicId() {
        return mPublicId;
    }

    public URL getSystemId() {
        return mSystemId;
    }

    /*
    ///////////////////////////////////////////////
    // Overridden standard methods
    ///////////////////////////////////////////////
     */

    /**
     * Since we'll only use hash code of one of the Strings, and since
     * String objects themselves cache hash code, let's not duplicate
     * that value here.
     */
    public int hashCode() {

        if (mPublicId != null) {
            return mPublicId.hashCode();
        }
        return mSystemId.hashCode();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(60);
        sb.append("Public-id: ");
        sb.append(mPublicId);
        sb.append(", system-id: ");
        sb.append(mSystemId);
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (!(o instanceof ExternalId)) {
            return false;
        }
        ExternalId other = (ExternalId) o;
        if (mPublicId != null) {
            String op = other.mPublicId;
            return (op != null) && op.equals(mPublicId);
        }
        URL os = other.mSystemId;
        return (os != null) && os.equals(mSystemId);
    }
}
