package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.util.StringUtil;
import com.ctc.wstx.compat.JdkFeatures;

/**
 * Base class for all implementatations of {@link XMLStreamException}
 * Wstx uses.
 */
public class WstxException
    extends XMLStreamException
{
    /**
     * D'oh. Super-class munges and hides the message, have to duplicate here
     */
    final String mMsg;

    public WstxException(String msg) {
        super(msg);
        mMsg = msg;
    }

    public WstxException(Throwable th) {
        super(th);
        mMsg = "";

        // 13-Aug-2004, TSa: Better make sure root cause is set...
        JdkFeatures.getInstance().setInitCause(this, th);
    }

    public WstxException(String msg, Location loc) {
        super(msg, loc);
        mMsg = msg;
    }

    public WstxException(String msg, Location loc, Throwable th) {
        super(msg, loc, th);
        mMsg = msg;

        // 13-Aug-2004, TSa: Better make sure root cause is set...
        JdkFeatures.getInstance().setInitCause(this, th);
    }

    /**
     * Method is overridden for two main reasons: first, default method
     * does not display public/system id information, even if it exists, and
     * second, default implementation can not handle nested Location
     * information.
     */
    public String getMessage()
    {
        String locMsg = getLocationDesc();
        /* Better not use super's message, as it contains (part of) Location
         * info; something we can regenerate better...
         */
        if (locMsg == null) {
            return super.getMessage();
        }
        StringBuffer sb = new StringBuffer(mMsg.length() + locMsg.length() + 20);
        sb.append(mMsg);
        StringUtil.appendLF(sb);
        sb.append(" at ");
        sb.append(locMsg);
        return sb.toString();
    }

    public String toString()
    {
        return getClass().getName()+": "+getMessage();
    }

    /*
    ////////////////////////////////////////////////////////
    // Internal methods:
    ////////////////////////////////////////////////////////
     */

    protected String getLocationDesc()
    {
        Location loc = getLocation();
        return (loc == null) ? null : loc.toString();
    }
}
