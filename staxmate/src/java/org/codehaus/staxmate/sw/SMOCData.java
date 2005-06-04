package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

public abstract class SMOCData
    extends SMSimpleOutput
{
    private SMOCData(SMOutputContext ctxt) {
	super(ctxt);
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Factory methods
    ////////////////////////////////////////////////////////////////
     */

    public static SMLinkedOutput create(SMOutputContext ctxt, String text) {
	return new StringBased(ctxt, text);
    }

    public static SMLinkedOutput createShared(SMOutputContext ctxt, char[] buf, int offset, int len) {
	if (len < 1) {
	    return create(ctxt, "");
	}
	char[] arr = new char[len];
	System.arraycopy(buf, offset, arr, 0, len);
	return new ArrayBased(ctxt, arr);
    }

    public static SMLinkedOutput createNonShared(SMOutputContext ctxt,
						 char[] buf, int offset, int len) {
	if (offset == 0 && len == buf.length) {
	    return new ArrayBased(ctxt, buf);
	}
	return new ArrayBased3(ctxt, buf, offset, len);
    }

    protected abstract boolean doOutput(boolean canClose)
	throws XMLStreamException;

    /*
    ////////////////////////////////////////////////////////////////
    // Sub-classes
    ////////////////////////////////////////////////////////////////
     */

    private final static class StringBased
	extends SMOCData
    {
	final String mText;

	StringBased(SMOutputContext ctxt, String text) {
	    super(ctxt);
	    mText = text;
	}

	protected boolean doOutput(boolean canClose)
	    throws XMLStreamException
	{
	    mContext.writeCData(mText);
	    return true;
	}
    }

    private final static class ArrayBased
	extends SMOCData
    {
	final char[] mBuf;

	ArrayBased(SMOutputContext ctxt, char[] buf) {
	    super(ctxt);
	    mBuf = buf;
	}

	protected boolean doOutput(boolean canClose)
	    throws XMLStreamException
	{
	    mContext.writeCData(mBuf, 0, mBuf.length);
	    return true;
	}
    }

    private final static class ArrayBased3
	extends SMOCData
    {
	final char[] mBuf;
	final int mOffset, mLen;

	ArrayBased3(SMOutputContext ctxt, char[] buf, int offset, int len) {
	    super(ctxt);
	    mBuf = buf;
	    mOffset = offset;
	    mLen = len;
	}

	protected boolean doOutput(boolean canClose)
	    throws XMLStreamException
	{
	    mContext.writeCData(mBuf, mOffset, mLen);
	    return true;
	}
    }
}
