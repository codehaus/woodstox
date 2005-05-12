package preValidator.parsers;

import java.lang.System;
import preValidator.documents.*;
import java_cup.runtime.Symbol;
import preValidator.utils.Debug;
import preValidator.errors.*;

/*class Sample {
    public static void main(String argv[]) throws java.io.IOException {
	Yylex yy = new Yylex(System.in);
	Yytoken t;
	while ((t = yy.yylex()) != null)
	    System.out.println(t);
    }
}
/*
__ENTITYVALUE= [\"]([^%&\"]|{PEREFERENCE}|{REFERENCE})*[\"]|[\"]([^%&']|{PEREFERENCE}|{REFERENCE})*[\"] 
__ENTITYVALUE= [\x22]([a-z])*[\x22] 
__ATTVALUE=    [\"]([^<&\"]|{REFERENCE})*[\"]|[\"]([^<&']|{REFERENCE})*[\"] 
__SYSTEMLITERAL=    ([\"][^\"]*[\"])|([\"][^']*[\"])
__PUBIDLITERAL=    [\"]{PUBIDCHAR}*[\"]|[']({PUBIDCHAR_})*[']

PUBIDCHAR_=		\x20|\x0D|\x0A|[a-zA-Z0-9]|[\x28-\x2F]|\03F|[:=;!#@$_%]
PUBIDCHAR=		{PUBIDCHAR_}|(')     

<YYINITIAL> {ENTITYVALUE} { System.out.println("ENETT");return (new Symbol(sym.ENTITYVALUE,yytext(),yyline,yychar,yytext().length())); }
<YYINITIAL> {ATTVALUE} { return (new Symbol(sym.ATTVALUE,yytext(),yyline,yychar,yytext().length())); }
<YYINITIAL> {SYSTEMLITERAL} { return (new Symbol(sym.SYSTEMLITERAL,yytext(),yyline,yychar,yytext().length())); }
<YYINITIAL> {PUBIDLITERAL} { return (new Symbol(sym.PUBIDLITERAL,yytext(),yyline,yychar,yytext().length())); }
<YYINITIAL> {PUBIDCHAR} { Debug.LexerPrint("PUBIDCHAR--"+yytext()); return (new Symbol(sym.PUBIDCHAR,yytext(),yyline,yychar,yytext().length())); }


*/

%%

%{
	public ErrorList el;

	private int string_count = 0;
	private boolean in_facet=false;
	private boolean end_string=false;
	private int comment_count = 0;

	StringBuffer string = new StringBuffer();


	int quoteState=0;		// 1=single, 2=double
	int quoteStateAttlist=0;
	boolean entityRef=false;
	boolean entityOn=false;
	boolean entityComplex=false;
	boolean fromEntity=false;
	int entityComplexQuote=0;
	boolean includeState=false;
	boolean ignoreState=false;


	public Yylex(java.io.Reader in,ErrorList el) {
		this(in);
		this.el=el;
	}

	private void resetAll()
	{
		quoteStateAttlist=0;
		entityRef=false;
		entityOn=false;
		fromEntity=false; 
		entityComplex=false;
		entityComplexQuote=0; 
		quoteState=0;
	}

%} 


%implements java_cup.runtime.Scanner
%function next_token
%type java_cup.runtime.Symbol


%line
%char
%public

%state COMMENT_STATE
%state STRING
%state PATTERN
%state MACRO_STATE
%state MACROSTRING
%state ATTLIST
%state ATTLISTSTRING
%state TARGETNS
%state TARGETNSSTRING
%state IGNORE
%state NOTATION
%unicode
%notunix


XMLDECL=		((<){QUEST}(xml)){VERSIONINFO}({ENCODINGDECL})?({SDDECL})?({S})?{QUEST}(>) 
VERSIONINFO=    {S}(version){EQ}({SINGLEQUOTE}{VERSIONNUM}{SINGLEQUOTE}|{QUOTE}{VERSIONNUM}{QUOTE})
EQ=				({S})?(=)({S})? 
VERSIONNUM=		([a-zA-Z0-9_.:]|{LESS})+ 
ENCODINGDECL=	{S}(encoding)({EQ})({QUOTE}{ENCNAME}{QUOTE}|{SINGLEQUOTE}{ENCNAME}{SINGLEQUOTE})  
ENCNAME=		[A-Za-z]([A-Za-z0-9._]|{LESS})* 
SDDECL=			{S}(standalone){EQ}(({SINGLEQUOTE}((yes)|(no)){SINGLEQUOTE})|({QUOTE}((yes)|(no)){QUOTE}))  

