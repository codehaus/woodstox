package org.codehaus.staxbind.dbconv;

import javax.xml.stream.*;

public final class Stax1Driver
    extends DbconvDriver
{
    public Stax1Driver()
    {
        super(new StaxXmlConverter());
    }

    @Override
    public void initializeDriver() {
        ((StaxXmlConverter) _converter).initStax
            (getParam("javax.xml.stream.XMLInputFactory"),
             getParam("javax.xml.stream.XMLOutputFactory")
             );
    }
}
