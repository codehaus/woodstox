package org.codehaus.staxbind.dbconv;

import java.io.*;
import javax.xml.bind.*;
import javax.xml.stream.*;

/**
 * Converter that uses JAXB2 on top of regular Stax 1
 * implementation (such as Woodstox).
 */
public class JaxbConverter
    extends DbConverter
{
    final XMLInputFactory _staxInFactory;
    final XMLOutputFactory _staxOutFactory;

    final JAXBContext _jaxbContext;

    public JaxbConverter()
        throws Exception
    {
        _jaxbContext = JAXBContext.newInstance(DbData.class);
        try {
            _staxInFactory = (XMLInputFactory) Class.forName(WSTX_INPUT_FACTORY).newInstance();
            _staxOutFactory = (XMLOutputFactory) Class.forName(WSTX_OUTPUT_FACTORY).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DbData readData(InputStream in) throws Exception
    {
        XMLStreamReader sr = _staxInFactory.createXMLStreamReader(in);
        Unmarshaller jaxbU = _jaxbContext.createUnmarshaller();
        // For debugging, can use this handler:
        //jaxbU.setEventHandler(new VHandler());
        DbData result = (DbData)jaxbU.unmarshal(sr);
        sr.close();
        return result;
    }
    
    @Override
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        XMLStreamWriter sw = _staxOutFactory.createXMLStreamWriter(out, "UTF-8");
        Marshaller jaxbM = _jaxbContext.createMarshaller();
        jaxbM.marshal(data, sw);
        sw.close();
        return data.size();
    }
}
