package org.codehaus.staxbind.dbconv;

public final class Stax2Driver
    extends DbconvDriver
{
    public Stax2Driver()
    {
        super(new Stax2XmlConverter());
    }

    public void initializeDriver() {
        ((Stax2XmlConverter) _converter).initStax2
            (getParam("javax.xml.stream.XMLInputFactory"),
             getParam("javax.xml.stream.XMLOutputFactory")
             );
    }
}
