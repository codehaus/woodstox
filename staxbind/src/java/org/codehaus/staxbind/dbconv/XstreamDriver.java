package org.codehaus.staxbind.dbconv;

public final class XstreamDriver extends DbconvDriver
{
    public XstreamDriver()
    {
        super(new XstreamConverter());
    }
}
