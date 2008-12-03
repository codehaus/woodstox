package org.codehaus.staxbind.dbconv;

public final class JaxbDriver
    extends DbconvDriver
{
    public JaxbDriver() throws Exception
    {
        super(new JaxbConverter());
    }
}