BR_CHAR=[\r\n]
WHITE_SPACE_CHAR=[\r\n\ \t\b\012]

NEW_LINE=[\r(\n)(\r\n)]

O_BRACE=[\x7B]
C_BRACE=[\x7D]
O_SQUARE=[\x5B]
C_SQUARE=[\x5D]
O_PAR=[\x28]
C_PAR=[\x29]
SLASH=[\x2F]
BACKSLASH=[\x5C]
QUOTE=[\x22]
PIPE=[\xA6]|[\x7C]
LESS=[\x2D]
QUEST=[\x3F]
SINGLEQUOTE=[\x27]
DOT=[\x2E]


PATTERN=({BACKSLASH}|{BACKSLASH}{SLASH}|{LETTER}|{O_BRACE}|{C_BRACE}|{O_SQUARE}|{C_SQUARE}|{O_PAR}|{C_PAR}|[0-9])*


CHAR=			[\x09]|[\x0A]|[\x0D]|[\x20-\xD7FF]|[\xE000-\xFFFD]|[\x10000-\x10FFF]
S=				(\x20|\x09|\x0D|\x0A)+ 
BASECHAR=		[\x0041-\x005A]|[\x0061-\x007A]|[\x00C0-\x00D6]|[\x00D8-\x00F6]|[\x00F8-\x00FF]|[\x0100-\x0131]|[\x0134-\x013E]|[\x0141-\x0148]|[\x014A-\x017E]|[\x0180-\x01C3]|[\x01CD-\x01F0]|[\x01F4-\x01F5]|[\x01FA-\x0217]|[\x0250-\x02A8]|[\x02BB-\x02C1]|\x0386|[\x0388-\x038A]|\x038C|[\x038E-\x03A1]|[\x03A3-\x03CE]|[\x03D0-\x03D6]|\x03DA|\x03DC|\x03DE|\x03E0|[\x03E2-\x03F3]|[\x0401-\x040C]|[\x040E-\x044F]|[\x0451-\x045C]|[\x045E-\x0481]|[\x0490-\x04C4]|[\x04C7-\x04C8]|[\x04CB-\x04CC]|[\x04D0-\x04EB]|[\x04EE-\x04F5]|[\x04F8-\x04F9]|[\x0531-\x0556]|\x0559|[\x0561-\x0586]|[\x05D0-\x05EA]|[\x05F0-\x05F2]|[\x0621-\x063A]|[\x0641-\x064A]|[\x0671-\x06B7]|[\x06BA-\x06BE]|[\x06C0-\x06CE]|[\x06D0-\x06D3]|\x06D5|[\x06E5-\x06E6]|[\x0905-\x0939]|\x093D|[\x0958-\x0961]|[\x0985-\x098C]|[\x098F-\x0990]|[\x0993-\x09A8]|[\x09AA-\x09B0]|\x09B2|[\x09B6-\x09B9]|[\x09DC-\x09DD]|[\x09DF-\x09E1]|[\x09F0-\x09F1]|[\x0A05-\x0A0A]|[\x0A0F-\x0A10]|[\x0A13-\x0A28]|[\x0A2A-\x0A30]|[\x0A32-\x0A33]|[\x0A35-\x0A36]|[\x0A38-\x0A39]|[\x0A59-\x0A5C]|\x0A5E|[\x0A72-\x0A74]|[\x0A85-\x0A8B]|\x0A8D|[\x0A8F-\x0A91]|[\x0A93-\x0AA8]|[\x0AAA-\x0AB0]|[\x0AB2-\x0AB3]|[\x0AB5-\x0AB9]|\x0ABD|\x0AE0|[\x0B05-\x0B0C]|[\x0B0F-\x0B10]|[\x0B13-\x0B28]|[\x0B2A-\x0B30]|[\x0B32-\x0B33]|[\x0B36-\x0B39]|\x0B3D|[\x0B5C-\x0B5D]|[\x0B5F-\x0B61]|[\x0B85-\x0B8A]|[\x0B8E-\x0B90]|[\x0B92-\x0B95]|[\x0B99-\x0B9A]|\x0B9C|[\x0B9E-\x0B9F]|[\x0BA3-\x0BA4]|[\x0BA8-\x0BAA]|[\x0BAE-\x0BB5]|[\x0BB7-\x0BB9]|[\x0C05-\x0C0C]|[\x0C0E-\x0C10]|[\x0C12-\x0C28]|[\x0C2A-\x0C33]|[\x0C35-\x0C39]|[\x0C60-\x0C61]|[\x0C85-\x0C8C]|[\x0C8E-\x0C90]|[\x0C92-\x0CA8]|[\x0CAA-\x0CB3]|[\x0CB5-\x0CB9]|\x0CDE|[\x0CE0-\x0CE1]|[\x0D05-\x0D0C]|[\x0D0E-\x0D10]|[\x0D12-\x0D28]|[\x0D2A-\x0D39]|[\x0D60-\x0D61]|[\x0E01-\x0E2E]|\x0E30|[\x0E32-\x0E33]|[\x0E40-\x0E45]|[\x0E81-\x0E82]|\x0E84|[\x0E87-\x0E88]|\x0E8A|\x0E8D|[\x0E94-\x0E97]|[\x0E99-\x0E9F]|[\x0EA1-\x0EA3]|\x0EA5|\x0EA7|[\x0EAA-\x0EAB]|[\x0EAD-\x0EAE]|\x0EB0|[\x0EB2-\x0EB3]|\x0EBD|[\x0EC0-\x0EC4]|[\x0F40-\x0F47]|[\x0F49-\x0F69]|[\x10A0-\x10C5]|[\x10D0-\x10F6]|\x1100|[\x1102-\x1103]|[\x1105-\x1107]|\x1109|[\x110B-\x110C]|[\x110E-\x1112]|\x113C|\x113E|\x1140|\x114C|\x114E|\x1150|[\x1154-\x1155]|\x1159|[\x115F-\x1161]|\x1163|\x1165|\x1167|\x1169|[\x116D-\x116E]|[\x1172-\x1173]|\x1175|\x119E|\x11A8|\x11AB|[\x11AE-\x11AF]|[\x11B7-\x11B8]|\x11BA|[\x11BC-\x11C2]|\x11EB|\x11F0|\x11F9|[\x1E00-\x1E9B]|[\x1EA0-\x1EF9]|[\x1F00-\x1F15]|[\x1F18-\x1F1D]|[\x1F20-\x1F45]|[\x1F48-\x1F4D]|[\x1F50-\x1F57]|\x1F59|\x1F5B|\x1F5D|[\x1F5F-\x1F7D]|[\x1F80-\x1FB4]|[\x1FB6-\x1FBC]|\x1FBE|[\x1FC2-\x1FC4]|[\x1FC6-\x1FCC]|[\x1FD0-\x1FD3]|[\x1FD6-\x1FDB]|[\x1FE0-\x1FEC]|[\x1FF2-\x1FF4]|[\x1FF6-\x1FFC]|\x2126|[\x212A-\x212B]|\x212E|[\x2180-\x2182]|[\x3041-\x3094]|[\x30A1-\x30FA]|[\x3105-\x312C]|[\xAC00-\xD7A3]
IDEOGRAPHIC=	[\x4E00-\x9FA5]|\x3007|[\x3021-\x3029]
DIGIT=			[\x0030-\x0039]|[\x0660-\x0669]|[\x06F0-\x06F9]|[\x0966-\x096F]|[\x09E6-\x09EF]|[\x0A66-\x0A6F]|[\x0AE6-\x0AEF]|[\x0B66-\x0B6F]|[\x0BE7-\x0BEF]|[\x0C66-\x0C6F]|[\x0CE6-\x0CEF]|[\x0D66-\x0D6F]|[\x0E50-\x0E59]|[\x0ED0-\x0ED9]|[\x0F20-\x0F29]

