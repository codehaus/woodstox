package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.JavaTypeMapper;

/**
 * Converter that uses Jackson JSON processor for data binding,
 * using automatic bindings where they are available (as of now
 * (Dec-2008) just for deserialization)
 */
public class JacksonConverterAutomatic
    extends DbConverter
{
    final JsonFactory _jsonFactory;

    final JavaTypeMapper _mapper;

    public JacksonConverterAutomatic()
    {
        _jsonFactory = new JsonFactory();
        _mapper = new JavaTypeMapper();
    }

    public DbData readData(InputStream in)
        throws IOException
    {
        JsonParser jp = _jsonFactory.createJsonParser(in);
        DbData result = new DbData();

        /* Note: when using automatic bindings, we'll get Object
         * at main level, not array like with manual
         */

        if (jp.nextToken() != JsonToken.START_OBJECT) { // data validity check
            throw new IOException("Expected data to be an Object");
        }

        // With just one field:
        if (jp.nextToken() != JsonToken.FIELD_NAME) { // data validity check
            throw new IOException("Expected a single field");
        }
        if (!"row".equals(jp.getCurrentName())) {
            throw new IOException("Expected field 'row', got '"+jp.getCurrentName()+"'");
        }
        // Of type array...
        if (jp.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected type START_ARRAY for 'row' field, got "+jp);
        }

        JsonToken t;
        
        while ((t = jp.nextToken()) != JsonToken.END_ARRAY) {
            if (t != JsonToken.START_OBJECT) { // sanity check, must be this
                throw new IOException("Broken JSON doc: got "+t);
            }
            DbRow row = new DbRow();
            while ((t = jp.nextToken()) != JsonToken.END_OBJECT) {
                if (t != JsonToken.FIELD_NAME) { // sanity check, must be this
                    throw new IOException("Broken JSON doc: got "+t);
                }
                String fn = jp.getCurrentName();
                // Let's move to value
                jp.nextToken();
                DbRow.Field f = DbRow.fieldByName(fn);
                if (f == null) { // sanity check
                    throw new IOException("Unexpected field '"+fn+"': not one of recognized field names");
                }
                switch (f) {
                case id:
                    row.setId(jp.getLongValue());
                    break;
                case firstname:
                    row.setFirstname(jp.getText());
                    break;
                case lastname:
                    row.setLastname(jp.getText());
                    break;
                case zip:
                    row.setZip(jp.getIntValue());
                    break;
                case street:
                    row.setStreet(jp.getText());
                    break;
                case city:
                    row.setCity(jp.getText());
                    break;
                case state:
                    row.setState(jp.getText());
                    break;
                }
            }
            result.addRow(row);
        }

        jp.close();
        return result;
    }
    
    public int writeData(OutputStream out, DbData data) throws Exception
    {
        JsonGenerator jg = _jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        _mapper.writeValue(jg, data);
        jg.close();
        return -1;
    }
}
