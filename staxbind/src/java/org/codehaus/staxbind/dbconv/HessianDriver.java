package org.codehaus.staxbind.dbconv;

public final class HessianDriver
    extends DbconvDriver
{
    public HessianDriver() throws Exception
    {
        super(new HessianConverter());
    }
}
