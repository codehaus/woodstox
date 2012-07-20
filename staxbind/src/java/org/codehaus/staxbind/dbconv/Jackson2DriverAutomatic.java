package org.codehaus.staxbind.dbconv;

import org.codehaus.staxbind.std.StdJackson2Converter;

/**
 * Driver that uses "automatic" (bean/annotation-based) serialization with
 * Jackson (compared to hand-written one)
 */
public final class Jackson2DriverAutomatic
    extends DbconvDriver
{
    public Jackson2DriverAutomatic() throws Exception
    {
        super(new StdJackson2Converter<DbData>(DbData.class));
    }
}
