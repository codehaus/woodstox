package org.codehaus.staxbind.dbconv;

public final class Wstx3Driver
    extends DbconvDriver
{
    public Wstx3Driver()
    {
        super(getConverter());
    }

    final static DbConverter getConverter()
    {
        return new StaxXmlConverter(DbConverter.WSTX_INPUT_FACTORY, DbConverter.WSTX_OUTPUT_FACTORY);
    }
}
