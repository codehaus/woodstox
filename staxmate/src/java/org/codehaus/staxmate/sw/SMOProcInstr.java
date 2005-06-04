package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

public class SMOProcInstr
    extends SMSimpleOutput
{
    final String mTarget;
    final String mData;

    public SMOProcInstr(SMOutputContext ctxt, String target, String data) {
	super(ctxt);
	mTarget = target;
	mData = data;
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	mContext.writeProcessingInstruction(mTarget, mData);
	return true;
    }
}
