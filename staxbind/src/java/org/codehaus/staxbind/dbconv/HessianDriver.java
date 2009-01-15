package org.codehaus.staxbind.dbconv;

import org.codehaus.staxbind.std.StdHessianConverter;

public final class HessianDriver
    extends DbconvDriver
{
    public HessianDriver() throws Exception
    {
        super(new StdHessianConverter<DbData>());
    }
}
