package org.codehaus.staxbind.dbconv;

import java.util.*;

/**
 * Simple bean class to contain one row-full of data that is part
 * of {@link DbData} data set.
 */
public final class DbRow
{
    public enum Field {
        id {
            public void setValue(DbRow row, String value) { row.setId(Long.parseLong(value)); }
        }
        ,firstname {
            public void setValue(DbRow row, String value) { row.setFirstname(value); }
        }
        ,lastname {
            public void setValue(DbRow row, String value) { row.setLastname(value); }
        }
        ,zip {
            public void setValue(DbRow row, String value) { row.setZip(Integer.parseInt(value)); }
        }
        ,street {
            public void setValue(DbRow row, String value) { row.setStreet(value); }
        }
        ,city {
            public void setValue(DbRow row, String value) { row.setCity(value); }
        }
        ,state {
            public void setValue(DbRow row, String value) { row.setState(value); }
        }
        ;

        private Field() { }

        public abstract void setValue(DbRow row, String value);
    }

    final static HashMap<String, Field> _fields = new HashMap<String,Field>();
    static {
        for (Field f : Field.values()) {
            _fields.put(f.name(), f);
        }
    }

    long _id;

    String _firstname, _lastname;
    int _zip;
    String _street, _city, _state;

    public DbRow() { }

    /*
    ///////////////////////////////////////////////////
    // Static helper methods
    ///////////////////////////////////////////////////
     */

    public static Field fieldByName(String name)
    {
        return _fields.get(name);
    }

    /*
    ///////////////////////////////////////////////////
    // Mutators
    ///////////////////////////////////////////////////
     */

    public void setId(long v) { _id = v; }
    public void setZip(int v) { _zip = v; }

    public void setFirstname(String v) { _firstname = v; }
    public void setLastname(String v) { _lastname = v; }
    public void setStreet(String v) { _street = v; }
    public void setCity(String v) { _city = v; }
    public void setState(String v) { _state = v; }

    /**
     * Non-type-safe method that can be used for convenient by-name
     * assignmend
     */
    public boolean assign(String fieldName, String valueStr)
    {
        Field f = _fields.get(fieldName);
        if (f != null) {
            f.setValue(this, valueStr);
            return true;
        }
        return false;
    }

    /*
    ///////////////////////////////////////////////////
    // Accessors
    ///////////////////////////////////////////////////
     */

    public long getId() { return _id; }
    public int getZip() { return _zip; }

    public String getFirstname() { return _firstname; }
    public String getLastname() { return _lastname; }
    public String getStreet() { return _street; }
    public String getCity() { return _city; }
    public String getState() { return _state; }

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

        DbRow other = DbRow.class.cast(o);
        return (this._id == other._id)
            && (this._zip == other._zip)
            && (this._firstname.equals(other._firstname))
            && (this._lastname.equals(other._lastname))
            && (this._street.equals(other._street))
            && (this._city.equals(other._city))
            && (this._state.equals(other._state))
            ;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[DBRow:");
        sb.append(" id: ").append(_id);
        sb.append(" name: ").append(_firstname).append(' ').append(_lastname);
        sb.append(" address: ");
        sb.append(_street).append(' ').append(_city).append(' ').append(_state);
        sb.append(' ').append(_zip);
        sb.append("]");
        return sb.toString();
    }
}
