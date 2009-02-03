package org.codehaus.staxbind.dbconv;

public final class SunStaxDriver
    extends DbconvDriver
{
    public SunStaxDriver()
    {
        super(getConverter());
    }

    final static DbConverter getConverter()
    {

        // Hmmh. JDK would use this:
        /*
        return new StaxXmlConverter("com.sun.xml.internal.stream.XMLInputFactoryImpl"
                                    ,"com.sun.xml.internal.stream.XMLOutputFactoryImpl"
                                    );
        */

        // But stand-alone one from http://sjsxp.dev.java.net this:

        return new StaxXmlConverter("com.sun.xml.stream.ZephyrParserFactory",
                                    "com.sun.xml.stream.ZephyrWriterFactory"
                                    );
    }
}
