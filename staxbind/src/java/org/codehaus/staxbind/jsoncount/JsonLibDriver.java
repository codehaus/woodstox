package org.codehaus.staxbind.jsoncount;

import java.io.*;
import java.util.*;

import net.sf.json.*;

/**
 * Test driver that uses "Json-lib" parser, from
 * [http://json-lib.sourceforge.net/].
 */
public final class JsonLibDriver
    extends JsonCountDriver
{
    public JsonLibDriver() { }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        /* Ugh. If you think Json.org's input handling is not
         * particularly good, welcome to hell: this is braindead...
         * no way to use InputStream or Reader? WTF?
         */
        /* Worse: we can't test anything with JSONArray at
         * main-level, since there doesn't seem to be a flexible
         * way of parsing 'anything' -- must specify whether it
         * is an array or object.
         */
        String doc = new String(docData, "UTF-8");
        JSONObject job = JSONObject.fromObject(doc);
        _processObject(job, results);
    }

    private void _processNode(JSON node, CountResult results)
    {
        if (node instanceof JSONArray) {
            _processArray((JSONArray) node, results);
        } else if (node instanceof JSONObject) {
            _processObject((JSONObject) node, results);
        }
    }

    private void _processArray(JSONArray array, CountResult results)
    {
        for (Object value : array) {
            _processNode((JSON) value, results);
        }
    }

    private void _processObject(JSONObject object, CountResult results)
    {
        // neat that it's a real Map, too bad it has no generics...
        for (Object entryOb : object.entrySet()) {
            Map.Entry<?,?> en = (Map.Entry<?,?>) entryOb;
            String fn = (String) en.getKey();
            results.addReference(fn);
            // Ugh: not all values are of type JSON... like Strings
            Object value = en.getValue();
            if (value instanceof JSON) {
                _processNode((JSON) value, results);
            }
        }
    }
}
