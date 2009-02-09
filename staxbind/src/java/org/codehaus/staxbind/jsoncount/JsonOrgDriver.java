package org.codehaus.staxbind.jsoncount;

import java.io.*;
import java.util.*;

import org.json.*;

/**
 * Test driver that uses the "standard" Json.org driver, from
 * [http://www.json.org/]
 */
public final class JsonOrgDriver
    extends JsonCountDriver
{
    public JsonOrgDriver() { }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        /* Doh. Stupid driver doesn't allow taking in of input
         * streams...
         */
        Reader r = new InputStreamReader(new ByteArrayInputStream(docData), "UTF-8");
        JSONTokener t = new JSONTokener(r);
        Object ob = t.nextValue();
        r.close();
        _processNode(ob, results);
    }

    private void _processNode(Object node, CountResult results)
        throws JSONException
    {
        if (node instanceof JSONArray) {
            _processArray((JSONArray) node, results);
        } else if (node instanceof JSONObject) {
            _processObject((JSONObject) node, results);
        }
    }

    private void _processArray(JSONArray array, CountResult results)
        throws JSONException
    {
        for (int i = 0, len = array.length(); i < len; ++i) {
            if (!array.isNull(i)) {
                _processNode(array.get(i), results);
            }
        }
    }

    private void _processObject(JSONObject object, CountResult results)
        throws JSONException
    {
        Iterator<?> it = object.keys();
        while (it.hasNext()) {
            String fn = (String) it.next();
            results.addReference(fn);
            /* Hmmh. There should be a way to access these more
             * efficiently... but can't see one.
             */
            _processNode(object.get(fn), results);
        }
    }
}
