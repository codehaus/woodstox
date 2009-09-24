package org.codehaus.staxbind.dbconv;

import org.codehaus.staxbind.std.StdGsonConverter;

/**
 * Driver that uses Gson-based data binding for JSON serialization
 */
public final class GsonDriver
    extends DbconvDriver
{
    public GsonDriver() throws Exception
    {
        super(new StdGsonConverter<DbData>(DbData.class));
    }
}