LETTER=		[A-Za-z]
NUMBER=		[0-9]([0-9])*

NAMECHAR=	{LETTER}|{NUMBER}|[-_:]|\x2E
NAME=		({LETTER}|[_:])({NAMECHAR})* 


CHARREF=    (&#)[0-9]+(;)|(&#x)[0-9a-fA-F]+(;) 

REFERENCE=	{ENTITYREF}|{CHARREF}
ENTITYREF=  ([&]{NAME}[;])
PEREFERENCE=([%]{NAME}[;])
COMPLEXREF=([@]{NAME}[;])
SIMPLEREF=([#]{NAME}[;])

PREDEFINEDREF=([#]((STRING)|(LANGUAGE)|(CDATA)|(BOOLEAN)|(DECIMAL)|(FLOAT)|(DOUBLE)|(DURATION)|(DATETIME)|(TIME)|(DATE)|(GYEARMONTH)|(GYEAR)|(GMONTHDAY)|(GMONTH)|(HEXBINARY)|(BASE64BINARY)|(ANYURI)|(QNAME)|(NOTATION)|(NORMSTRING)|(TOKEN)|(NMTOKEN)|(NMTOKENS)|(NAME)|(NCNAME)|(ID)|(IDREF)|(IDREFS)|(ENTITY)|(ENTITIES)|(INTEGER)|(NONPOINTEGER)|(NEGINTEGER)|(LONG)|(INT)|(SHORT)|(BYTE)|(NONNEGINTEGER)|(POSINTEGER)|(ULONG)|(UINT)|(USHORT)|(UBYTE)))


STRINGTYPE=			(CDATA) 
TOKENIZEDTYPE=		(ID)|(IDREF)|(IDREFS)|(ENTITY)|(ENTITIES)|(NMTOKEN)|(NMTOKENS) 


CHARDATA=	[^<&]*(-)([^<&]*(\]\]>)[^<&]*) 


COMMENT_TEXT="<!--" ~"-->"
StringCharacter = [^\"\\\']
OctDigit          = [0-7]
PatternCharacter= [^/\\]


EMPTY=(EMPTY)
ANY=(ANY)

SPACE_P=(\\p\\)
SPACE_R=(\\r\\)
SPACE_C=(\\c\\)


%% 

<YYINITIAL,MACRO_STATE,ATTLIST,PATTERN,TARGETNS> {WHITE_SPACE_CHAR}+ { }


<YYINITIAL> "<!ENTITY %" { 	
				Debug.LexerPrint(yystate()+" BEGIN MACRO");
				yybegin(MACRO_STATE); 
			}

<YYINITIAL> 
{
	{XMLDECL}		{ Debug.LexerPrint(yytext());return (new Symbol(sym.XMLDECL,yytext(),yyline,yychar,yytext().length())); }
	"SYSTEM"		{ Debug.LexerPrint(yytext()); return (new Symbol(sym.SYSTEM,yytext(),yyline,yychar,yytext().length())); }
	"PUBLIC"		{ Debug.LexerPrint(yytext()); return (new Symbol(sym.PUBLIC,yytext(),yyline,yychar,yytext().length())); }
	"NDATA"			{ Debug.LexerPrint(yytext()); return (new Symbol(sym.NDATA,yytext(),yyline,yychar,yytext().length())); }

	"<!ELEMENT"		{ Debug.LexerPrint(yytext()); return (new Symbol(sym.BEGIN_ELEMENT,yytext(),yyline,yychar,yytext().length())); }
	"<!ENTITY"		{ Debug.LexerPrint(yytext()); entityOn=true; entityRef=true; return (new Symbol(sym.BEGIN_ENTITY,yytext(),yyline,yychar,yytext().length())); }
	">"			{ 
					Debug.LexerPrint(yytext());
					this.resetAll();
					return (new Symbol(sym.END_TAG,yytext(),yyline,yychar,yytext().length())); 
				}
	{COMMENT_TEXT}	{Debug.LexerPrint("COMMENT--"+yytext()); return (new Symbol(sym.COMMENTO,yytext(),yyline,yychar,yychar+yytext().length())); }

	"<!ATTLIST "	{	Debug.LexerPrint(yystate()+" "+yytext()+" "+sym.BEGIN_ATTLIST); 
						yybegin(ATTLIST); 
						return (new Symbol(sym.BEGIN_ATTLIST,yytext(),yyline,yychar,yytext().length())); 
					}
}
<YYINITIAL> 	{QUOTE}		
					{
						Debug.LexerPrint("YYINITIAL - QUOTE - quotestate="+quoteState); 
					
						if(quoteState==0)
							quoteState=2;
						else if(quoteState==2)
							quoteState=0;
						
						if(quoteState==1)
						{
							Debug.LexerPrint("Start Attlist Quote"); 
							quoteStateAttlist=2;
							string.setLength(0); 
							fromEntity=true;
							yybegin(ATTLISTSTRING);	
						}
						else
						{
							Debug.LexerPrint("start quote at: "+yychar); 
							if(entityOn && entityRef)
							{	
								Debug.LexerPrint("foundstring"); 
								string.setLength(0); 
								yybegin(STRING);
							}
							else
							{
								Debug.LexerPrint(yytext()); return (new Symbol(sym.VIRGOLETTE,yytext(),yyline,yychar,yytext().length())); 
							}
						}
					}

<YYINITIAL> 	{SINGLEQUOTE}		
					{
						Debug.LexerPrint("YYINITIAL - SINGLE - quotestate="+quoteState); 
						if(quoteState==0)
							quoteState=1;
						else if(quoteState==1)
							quoteState=0;

						if(quoteState==2)
						{
							Debug.LexerPrint("Start Attlist Quote"); 
							quoteStateAttlist=1;
							string.setLength(0); 
							fromEntity=true;
							yybegin(ATTLISTSTRING);	
						}
						else
						{

							Debug.LexerPrint("start quote at: "+yychar); 
							if(entityOn && entityRef)
							{	
								Debug.LexerPrint("foundstring"); 
								string.setLength(0); 
								yybegin(STRING);
							}
							else
							{
								Debug.LexerPrint(yytext()); return (new Symbol(sym.VIRGOLETTE,yytext(),yyline,yychar,yytext().length())); 
							}
						}
					}
<YYINITIAL,ATTLIST> 
{

	"("				{ Debug.LexerPrint(yystate()+" - "+yytext());return (new Symbol(sym.OPENPAR,yytext(),yyline,yychar,yytext().length())); }
	")"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.CLOSEPAR,yytext(),yyline,yychar,yytext().length())); }
	"["				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.OPENSQUARE,yytext(),yyline,yychar,yytext().length())); }
	"]"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.CLOSESQUARE,yytext(),yyline,yychar,yytext().length())); }
	"{"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.OPENBRACE,yytext(),yyline,yychar,yytext().length())); }
	"}"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.CLOSEBRACE,yytext(),yyline,yychar,yytext().length())); }

	"|"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.OR,yytext(),yyline,yychar,yytext().length())); }
	","				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.COMMA,yytext(),yyline,yychar,yytext().length())); }
	"."				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.DOT,yytext(),yyline,yychar,yytext().length())); }

	"?"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.QUEST,yytext(),yyline,yychar,yytext().length())); }
	"*"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.STAR,yytext(),yyline,yychar,yytext().length())); }
	"+"				{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.PLUS,yytext(),yyline,yychar,yytext().length())); }

	"@"				{ Debug.LexerPrint(yystate()+" AT - "+yytext()); if(entityOn) {entityComplex=true;} entityRef=false; return (new Symbol(sym.AT,yytext(),yyline,yychar,yytext().length())); }
	{WHITE_SPACE_CHAR}"&"{WHITE_SPACE_CHAR}
					{ Debug.LexerPrint(yystate()+" AMP - "+yytext());  return (new Symbol(sym.AMP,yytext(),yyline,yychar,yytext().length())); }
	"%"				{ Debug.LexerPrint(yystate()+" PERCENT - "+yytext()); entityRef=false; return (new Symbol(sym.PERCENT,yytext(),yyline,yychar,yytext().length())); }
	"#"				{ Debug.LexerPrint(yystate()+" SHARP - "+yytext()); entityRef=false; return (new Symbol(sym.SHARP,yytext(),yyline,yychar,yytext().length())); }


	"#PCDATA"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.PCDATA,yytext(),yyline,yychar,yytext().length())); }
