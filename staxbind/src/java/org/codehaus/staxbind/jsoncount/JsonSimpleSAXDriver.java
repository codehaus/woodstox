package org.codehaus.staxbind.jsoncount;

import java.io.*;

import org.json.simple.parser.*;

/**
 * Test driver that uses the "Json-simple" parser, from
 * [http://code.google.com/p/json-simple/] using its
 * SAX-like API (alternative to tree model)
 */
public final class JsonSimpleSAXDriver
    extends JsonCountDriver
            implements ContentHandler
{
    final JSONParser _parser;

    CountResult _results;

    public JsonSimpleSAXDriver() {
        _parser = new JSONParser();
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        /* Hmmh. Should really take an InputStream, but at least
         * we can give a Reader...
         */
        Reader r = new InputStreamReader(new ByteArrayInputStream(docData), "UTF-8");
        _results = results;
        _parser.parse(r, this, false);
    }

    /*
    /////////////////////////////////////////////
    // ContentHandler implementation
    /////////////////////////////////////////////
     */

    public void startJSON() throws ParseException, IOException { }

    public void endJSON() throws ParseException, IOException { }

    public boolean startObject() throws ParseException, IOException { return true; }

    public boolean endObject() throws ParseException, IOException { return true; }

    public boolean startObjectEntry(String key) throws ParseException, IOException
    {
        _results.addReference(key);
        return true;
    }

    public boolean endObjectEntry() throws ParseException, IOException { return true; }

    public boolean startArray() throws ParseException, IOException { return true; }

    public boolean endArray() throws ParseException, IOException { return true; }

    public boolean primitive(Object value) throws ParseException, IOException { return true; }
}
