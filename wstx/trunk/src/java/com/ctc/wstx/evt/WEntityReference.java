package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import org.codehaus.stax2.ri.evt.EntityReferenceEventImpl;

import org.codehaus.stax2.XMLStreamWriter2;

import com.ctc.wstx.ent.EntityDecl;

/**
 * We need a slightly specialized version to support concept of
 * undeclared entities, which can be used in (non-default, non-standard)
 * mode where undeclared entities are allowed to be handled.
 */
public class WEntityReference
    extends EntityReferenceEventImpl
    implements EntityReference
{
    final String mName;

    public WEntityReference(Location loc, EntityDeclaration decl)
    {
        super(loc, decl);
        mName = null;
    }

    /**
     * This constructor gets called for undeclared/defined entities: we will
     * still know the name (from the reference), but not how it's defined
     * (since it is not defined).
     */
    public WEntityReference(Location loc, String name)
    {
        super(loc, (EntityDeclaration) null);
        mName = name;
    }

    public String getName()
    {
        if (mName != null) {
            return mName;
        }
        return super.getName();
    }
}
