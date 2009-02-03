package org.codehaus.staxbind.dbconv;

public final class IbmStaxDriver
    extends DbconvDriver
{
    public IbmStaxDriver()
    {
        super(getConverter());
    }

    final static DbConverter getConverter()
    {
        return new StaxXmlConverter("com.ibm.xml.xlxp.api.stax.XMLInputFactoryImpl"
                                    ,"com.ibm.xml.xlxp.api.stax.XMLOutputFactoryImpl");
    }
}