//	"#TOKEN"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.TOKEN,yytext(),yyline,yychar,yytext().length())); }


	"##any"			{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.ANY_ANY ,yytext(),yyline,yychar,yytext().length())); }
	"##other"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.ANY_OTHER,yytext(),yyline,yychar,yytext().length())); }
	"##local"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.ANY_LOCAL,yytext(),yyline,yychar,yytext().length())); }
	"##targetNamespace" { Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.ANY_TNS,yytext(),yyline,yychar,yytext().length())); }

	"#REQUIRED"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.REQUIRED,yytext(),yyline,yychar,yytext().length())); }
	"#IMPLIED"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.IMPLIED,yytext(),yyline,yychar,yytext().length())); }
	"#FIXED"		{ Debug.LexerPrint(yystate()+" - "+yytext()); return (new Symbol(sym.FIXED,yytext(),yyline,yychar,yytext().length())); }
	{EMPTY}			{ Debug.LexerPrint(yystate()+" - "+"EMPTY--"+yytext()); return (new Symbol(sym.EMPTY,yytext(),yyline,yychar,yytext().length())); }
	{ANY}			{ Debug.LexerPrint(yystate()+" - "+"ANY--"+yytext()); return (new Symbol(sym.ANY,yytext(),yyline,yychar,yytext().length())); }


	{STRINGTYPE}	{ Debug.LexerPrint(yystate()+" - "+"STR_TYPE--"+yytext()); return (new Symbol(sym.STRINGTYPE,yytext(),yyline,yychar,yytext().length())); }
	{TOKENIZEDTYPE} { Debug.LexerPrint(yystate()+" - "+"TOK_TYPE--"+yytext()); return (new Symbol(sym.TOKENIZEDTYPE,yytext(),yyline,yychar,yytext().length())); }

	{SPACE_P}		{ Debug.LexerPrint(yystate()+" - "+"SPACE_P--"+yytext()); return (new Symbol(sym.SPACE_P,yytext(),yyline,yychar,yytext().length())); }
	{SPACE_R}		{ Debug.LexerPrint(yystate()+" - "+"SPACE_R--"+yytext()); return (new Symbol(sym.SPACE_R,yytext(),yyline,yychar,yytext().length())); }
	{SPACE_C}		{ Debug.LexerPrint(yystate()+" - "+"SPACE_C--"+yytext()); return (new Symbol(sym.SPACE_C,yytext(),yyline,yychar,yytext().length())); }

	{CHARREF}		{ Debug.LexerPrint(yystate()+" - "+"CHARREF--"+yytext()); return (new Symbol(sym.CHARREF,yytext(),yyline,yychar,yytext().length())); }


	{ENTITYREF}		{ Debug.LexerPrint(yystate()+" - "+"ENTITYREF--"+yytext()); return (new Symbol(sym.ENTITYREF,yytext(),yyline,yychar,yytext().length())); }
	{PEREFERENCE}	{ Debug.LexerPrint(yystate()+" - "+"PREREFERENCE--"+yytext()); return (new Symbol(sym.PEREFERENCE,yytext(),yyline,yychar,yytext().length())); }
	{COMPLEXREF}	{ Debug.LexerPrint(yystate()+" - "+"COMPLEXREF--"+yytext()); return (new Symbol(sym.COMPLEXREF,yytext(),yyline,yychar,yytext().length())); }
	{SIMPLEREF}		{ Debug.LexerPrint(yystate()+" - "+"SIMPLEREF--"+yytext()); return (new Symbol(sym.SIMPLEREF,yytext(),yyline,yychar,yytext().length())); }
	{PREDEFINEDREF} { Debug.LexerPrint(yystate()+" - "+"PREDEFREF--"+yytext()); return (new Symbol(sym.PREDEFINEDREF,yytext(),yyline,yychar,yytext().length())); }
	{NUMBER}		{ Debug.LexerPrint(yystate()+" - "+"NUMBER--"+yytext()); return (new Symbol(sym.NUMBER,yytext(),yyline,yychar,yytext().length())); }

	
	{NAME}			{ Debug.LexerPrint(yystate()+" - "+"NAME--"+yytext()); return (new Symbol(sym.NAME,yytext(),yyline,yychar,yytext().length())); }
}

