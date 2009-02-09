package org.codehaus.staxbind.jsoncount;

import java.io.*;
import java.util.*;

import com.sdicons.json.model.*;
import com.sdicons.json.parser.JSONParser;

/**
 * Test driver that uses the "Json Tools" parser, from
 * [http://jsontools.berlios.de/]
 */
public final class JsonToolsDriver
    extends JsonCountDriver
{
    public JsonToolsDriver() { }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        /* Good: this parser at least takes in an InputStream,
         * unlike some other candidates
         */
        JSONParser jp = new JSONParser(new ByteArrayInputStream(docData));
        JSONValue v = jp.nextValue();
        _processNode(v, results);
    }

    private void _processNode(JSONValue node, CountResult results)
        throws Exception
    {
        if (node.isObject()) {
            _processObject((JSONObject) node, results);
        } else if (node.isArray()) {
            _processArray((JSONArray) node, results);
        }
    }

    private void _processArray(JSONArray array, CountResult results)
        throws Exception
    {
        for (int i = 0, len = array.size(); i < len; ++i) {
            _processNode(array.get(i), results);
        }
    }

    private void _processObject(JSONObject object, CountResult results)
        throws Exception
    {
        /* This is bit clumsy: why is Object not a Map; or at least
         * expose decent set of convenience accessors?
         * But at least it's generics aware...
         */
        for (Map.Entry<String, JSONValue> en : object.getValue().entrySet()) {
            String fn = en.getKey();
            results.addReference(fn);
            _processNode(en.getValue(), results);
        }
    }
}
