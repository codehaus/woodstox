package org.codehaus.staxbind.std;

import java.io.*;

/**
 * Base class for serializable objects used for data binding tests.
 *<p>
 * Note: we mark it as Serializable since many converters require
 * this; and this way sub-classes need not add marker.
 */
public abstract class StdItem<T extends StdItem>
    implements java.io.Serializable // hessian needs this, as does jdk serializer (if we use it)
{
    protected StdItem() { }

    /*
    ///////////////////////////////////////////////////
    // Std methods, abstracts added
    ///////////////////////////////////////////////////
     */

    @Override
        public final boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        /* Here we assume final classes, and comparisons only
         * between leaf (final) classes
         */
        if (o.getClass() != getClass()) return false;
        return _equals((T) o);
    }

    public abstract boolean _equals(T other);

    public abstract String toString();
}