<YYINITIAL> 	{SLASH}		{ yybegin(PATTERN); string.setLength(0); }
<PATTERN> {
	"/"							{	
									Debug.LexerPrint("pattern -- "+yytext());
									yybegin(YYINITIAL);
									return (new Symbol(sym.PATTERN,string.toString(),yyline,yychar,yytext().length())); 
								}
					  
  {PatternCharacter}+			{ string.append( yytext() ); }
  
  /* escape sequences */
  "\\b"							{ string.append( "\\b" ); }
  "\\t"                         { string.append( "\\t" ); }
  "\\n"                         { string.append( "\\n" ); }
  "\\f"                         { string.append( "\\f" ); }
  "\\r"                         { string.append( "\\r" ); }
  \\{PatternCharacter}			{ string.append( yytext() ); }
  "\\\\"						{ string.append( "\\\\" ); }
  "\\/"							{ string.append( "\\/" ); }
  "//"							{ string.append( "//" ); }
  \\[0-3]?{OctDigit}?{OctDigit} { char val = (char) Integer.parseInt(yytext().substring(1),8);
                        				   string.append( val ); }
  
  /* error cases */
  \\.							{ throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
}





<MACRO_STATE> {
	
	"SYSTEM"	{	Debug.LexerPrint("MACRO STATE "+yytext()); }
	"PUBLIC"	{	Debug.LexerPrint("MACRO STATE "+yytext()); }
	"NDATA"		{	Debug.LexerPrint("MACRO STATE "+yytext()); }

	{NAME}	{
				Debug.LexerPrint("MACRO STATE "+yytext()); 
			}


	{QUOTE}			{ quoteState=2;yybegin(MACROSTRING);  }
	{SINGLEQUOTE}	{ quoteState=1;yybegin(MACROSTRING);  }

	">"		{ 
				Debug.LexerPrint("MACRO STATE "+"- CLOSE MACRO");
				this.resetAll();
				yybegin(YYINITIAL);
			}

	/* error cases */
	.		{
				Debug.LexerPrint("MACRO STATE "+"- ERROR");
				el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
			}
}


<MACROSTRING> {
	{QUOTE}	{ 
			if(quoteState==2)
				yybegin(MACRO_STATE);
			}

  	{SINGLEQUOTE}	{ 
			if(quoteState==1)
				yybegin(MACRO_STATE);
			}

  {StringCharacter}+             {}
  
  /* escape sequences */
  "\\b"                          {}
  "\\t"                          {}
  "\\n"                          {}
  "\\f"                          {}
  "\\r"                          {}
  "\\\""                         {}
  "\\'"                          {}
  "\\\\"                         {}
  \\[0-3]?{OctDigit}?{OctDigit}  {}
 
  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
}




<ATTLIST>
{
	{QUOTE}		{ 	
					Debug.LexerPrint("ATTLIST - QUOTE - quotestate="+quoteState); 
					Debug.LexerPrint("Start Attlist Quote"); 
					quoteStateAttlist=2;
					string.setLength(0); 
					yybegin(ATTLISTSTRING);
				}

	{SINGLEQUOTE}
				{ 	
					Debug.LexerPrint("ATTLIST - SINGLEQUOTE - quotestate="+quoteState); 
					Debug.LexerPrint("Start Attlist Quote"); 
					quoteStateAttlist=1;
					string.setLength(0); 
					yybegin(ATTLISTSTRING);
				}

	">"			{
					Debug.LexerPrint("Close ATTLIST-TAG"); 
					this.resetAll();
					yybegin(YYINITIAL);
					return (new Symbol(sym.END_TAG,yytext(),yyline,yychar,yytext().length())); 
				}
	/* error cases */
	.		{
				el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
			}
}
<ATTLISTSTRING> {
	{QUOTE}
		{
			Debug.LexerPrint("ATTLISTSTRING - QUOTE - quotestate="+quoteState); 

			if(quoteStateAttlist==2)
			{
				if(fromEntity==true)
					yybegin(YYINITIAL);
				else
					yybegin(ATTLIST);

				Debug.LexerPrint("Stop Attlist Quote"); 
				return (new Symbol(sym.ATTLISTSTRING,string.toString(),yyline,yychar,0)); 
			}
			else
			{
				string.append( yytext() );
			}
		}

	{SINGLEQUOTE}
		{ 
			Debug.LexerPrint("ATTLISTSTRING - SINGLEQUOTE - quotestate="+quoteState); 
			if(quoteStateAttlist==1)
			{
				if(fromEntity==true)
					yybegin(YYINITIAL);
				else
					yybegin(ATTLIST);
				Debug.LexerPrint("Stop Attlist Quote"); 
				return (new Symbol(sym.ATTLISTSTRING,string.toString(),yyline,yychar,0)); 
			}
			else
			{
				string.append( yytext() );
			}
		}
  {StringCharacter}+             { 
					Debug.LexerPrint("ATTLISTSTRING - append text - quotestate="+quoteState+" yytext="+yytext()); 
					
					string.append( yytext() ); }
  
  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8);
                        				   string.append( val ); }
  
  
  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
}


