package org.codehaus.staxbind.dbconv;

import java.util.*;

/**
 * Simple bean class to contain all "DB data" read from data files.
 */
public final class DbData
{
    List<DbRow> _rows;

    public DbData() { }

    /*
    ///////////////////////////////////////////////////
    // Mutators
    ///////////////////////////////////////////////////
     */

    public void setRow(List<DbRow> rows) { _rows = rows; }
    public void addRow(DbRow row) { getRow().add(row); }

    /*
    ///////////////////////////////////////////////////
    // Accessors
    ///////////////////////////////////////////////////
     */

    public int size() { return (_rows == null) ? 0 : _rows.size(); }

    /**
     *<p>
     * Note: name uses singular row just to make life easier with
     * JAXB and other name convention based tools.
     */
    public List getRow()
    {
        // must return non-null for JAXB to work
        if (_rows == null) {
            _rows = new ArrayList<DbRow>(100);
        }
        return _rows;
    }

    /*
    ///////////////////////////////////////////////////
    // Std methods
    ///////////////////////////////////////////////////
     */

    @Override
        public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        DbData other = DbData.class.cast(o);
        return other.getRow().equals(getRow());
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[DBData: ");
        sb.append(getRow().size()).append(" entries]");
        return sb.toString();
    }
}

