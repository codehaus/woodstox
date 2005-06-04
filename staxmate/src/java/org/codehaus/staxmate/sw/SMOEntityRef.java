package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

public class SMOEntityRef
    extends SMSimpleOutput
{
    final String mName;

    public SMOEntityRef(SMOutputContext ctxt, String name) {
	super(ctxt);
	mName = name;
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	mContext.writeEntityRef(mName);
	return true;
    }
}
