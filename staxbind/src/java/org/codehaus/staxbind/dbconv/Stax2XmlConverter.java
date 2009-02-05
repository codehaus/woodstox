package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

// stax2 api:
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Converter that uses Stax2 extension API (v3+) and specifically
 * its Typed Access API for more optimal data binding
 */
public class Stax2XmlConverter
    extends DbConverter
{
    XMLInputFactory2 _staxInFactory;
    XMLOutputFactory2 _staxOutFactory;

    public Stax2XmlConverter() { }

    public void initStax2(String infName, String outfName)
    {
        try {
            _staxInFactory = (XMLInputFactory2) Class.forName(infName).newInstance();
            _staxOutFactory = (XMLOutputFactory2) Class.forName(outfName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DbData readData(InputStream in)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = (XMLStreamReader2) _staxInFactory.createXMLStreamReader(in);
        DbData result = new DbData();

        sr.nextTag();
        expectTag(FIELD_TABLE, sr);

        try {
            while (sr.nextTag() == XMLStreamReader.START_ELEMENT) {
                result.addRow(readRow(sr));
            }
        } catch (IllegalArgumentException iae) {
            throw new XMLStreamException("Data problem: "+iae.getMessage(), sr.getLocation());
        }

        sr.close();
        return result;
    }

    private final DbRow readRow(XMLStreamReader2 sr)
        throws XMLStreamException
    {
        expectTag(FIELD_ROW, sr);
        DbRow row = new DbRow();
        while (sr.nextTag() == XMLStreamReader.START_ELEMENT) {
            String elemName = sr.getLocalName();
            DbRow.Field f = DbRow.fieldByName(elemName);
            if (f == null) { // sanity check
                throw new XMLStreamException("Unexpected element <"+elemName+">: not one of recognized field names");
            }
            switch (f) {
            case id:
                row.setId(sr.getElementAsLong());
                break;
            case firstname:
                row.setFirstname(sr.getElementText());
                break;
            case lastname:
                row.setLastname(sr.getElementText());
                break;
            case zip:
                row.setZip(sr.getElementAsInt());
                break;
            case street:
                row.setStreet(sr.getElementText());
                break;
            case city:
                row.setCity(sr.getElementText());
                break;
            case state:
                row.setState(sr.getElementText());
                break;
            }
        }
        return row;
    }
    
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        XMLStreamWriter2 sw = (XMLStreamWriter2) _staxOutFactory.createXMLStreamWriter(out, "UTF-8");
        sw.writeStartDocument();
        sw.writeStartElement(FIELD_TABLE);
        Iterator<DbRow> it = data.rows();

        while (it.hasNext()) {
            DbRow row = it.next();
            sw.writeStartElement(FIELD_ROW); // <row>

            sw.writeStartElement(DbRow.Field.id.name());
            sw.writeLong(row.getId());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.firstname.name());
            sw.writeCharacters(row.getFirstname());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.lastname.name());
            sw.writeCharacters(row.getLastname());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.zip.name());
            sw.writeInt(row.getZip());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.street.name());
            sw.writeCharacters(row.getStreet());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.city.name());
            sw.writeCharacters(row.getCity());
            sw.writeEndElement();

            sw.writeStartElement(DbRow.Field.state.name());
            sw.writeCharacters(row.getState());
            sw.writeEndElement();

            sw.writeEndElement(); // </row>
        }
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
        return -1;
    }

    private final void expectTag(String expElem, XMLStreamReader sr)
        throws XMLStreamException
    {
        if (!expElem.equals(sr.getLocalName())) {
            throw new XMLStreamException("Unexpected element <"+sr.getLocalName()+">: expecting <"+expElem+">", sr.getLocation());
        }
    }
}
