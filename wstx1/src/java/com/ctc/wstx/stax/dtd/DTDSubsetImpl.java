/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax.dtd;

import java.text.MessageFormat;
import java.util.*;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.stax.cfg.ErrorConsts;
import com.ctc.wstx.stax.evt.WNotationDeclaration;
import com.ctc.wstx.stax.exc.WstxException;
import com.ctc.wstx.stax.exc.WstxParsingException;
import com.ctc.wstx.util.DataUtil;

/**
 * The default implementation of {@link DTDSubset}
 */
public final class DTDSubsetImpl
    extends DTDSubset
{
    /**
     * Whether this subset is cachable. In general, only those external
     * subsets that do not refer to PEs defined by internal subsets are
     * cachable.
     */
    final boolean mIsCachable;

    /*
    //////////////////////////////////////////////////////
    // Entity information
    //////////////////////////////////////////////////////
     */

    /**
     * Map (name-to-EntityDecl) of general entity declarations (internal,
     * external) for this DTD subset.
     */
    final Map mGeneralEntities;

    /**
     * Lazily instantiated List that contains all notations from
     * {@link #mGeneralEntities} (preferably in their declaration order; depends
     * on whether platform, ie. JDK version, has insertion-ordered
     * Maps available), used by DTD event Objects.
     */
    volatile transient List mGeneralEntityList = null;

    // // // Parameter entity info:

    /**
     * Map (name-to-WEntityDeclaration) that contains all parameter entities
     * defined by this subset. May be empty if such information will not be
     * needed for use; for example, external subset's definitions are needed,
     * nor are combined DTD set's.
     */
    final Map mDefinedPEs;

    /**
     * Set of names of parameter entities references by this subset. Needed
     * when determinining if external subset materially depends on definitions
     * from internal subset, which is needed to know when caching external
     * subsets.
     */
    final Set mReferencedPEs;


    /*
    //////////////////////////////////////////////////////
    // Notation definitions:
    //////////////////////////////////////////////////////
     */

    /**
     * Map (name-to-WNotationDeclaration) that this subset has defined.
     */
    final Map mNotations;

    /**
     * Lazily instantiated List that contains all notations from
     * {@link #mNotations} (preferably in their declaration order; depends
     * on whether platform, ie. JDK version, has insertion-ordered
     * Maps available), used by DTD event Objects.
     */
    volatile transient List mNotationList = null;


    /*
    //////////////////////////////////////////////////////
    // Element definitions:
    //////////////////////////////////////////////////////
     */


    final Map mElements;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    private DTDSubsetImpl(boolean cachable, Map genEnt,
                          Map paramEnt, Set peRefs,
                          Map notations, Map elements)
    {
        mIsCachable = cachable;
        mGeneralEntities = genEnt;
        mDefinedPEs = paramEnt;
        mReferencedPEs = peRefs;
        mNotations = notations;
        mElements = elements;
    }

    public static DTDSubsetImpl constructInstance(boolean cachable,
                                                  Map genEnt,
                                                  Map paramEnt, Set refdPEs,
                                                  Map notations,
                                                  Map elements)
    {
        return new DTDSubsetImpl(cachable, genEnt, paramEnt, refdPEs,
                                 notations, elements);
    }

    /**
     * Method that will combine definitions from internal and external subsets,
     * producing a single DTD set.
     */
    public DTDSubset combineWithExternalSubset(DTDSubset extSubset, XMLReporter rep)
        throws WstxException
    {
        /* First let's see if we can just reuse GE Map used by int or ext
         * subset; (if only one has contents), or if not, combine them.
         */
        Map ge1 = getGeneralEntityMap();
        Map ge2 = extSubset.getGeneralEntityMap();
        if (ge1 == null || ge1.isEmpty()) {
            ge1 = ge2;
        } else {
            if (ge2 != null && !ge2.isEmpty()) {
                /* Internal subset Objects are never shared or reused (and by
                 * extension, neither are objects they contain), so we can just
                 * modify GE map if necessary
                 */
                combineMaps(ge1, ge2);
            }
        }

        // Ok, then, let's combine notations similarly
        Map n1 = getNotationMap();
        Map n2 = extSubset.getNotationMap();
        if (n1 == null || n1.isEmpty()) {
            n1 = n2;
        } else {
            if (n2 != null && !n2.isEmpty()) {
                /* First; let's make sure there are no colliding notation
                 * definitions: it's an error to try to redefine notations.
                 */
                checkNotations(n1, n2);

                /* Internal subset Objects are never shared or reused (and by
                 * extension, neither are objects they contain), so we can just
                 * modify notation map if necessary
                 */
                combineMaps(n1, n2);
            }
        }


        // And finally elements, rather similarly:
        Map e1 = getElementMap();
        Map e2 = extSubset.getElementMap();
        if (e1 == null || e1.isEmpty()) {
            e1 = e2;
        } else {
            if (e2 != null && !e2.isEmpty()) {
                /* Internal subset Objects are never shared or reused (and by
                 * extension, neither are objects they contain), so we can just
                 * modify element map if necessary
                 */
                combineElements(e1, e2, rep);
            }
        }

        /* Combos are not cachable, and because of that, there's no point
         * in storing any PE info either.
         */
        return constructInstance(false, ge1, null, null, n1, e1);
    }

    /*
    //////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////
     */

    public boolean isCachable() {
        return mIsCachable;
    }
    
    public Map getGeneralEntityMap() {
        return mGeneralEntities;
    }

    public List getGeneralEntityList()
    {
        List l = mGeneralEntityList;
        if (l == null) {
            if (mGeneralEntities == null || mGeneralEntities.size() == 0) {
                l = JdkFeatures.getInstance().getEmptyList();
            } else {
                l = Collections.unmodifiableList(new ArrayList(mGeneralEntities.values()));
            }
            mGeneralEntityList = l;
        }

        return l;
    }

    public Map getParameterEntityMap() {
        return mDefinedPEs;
    }

    public Map getNotationMap() {
        return mNotations;
    }

    public List getNotationList()
    {
        List l = mNotationList;
        if (l == null) {
            if (mNotations == null || mNotations.size() == 0) {
                l = JdkFeatures.getInstance().getEmptyList();
            } else {
                l = Collections.unmodifiableList(new ArrayList(mNotations.values()));
            }
            mNotationList = l;
        }

        return l;
    }

    public Map getElementMap() {
        return mElements;
    }

    /**
     * Method used in determining whether cached external subset instance
     * can be used with specified internal subset. If ext. subset references
     * any parameter entities int subset (re-)defines, it can not; otherwise
     * it can be used.
     *
     * @return True if this (external) subset refers to a parameter entity
     *    defined in passed-in internal subset.
     */
    public boolean isReusableWith(DTDSubset intSubset)
    {
        Set refdPEs = mReferencedPEs;

        if (refdPEs != null && refdPEs.size() > 0) {
            Map intPEs = intSubset.getParameterEntityMap();
            if (intPEs != null && intPEs.size() > 0) {
                if (DataUtil.anyValuesInCommon(refdPEs, intPEs.keySet())) {
                    return false;
                }
            }
        }
        return true; // yep, no dependencies overridden
    }

    /*
    //////////////////////////////////////////////////////
    // Overridden default methods:
    //////////////////////////////////////////////////////
     */

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[DTDSubset: ");
        int count = (mGeneralEntities == null) ? 0 : mGeneralEntities.size();
        sb.append(count);
        sb.append(" general entities");
        sb.append(']');
        return sb.toString();
    }

    /*
    //////////////////////////////////////////////////////
    // Convenience methods used by other classes
    //////////////////////////////////////////////////////
     */

   public static void throwNotationException(WNotationDeclaration oldDecl, WNotationDeclaration newDecl)
        throws WstxException
    {
        throw new WstxParsingException
            ("Trying to redefine notation '"
             +newDecl.getName()+"' (originally defined at "+oldDecl.getLocation()+")",
             newDecl.getLocation());
    }

   public static void throwElementException(DTDElement oldElem, Location loc)
        throws WstxException
    {
        throw new WstxParsingException
            ("Trying to redefine element '"
             +oldElem.getDisplayName()+"' (originally defined at "+oldElem.getLocation()+")",
             loc);
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
     */

    private static void combineMaps(Map m1, Map m2)
    {
        Iterator it = m2.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            Object key = me.getKey();
            /* Int. subset has precedence, but let's guess most of
             * the time there are no collisions:
             */
            Object old = m1.put(key, me.getValue());
            // Oops, got value! Let's put it back
            if (old != null) {
                m1.put(key, old);
            }
        }
    }

    /**
     * Method that will try to merge in elements defined in the external
     * subset, into internal subset; it will also check for redeclarations
     * when doing this, as it's illegal to redeclare elements. Care has to
     * be taken to only check actual redeclarations: placeholders should
     * not cause problems.
     */
    private static void combineElements(Map intElems, Map extElems,
                                        XMLReporter rep)
        throws WstxException
    {
        Iterator it = extElems.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry) it.next();
            Object key = me.getKey();
            Object extVal = me.getValue();
            Object oldVal = intElems.put(key, extVal);

            // If there was no old value, can just merge new one in and continue
            if (oldVal == null) {
                continue;
            }

            DTDElement extElem = (DTDElement) extVal;
            DTDElement intElem = (DTDElement) oldVal;

            // First: it's illegal to have a full redeclaration:
            if (extElem.isDefined()) {
                if (intElem.isDefined()) {
                    throwElementException(intElem, extElem.getLocation());
                }
            } else if (!intElem.isDefined()) {
                /* ??? Should we warn about neither of them being really
                 *   declared?
                 */
                if (rep != null) {
                    try {
                        rep.report(MessageFormat.format(ErrorConsts.W_UNDEFINED_ELEM,
                                                        new String[] { extElem.getDisplayName() }),
                                   ErrorConsts.WT_ENT_DECL,
                                   intElem, intElem.getLocation());
                    } catch (XMLStreamException foo) { // should never happen
                        throw new Error(foo);
                    }
                }
            }

            /* Now, need to add external subset attributes internal one is
             * missing, and put that entry back in the map
             */
            intElem.mergeMissingAttributesFrom(extElem);
            intElems.put(key, intElem);
        }
    }

    private static void checkNotations(Map fromInt, Map fromExt)
        throws WstxException
    {
        /* Since it's external subset that would try to redefine things
         * defined in internal subset, let's traverse definitions in
         * the ext. subset first (even though that may not be the fastest
         * way), so that we have a chance of catching the first problem
         * (As long as Maps iterate in insertion order).
         */
        Iterator it = fromExt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry en = (Map.Entry) it.next();
            if (fromInt.containsKey(en.getKey())) {
                throwNotationException((WNotationDeclaration) fromInt.get(en.getKey()),
                                       (WNotationDeclaration) en.getValue());
            }
        }
    }
}