<STRING> {
	{QUOTE}	{
			Debug.LexerPrint("STRING - QUOTE"); 

			if(quoteState==2)
			{
				yybegin(YYINITIAL);
				return (new Symbol(sym.STRING,string.toString(),yyline,yychar,0)); 
			}
			else
			{
				if(entityComplexQuote==3)
				{
					Debug.LexerPrint("found attstring"); 
					string.setLength(0); 

					yybegin(ATTLISTSTRING);
				}
				else	
					string.append( yytext() );
			}
		}
	{SINGLEQUOTE}	{
			Debug.LexerPrint("STRING - SINGLEQUOTE"); 
			if(quoteState==1)
			{
				yybegin(YYINITIAL);
				return (new Symbol(sym.STRING,string.toString(),yyline,yychar,0)); 
			}
			else
			{
				if(entityComplexQuote==3)
				{
					Debug.LexerPrint("found attstring"); 
					string.setLength(0); 
					yybegin(ATTLISTSTRING);
				}
				else	
					string.append( yytext() );
			}

		}


  {StringCharacter}+             { string.append( yytext() ); }
  
  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8);
                        				   string.append( val ); }
  
  
  /* error cases */
  \\.                       {
				el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
			}
}


<YYINITIAL> "<!TARGETNS " {		
							Debug.LexerPrint(yystate()+" "+yytext()+" "+sym.BEGIN_TARGETNS); 
							yybegin(TARGETNS); 
							return (new Symbol(sym.BEGIN_TARGETNS,yytext(),yyline,yychar,yytext().length())); 
						}

