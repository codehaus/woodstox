package org.codehaus.staxbind.dbconv;

public final class JacksonDriver
    extends DbconvDriver
{
    public JacksonDriver() throws Exception
    {
        super(new JacksonConverter());
    }
}
