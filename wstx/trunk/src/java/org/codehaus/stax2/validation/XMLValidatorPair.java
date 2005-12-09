package org.codehaus.stax2.validation;

/**
 * Simple utility class that allows chaining of {@link XMLValidator}
 * instances. Since the class itself implements {@link XMLValidator},
 * multiple validators can be added by chaining these pairs; ordering
 * of validator calls depends on ordering of the pairs.
 *<p>
 * Default semantics are quite simple: first validator of the pair is
 * always called first, and results as/if modified by that validator
 * are passed on to the second validator.
 *<p>
 * It is expected that this class is mostly used by actual stream reader
 * and writer implementations; not so much by validator implementations.
 */
public class XMLValidatorPair
    extends XMLValidator
{
    public final static String ATTR_TYPE_DEFAULT = "CDATA";

    protected XMLValidator mFirst, mSecond;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public XMLValidatorPair(XMLValidator first, XMLValidator second)
    {
        mFirst = first;
        mSecond = second;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLValidator implementation
    ////////////////////////////////////////////////////
     */

    /**
     * Two choices here; could either return schema of the first child,
     * or return null. Let's do latter, do avoid accidental matches.
     */
    public XMLValidationSchema getSchema()
    {
        return null;
    }

    public void validateElementStart(String localName, String uri,
                                     String prefix)
        throws XMLValidationException
    {
        mFirst.validateElementStart(localName, uri, prefix);
        mSecond.validateElementStart(localName, uri, prefix);
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix, String value)
        throws XMLValidationException
    {
        String retVal =  mFirst.validateAttribute(localName, uri, prefix,
                                                  value);
        if (retVal != null) {
            value = retVal;
        }
        return mSecond.validateAttribute(localName, uri, prefix, value);
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix,
                                    char[] valueChars, int valueStart,
                                    int valueEnd)
        throws XMLValidationException
    {
        String retVal =  mFirst.validateAttribute(localName, uri, prefix,
                                                  valueChars, valueStart, valueEnd);
        /* If it got normalized, let's pass normalized value to the second
         * validator
         */
        if (retVal != null) {
            return mSecond.validateAttribute(localName, uri, prefix, retVal);
        }
        // Otherwise the original
        return mSecond.validateAttribute(localName, uri, prefix,
                                         valueChars, valueStart, valueEnd);
    }

    public int validateElementAndAttributes()
        throws XMLValidationException
    {
        int textType1 = mFirst.validateElementAndAttributes();
        int textType2 = mSecond.validateElementAndAttributes();

        /* Here, let's choose the stricter (more restrictive) of the two.
         * Since constants are order from strictest to most lenient,
         * we'll just take smaller of values
         */
        return (textType1 < textType2) ? textType1 : textType2;
    }

    public int validateElementEnd(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        int textType1 = mFirst.validateElementEnd(localName, uri, prefix);
        int textType2 = mSecond.validateElementEnd(localName, uri, prefix);

        // As with earlier, let's return stricter of the two
        return (textType1 < textType2) ? textType1 : textType2;
    }

    public String getAttributeType(int index)
    {
        String type = mFirst.getAttributeType(index);
        /* Hmmh. Which heuristic to use here: obviously no answer (null or
         * empty) is not useful. But what about the default type (CDATA)?
         * We can probably find a more explicit type?
         */
        if (type == null || type.length() == 0 || type.equals(ATTR_TYPE_DEFAULT)) {
            String type2 = mSecond.getAttributeType(index);
            if (type2 != null && type2.length() > 0) {
                return type2;
            }

        }
        return type;
    }

    public int getIdAttrIndex()
    {
        int index = mFirst.getIdAttrIndex();
        if (index < 0) {
            return mSecond.getIdAttrIndex();
        }
        return index;
    }

    public int getNotationAttrIndex()
    {
        int index = mFirst.getNotationAttrIndex();
        if (index < 0) {
            return mSecond.getNotationAttrIndex();
        }
        return index;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */
}
