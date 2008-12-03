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
        return new StaxXmlConverter("com.ctc.wstx.stax.WstxInputFactory",
                                    "com.ctc.wstx.stax.WstxOutputFactory");
    }
}
