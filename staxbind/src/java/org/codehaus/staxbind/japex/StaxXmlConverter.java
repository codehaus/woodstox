package org.codehaus.staxbind.dbconv;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

public class StaxXmlConverter
    extends DbConverter
{
    final XMLInputFactory _staxInFactory;
    final XMLOutputFactory _staxOutFactory;

    public StaxXmlConverter(String infClass, String outfClass)
    {
        try {
            _staxInFactory = (XMLInputFactory) Class.forName(infClass).newInstance();
            _staxOutFactory = (XMLOutputFactory) Class.forName(outfClass).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DbData readData(InputStream in) throws Exception
    {
        return null;
    }

    public int writeData(OutputStream out, DbData data) throws Exception
    {
        return -1;
    }
}
