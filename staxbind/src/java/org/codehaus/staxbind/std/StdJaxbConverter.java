package org.codehaus.staxbind.std;

import java.io.*;

import javax.xml.bind.*;
import javax.xml.stream.*;

/**
 * Converter that uses JAXB2 on top of regular Stax 1
 * implementation (such as Woodstox).
 */
public class StdJaxbConverter<T extends StdItem>
    extends StdConverter<T>
{
    XMLInputFactory _staxInFactory;
    XMLOutputFactory _staxOutFactory;

    final JAXBContext _jaxbContext;

    public StdJaxbConverter(Class<T> itemClass)
    {
        this(itemClass, null, null);
    }

    public StdJaxbConverter(Class<T> itemClass,
                            String staxInputFactory,
                            String staxOutputFactory)
    {
        _jaxbContext = JAXBContext.newInstance(itemClass);
        initStaxFactories(staxInputFactory, staxOutputFactory);
    }

    public void initStaxFactories(String staxInputFactory,
                                  String staxOutputFactory)
    {
        try {
            if (staxInputFactory != null) {
                _staxInFactory = (XMLInputFactory) Class.forName(staxInputFactory).newInstance();
            }
            if (_staxOutFactory != null) {
                _staxOutFactory = (XMLOutputFactory) Class.forName(staxOutputFactory).newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T readData(InputStream in) throws Exception
    {
        XMLStreamReader sr = _staxInFactory.createXMLStreamReader(in);
        Unmarshaller jaxbU = _jaxbContext.createUnmarshaller();
        // For debugging, can use this handler:
        //jaxbU.setEventHandler(new VHandler());
        Object result = jaxbU.unmarshal(sr);
        sr.close();
        return (T) result;
    }
    
    @Override
    public int writeData(OutputStream out, T data) throws Exception
    {
        XMLStreamWriter sw = _staxOutFactory.createXMLStreamWriter(out, "UTF-8");
        Marshaller jaxbM = _jaxbContext.createMarshaller();
        jaxbM.marshal(data, sw);
        sw.close();
        return -1;
    }
}
