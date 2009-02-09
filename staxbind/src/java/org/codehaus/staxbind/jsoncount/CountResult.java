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

    public int size() { return _countByName.size(); }

    @Override
        public String toString()
    {
        return _countByName.toString();
    }

    public TreeMap<String,int[]> getCounts() {
        return new TreeMap<String,int[]>(_countByName);
    }

    /*
    @Override
        public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        CountResult other = (CountResult) o;
        return other._countByName.equals(_countByName);
    }
    */
}

