package com.ctc.wstx.stax.io;

import java.io.IOException;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

/**
 * Interface that defines how input (replacement text for external entities,
 * DTD external subset) is to be resolved to be usable by Wstx.
 *<p>
 * Implementations usually use {@link com.ctc.wstx.stax.io.InputSourceFactory}
 * to create instances returned.
 */
public interface WstxInputResolver
{
  /**
   * Method that gets called by Wstx reader when it needs to resolve an
   * external entity. If method returns an input source, it's used for
   * accessing resource; if it returns null, reader is to try to resolve
   * it by itself.
   *
   * @param refCtxt Input source that produced input that contained reference
   *  to the resource being resolved.
   * @param entityId Name/id of the entity being expanded, if this is an
   *   entity expansion; null otherwise (for example, when resolving external
   *   subset).
   * @param publicId Public identifier of the resource, if any; may be null
   *   or empty String to indicate missing public identifier. but only if
   *   systemId is not.
   * @param systemId System identifier of the resource, if any; may be null
   *   or empty String, but only if public id is not.
   * @param assumedLocation Location Reader has derived from current
   *   context information and which default resolver would use. Can be
   *   used or ignored by resolvers.
   *
   * @return Resolved input source, or null to indicate resolver couldn't
   *   resolve it and caller needs to resolve it using some other mechanism.
   */
  public WstxInputSource resolveReference(WstxInputSource refCtxt, String entityId,
                                          String publicId, String systemId,
                                          URL assumedLocation)
      throws IOException, XMLStreamException;
}
