package com.ctc.wstx.cfg;

import java.text.MessageFormat;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamConstants;

/**
 * "Static" class that contains error message constants. Note that the
 * error message constants are NOT made final; reason is that doing so
 * would make compiler inline them in other classes. Doing so would increase
 * class size (although not mem usage -- Strings do get interned), with
 * minimal performance impact.
 */
public class ErrorConsts
    implements XMLStreamConstants
{
    // // // Types of warnings we issue via XMLReporter

    public static String WT_ENT_DECL = "entity declaration";

    public static String WT_ELEM_DECL = "element declaration";
    public static String WT_ATTR_DECL = "attribute declaration";
    public static String WT_XML_DECL = "xml declaration";

    public static String W_UNDEFINED_ELEM = "Undefined element \"{0}\"; referred to by attribute(s)";
    public static String W_MIXED_ENCODINGS = "Inconsistent text encoding; declared as \"{0}\" in xml declaration, application had passed \"{1}\"";

    // // // Generic errors:

    public static String ERR_INTERNAL = "Internal error";


    // // // Wrong reader state:

    public static String ERR_STATE_NOT_STELEM = "Current state not START_ELEMENT";
    public static String ERR_STATE_NOT_ELEM = "Current state not START_ELEMENT or END_ELEMENT";

    // // // Structural problems, prolog/epilog:

    public static String ERR_DTD_IN_EPILOG = "Can not have DOCTYPE declaration in epilog";
    public static String ERR_DTD_DUP = "Duplicate DOCTYPE declaration";
    public static String ERR_CDATA_IN_EPILOG = " (CDATA not allowed in prolog/epilog)";

    // // // Illegal input:

    public static String ERR_HYPHENS_IN_COMMENT = "String '--' not allowed in comment (missing '>'?)";
    public static String ERR_BRACKET_IN_TEXT = "String ']]>' not allowed in textual content, except as the end marker of CDATA section";

    // // // Namespace problems:

    public static String ERR_NS_REDECL_XML = "Trying to redeclare prefix 'xml' from its default URI '"
                                               +XMLConstants.XML_NS_URI
                                               +"' to \"{0}\"";

    public static String ERR_NS_REDECL_XMLNS = "Trying to redeclare prefix 'xmlns' from its default URI '"
                                               +XMLConstants.XMLNS_ATTRIBUTE_NS_URI
                                               +"' to \"{0}\"";

    public static String ERR_NS_REDECL_XML_URI = "Trying to redeclare URI '"
                                               +XMLConstants.XML_NS_URI
                                               +"' from its default prefix 'xml' to \"{0}\"";

    public static String ERR_NS_REDECL_XMLNS_URI = "Trying to redeclare URI '"
                                               +XMLConstants.XMLNS_ATTRIBUTE_NS_URI
                                               +"' from its default prefix 'xmlns' to \"{0}\"";


    // // // DTD-specific:

    public static String ERR_DTD_MAINLEVEL_KEYWORD = "; expected a keyword (ATTLIST, ELEMENT, ENTITY, NOTATION), comment, or conditional section";

    public static String ERR_DTD_ATTR_TYPE = "; expected one of type (CDATA, ID, IDREF, IDREFS, ENTITY, ENTITIES NOTATION, NMTOKEN or NMTOKENS)";

    public static String ERR_DTD_DEFAULT_TYPE = "; expected #REQUIRED, #IMPLIED or #FIXED";


    // // // DTD-validation:

    public static String ERR_VLD_UNKNOWN_ELEM = "Undefined element <{0}> encountered";

    public static String ERR_VLD_EMPTY = "Element <{0}> has EMPTY content specification; can not contain {1}";
    public static String ERR_VLD_NON_MIXED = "Element <{0}> has non-mixed content specification; can not contain non-white space text, or any CDATA sections";
    public static String ERR_VLD_ANY = "Element <{0}> has ANY content specification; can not contain {1}";
    public static String ERR_VLD_UNKNOWN_ATTR = "Element <{0}> has no attribute \"{1}\"";

    // // // Namespace problems:

    public static String ERR_NS_EMPTY = 
"Non-default namespace can not map to empty URI (as per Namespace 1.0 # 2)";

    // // // Output problems:

    public static String WERR_PROLOG_CDATA =
        "Trying to output a CDATA block outside main element tree (in prolog or epilog)";
    public static String WERR_PROLOG_NONWS_TEXT =
        "Trying to output non-whitespace characters outside main element tree (in prolog or epilog)";

    public static String WERR_CDATA_CONTENT =
        "Illegal input: CDATA block has embedded ']]>' in it (index {0})";
    public static String WERR_COMMENT_CONTENT = 
        "Illegal input: comment content has embedded '--' in it (index {0})";

    public static String WERR_ATTR_NO_ELEM =
        "Trying to write an attribute when there is no open start element.";

    public static String WERR_NAME_EMPTY = "Illegal to pass empty name";

    public static String WERR_NAME_ILLEGAL_FIRST_CHAR = "Illegal first name character {0}";
    public static String WERR_NAME_ILLEGAL_CHAR = "Illegal name character {0}";

    /*
    ////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////
     */

    public static String tokenTypeDesc(int type)
    {
        switch (type) {
        case START_ELEMENT:
            return "START_ELEMENT";
        case END_ELEMENT:
            return "END_ELEMENT";
        case START_DOCUMENT:
            return "START_DOCUMENT";
        case END_DOCUMENT:
            return "END_DOCUMENT";

        case CHARACTERS:
            return "CHARACTERS";
        case CDATA:
            return "CDATA";
        case SPACE:
            return "SPACE";

        case COMMENT:
            return "COMMENT";
        case PROCESSING_INSTRUCTION:
            return "PROCESSING_INSTRUCTION";
        case DTD:
            return "DTD";
        case ENTITY_REFERENCE:
            return "ENTITY_REFERENCE";
        }
        return "["+type+"]";
    }

    public static String formatMessage(String format, Object arg)
    {
        return MessageFormat.format(format, new Object[] { arg });
    }

    public static String formatMessage(String format, Object arg1, Object arg2)
    {
        return MessageFormat.format(format, new Object[] { arg1, arg2 });
    }
}
