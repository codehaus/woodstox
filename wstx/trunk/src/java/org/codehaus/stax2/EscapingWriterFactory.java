package org.codehaus.stax2;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface that defines API for the factories stream writers use for
 * creating "escaping writers". These factories are used when defining
 * custom escaping of text (as well as possibly used by the
 * implementations for default escaping too). Escaping in this context
 * refers to the process of replacing individual text/attribute content
 * character with pre-defined and character entities, as per XML
 * specification (2.4, Appendix D).
 *<p>
 * Typical escaping writers replace characters like '&lt;' and '&amp;',
 * as well as some additional characters depending on context.
 * Custom implementations may choose to escape additional characters,
 * for example to make it easier to manually view or edit resulting
 * serialized XML document.
 *<p>
 * Note about implementing escaping writers: writers need to obey normal
 * Writer semantics, and specifically they should pass calls to
 * <code>flush()</code> and <code>close()</code> to the underlying
 * Writer.
 */
public interface EscapingWriterFactory
{
    /**
     * Method called to create an instance of escaping writer that
     * will output to the specified stream.
     *
     * @param w Underlying writer that the encoding writer should
     *    output
     */
    public Writer createEscapingWriterFor(Writer w);
}
