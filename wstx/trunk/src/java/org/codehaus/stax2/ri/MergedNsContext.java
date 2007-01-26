package org.codehaus.stax2.ri;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

/**
 * Helper class used to combine an enclosing namespace context with
 * a list of namespace declarations contained, to result in a single
 * namespace context object.
 */
public class MergedNsContext
    implements NamespaceContext
{
    final NamespaceContext mParentCtxt;

    /**
     * List of {@link Namespace} instances.
     */
    final List mNamespaces;

    protected MergedNsContext(NamespaceContext parentCtxt, List localNs)
    {
        mParentCtxt = parentCtxt;
        mNamespaces = (localNs == null) ? Collections.EMPTY_LIST : localNs;
    }

    public static MergedNsContext construct(NamespaceContext parentCtxt,
                                            List localNs)
    {
        return new MergedNsContext(parentCtxt, localNs);
    }

    /*
    /////////////////////////////////////////////
    // NamespaceContext API
    /////////////////////////////////////////////
     */

    public String getNamespaceURI(String prefix)
    {
        // !!! TBI: traverse over the list

        return null;
    }

    public String getPrefix(String nsURI)
    {
        // !!! TBI

        return null;
    }

    public Iterator getPrefixes(String nsURI)
    {
        // !!! TBI

        return null;
    }
}