<TARGETNS>
{
	{QUOTE}		{ 	Debug.LexerPrint("TN foundstring"); 
					quoteState=2;
					string.setLength(0); 
					yybegin(TARGETNSSTRING);}

	{SINGLEQUOTE}	{ 	Debug.LexerPrint("TN foundstring"); 
					quoteState=1;
					string.setLength(0); 
					yybegin(TARGETNSSTRING);}


	">"			{
					Debug.LexerPrint(yytext()); 
					this.resetAll();
					yybegin(YYINITIAL);
					return (new Symbol(sym.END_TAG,yytext(),yyline,yychar,yytext().length())); 
				}

	{NAME}			{ Debug.LexerPrint(yystate()+" Target ns "+"NAME--"+yytext()); return (new Symbol(sym.NAME,yytext(),yyline,yychar,yytext().length())); }

	/* error cases */
	.		{
				el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
			}
}
<TARGETNSSTRING> {
	{QUOTE}	{
			if(quoteState==2)
			{
				yybegin(TARGETNS);
				return (new Symbol(sym.TARGETNSSTRING,string.toString(),yyline,yychar,0)); 
			}
			else
				string.append( yytext() ); 
		}

	{SINGLEQUOTE}	{
			if(quoteState==1)
			{
				yybegin(TARGETNS);
				return (new Symbol(sym.TARGETNSSTRING,string.toString(),yyline,yychar,0)); 
			}
			else
				string.append( yytext() ); 
		}
  {StringCharacter}+             { string.append( yytext() ); }
  
  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8);
                        				   string.append( val ); }
  
  /* error cases */
  \\.                            { throw new RuntimeException("Illegal escape sequence \""+yytext()+"\""); }
}




<YYINITIAL> . {
		el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
}



/* Gestione dell'INCLUDE E dell'IGNORE */
<YYINITIAL> "<![INCLUDE[" { Debug.LexerPrint(yytext()); includeState=true;  }
<YYINITIAL> "<![IGNORE[" { Debug.LexerPrint(yytext()); yybegin(IGNORE); }
<IGNORE> "]]>"	{yybegin(YYINITIAL);}
<IGNORE> .	{}
<IGNORE> \n	{}

<YYINITIAL> "]]>" {
			if(includeState==true)
			{
				includeState=false;
			}
			else
			{
				el.addElement(ErrorType.CHARACTER_NOT_ATTENDED,yychar,yychar+1,yytext(),false);
			}
				
		}


/* Gestione dell'NOTATION*/
<YYINITIAL> "<!NOTATION " { Debug.LexerPrint(yytext()); yybegin(NOTATION);}
<NOTATION> ">"	{yybegin(YYINITIAL);}
<NOTATION> .	{}
<NOTATION> \n	{}


