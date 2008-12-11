package org.codehaus.staxbind.dbconv;

/**
 * Driver that uses "automatic" (bean/annotation-based) serialization with
 * Jackson (compared to hand-written one)
 */
public final class JacksonDriverAutomatic
    extends DbconvDriver
{
    public JacksonDriverAutomatic() throws Exception
    {
        super(new JacksonConverterAutomatic());
    }
}
