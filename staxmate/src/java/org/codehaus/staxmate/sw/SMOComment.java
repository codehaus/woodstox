package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

public class SMOComment
    extends SMSimpleOutput
{
    final String mText;

    public SMOComment(SMOutputContext ctxt, String text) {
	super(ctxt);
	mText = text;
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	mContext.writeComment(mText);
	return true;
    }
}
