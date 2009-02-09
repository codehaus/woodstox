package org.codehaus.staxbind.jsoncount;

import java.util.*;

/**
 * Simple helper class that contains statistics from processed
 * json document. Used to abstract away details of collected
 * results.
 */
public final class CountResult
{
    final HashMap<String,int[]> _countByName = new HashMap<String,int[]>();

    public CountResult() { }

    public void addReference(String name)
    {
        int[] counts = _countByName.get(name);
        if (counts == null) {
            counts = new int[1];
            _countByName.put(name, counts);
        }
        ++counts[0];
    }
}

