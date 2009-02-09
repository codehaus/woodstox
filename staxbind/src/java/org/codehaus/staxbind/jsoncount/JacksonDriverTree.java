package org.codehaus.staxbind.jsoncount;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public final class JacksonDriverTree
    extends JsonCountDriver
{
    final TreeMapper _mapper;

    public JacksonDriverTree() {
        _mapper = new TreeMapper();
    }

    protected void read(byte[] docData, CountResult results)
        throws Exception
    {
        JsonNode root = _mapper.readTree(docData);
        _processNode(root, results);
    }

    private void _processNode(JsonNode node, CountResult results)
    {
        if (node.isObject()) {
            _countObjectFields(node, results);
        } else if (node.isArray()) {
            _iterateArrayElements(node, results);
        }
    }

    private void  _countObjectFields(JsonNode objNode, CountResult results)
    {
        // First: denote field names
        Iterator<String> it = objNode.getFieldNames();
        while (it.hasNext()) {
            results.addReference(it.next());
        }
        // Then descent on values, to find sub-maps
        Iterator<JsonNode> nodeIt = objNode.getFieldValues();
        while (nodeIt.hasNext()) {
            _processNode(nodeIt.next(), results);
        }
    }

    private void _iterateArrayElements(JsonNode objNode, CountResult results)
    {
        for (JsonNode n : objNode) {
            _processNode(n, results);
        }
    }
}
