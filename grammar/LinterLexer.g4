lexer grammar LinterLexer;

@header {
    package antlr4gen;
}

options {
    caseInsensitive = true;
}

// =============================================================================
// MACROS & LABELS
// =============================================================================
MACRO_BLOCK              : '.macro' .*? '.endm' ;
MACRO_NAME               : '__' ID                                                          -> mode(OPERANDS) ;
LABEL_DEF                : ID ':' ;

// =============================================================================
// INSTRUCTIONS (Grouped logically)
// =============================================================================

// --- Branches & Control Flow ---
BRANCH                   : B COND?                                                          -> mode(OPERANDS) ;
BRANCHLINK               : 'bl' COND?                                                       -> mode(OPERANDS) ;
BRANCHLINKX              : 'blx' COND?                                                      -> mode(OPERANDS) ;
BRANCHX                  : 'bx' COND?                                                       -> mode(OPERANDS) ;
BRANXJAZELLE             : 'bxj' COND?                                                      -> mode(OPERANDS) ;
CBNZ                     : ('cbnz' | 'cbz')                                                 -> mode(OPERANDS) ;
IT                       : 'it' ('t'|'e')? ('t'|'e')? ('t'|'e')?                            -> mode(OPERANDS) ;
TBB                      : 'tbb'                                                            -> mode(OPERANDS) ;
TBH                      : 'tbh'                                                            -> mode(OPERANDS) ;

// --- Memory (Load/Store) ---
ADRL                     : ('ADR' | 'ADRL') COND? WIDTH?                                    -> mode(OPERANDS) ;
LDR                      : 'ldr' TYPE? COND? WIDTH?                                         -> mode(OPERANDS) ;
LDRD                     : 'ldrd' COND?                                                     -> mode(OPERANDS) ;
LDRT                     : 'ldr' TYPE? T COND?                                              -> mode(OPERANDS) ;
STR                      : 'str' TYPE? COND?                                                -> mode(OPERANDS) ;
STRD                     : 'strd' COND?                                                     -> mode(OPERANDS) ;
STRT                     : 'str' TYPE? T COND?                                              -> mode(OPERANDS) ;
LDMSTM                   : ('ldm' | 'stm') ADDRMODE? COND?                                  -> mode(OPERANDS) ;
PUSH                     : 'push' COND?                                                     -> mode(OPERANDS) ;
POP                      : 'pop' COND?                                                      -> mode(OPERANDS) ;
PRELOADDATA              : ('PLD' | 'PLDW' | 'PLI') COND?                                   -> mode(OPERANDS) ;

// --- Data Movement & Shifts ---
MOV                      : ('mov'|'mvn') SFLAG? COND?                                       -> mode(OPERANDS) ;
MOVT                     : 'movt' COND?                                                     -> mode(OPERANDS) ;
MOVW                     : 'movw' COND?                                                     -> mode(OPERANDS) ;
MOV32                    : 'mov32' COND?                                                    -> mode(OPERANDS) ;
LSL                      : 'lsl' SFLAG? COND?                                               -> mode(OPERANDS) ;
ASR                      : 'asr' SFLAG? COND?                                               -> mode(OPERANDS) ;
LSR                      : 'lsr' SFLAG? COND?                                               -> mode(OPERANDS) ;
ROR                      : 'ror' SFLAG? COND?                                               -> mode(OPERANDS) ;
RRX                      : 'rrx' SFLAG? COND?                                               -> mode(OPERANDS) ;
REV                      : ('rev' | 'rev16' | 'revsh' | 'rbit') COND?                       -> mode(OPERANDS) ;
BFX                      : ('SBFX' | 'UBFX') COND?                                          -> mode(OPERANDS) ;
BFC                      : 'bfc' COND?                                                      -> mode(OPERANDS) ;
BFI                      : 'bfi' COND?                                                      -> mode(OPERANDS) ;
PKH                      : ('pkhbt' | 'pkhtb') COND? ;
SEL                      : 'sel' COND?                                                      -> mode(OPERANDS) ;
CLZ                      : 'CLZ' COND?                                                      -> mode(OPERANDS) ;
EXTEND                   : ('SXT' | 'UXT') (B16 | B | H) COND?                              -> mode(OPERANDS) ;
EXTENDADD                : ('SXTA' | 'UXTA') (B16 | B | H) COND?                            -> mode(OPERANDS) ;

// --- Arithmetic & Logic ---
ADDSUB                   : ('add'|'sub'|'adc'|'rsb'|'sbc'|'rsc') SFLAG? COND?               -> mode(OPERANDS) ;
NEG                      : 'neg' SFLAG?                                                     -> mode(OPERANDS) ;
MUL                      : 'mul' SFLAG? COND?                                               -> mode(OPERANDS) ;
MLA                      : 'mla' SFLAG? COND?                                               -> mode(OPERANDS) ;
MLS                      : 'mls' COND?                                                      -> mode(OPERANDS) ;
DIV                      : ('udiv'|'sdiv') COND?                                            -> mode(OPERANDS) ;
LOGIC                    : ('and'|'orr'|'eor'|'bic'|'orn') SFLAG? COND?                     -> mode(OPERANDS) ;
CMP                      : ('cmp'|'cmn') COND?                                              -> mode(OPERANDS) ;
TEST                     : ('tst'|'teq') COND?                                              -> mode(OPERANDS) ;
CHKA                     : 'CHKA'                                                           -> mode(OPERANDS) ;
SATURATING16             : ('SSAT16' | 'USAT16') COND?                                      -> mode(OPERANDS) ;
SATURATING               : ('SSAT' | 'USAT') COND?                                          -> mode(OPERANDS) ;
DUALMULT                 : ('SMUAD'|'SMUSD') X? COND?                                       -> mode(OPERANDS) ;
DUALADD                  : ('SMLAD'|'SMLSD') X? COND?                                       -> mode(OPERANDS) ;
SMULXY                   : 'smul' (B|T) (B|T) COND?                                         -> mode(OPERANDS) ;
SMALXY                   : 'smla' (B|T) (B|T) COND?                                         -> mode(OPERANDS) ;
SMMUL                    : 'SMMUL' R? COND?                                                 -> mode(OPERANDS) ;
SMMLAS                   : ('SMMLA' | 'SMMLS') R? COND?                                     -> mode(OPERANDS) ;
PARALLELOP               : PREFIX ('add8'|'add16'|'sub8'|'sub16'|'asx'|'sax') COND?         -> mode(OPERANDS) ;
LONGMULTIPLY             : ('UMULL'|'UMLAL'|'SMULL'|'SMLAL') SFLAG? COND?                   -> mode(OPERANDS) ;
SMLALXY                  : 'SMLAL' (B | T) (B | T) COND?                                    -> mode(OPERANDS) ;
DUAL16BITMUL             : ('SMLALD' | 'SMLSLD') X? COND?                                   -> mode(OPERANDS) ;
SMULW                    : 'SMULW' (B | T) COND?                                            -> mode(OPERANDS) ;
SMLAW                    : 'SMLAW' (B | T) COND?                                            -> mode(OPERANDS) ;
UMAAL                    : 'UMAAL' COND?                                                    -> mode(OPERANDS) ;
USAD8                    : 'USAD8' COND?                                                    -> mode(OPERANDS) ;
USADA8                   : 'USADA8' COND?                                                   -> mode(OPERANDS) ;

// --- System, Debug & Events ---
NOP                      : 'nop' COND?                                                      -> mode(OPERANDS) ;
BKPT                     : 'bkpt'                                                           -> mode(OPERANDS) ;
CPS                      : 'cps' ('ie' | 'id')                                              -> mode(OPERANDS) ;
EVENTS                   : ('SEV' | 'WFE' | 'WFI' | 'YIELD') COND?                          -> mode(OPERANDS) ;
CLREX                    : 'CLREX' COND? ;
DBG                      : 'DBG' COND? ;
DMB                      : ('DMB' | 'DSB' | 'ISB') COND?                                    -> mode(OPERANDS) ;
RFE                      : 'RFE' ADDRMODE? COND?                                            -> mode(OPERANDS) ;
SETEND                   : 'SETEND'                                                         -> mode(OPERANDS) ;
SMC                      : 'SMC' COND?                                                      -> mode(OPERANDS) ;
SVC                      : 'svc' COND?                                                      -> mode(OPERANDS) ;

// =============================================================================
// REGISTERS
// =============================================================================
PROGRAMCOUNTER           : 'pc' ;
LINKREGISTER             : 'lr' ;
REGISTER                 : ('r' ( [0-9] | '1' [0-5] )) ;

// =============================================================================
// DIRECTIVES
// =============================================================================
CONSTANT                 : '.equ'                                                           -> mode(OPERANDS) ;
INCLUDE                  : '.include'                                                       -> mode(OPERANDS) ;
TEXT                     : '.text'                                                          -> mode(OPERANDS) ;
DATA                     : '.data'                                                          -> mode(OPERANDS) ;
THUMBFUNC                : '.thumb_func'                                                    -> mode(OPERANDS) ;
REPT                     : '.rept'                                                          -> mode(OPERANDS) ;
ENDR                     : '.endr'                                                          -> mode(OPERANDS) ;
DATATYPE                 : ('.byte'|'.asciz'|'.word'|'.hword'|'.quad'|'.4byte'|'.2byte'|'.long') -> mode(OPERANDS) ;
WIDTH                    : '.W' ;
IFDIR                    : '.if'                                                            -> mode(OPERANDS) ;
ELSEIFDIR                : '.elseif'                                                        -> mode(OPERANDS) ;
ELSEDIR                  : '.else'                                                          -> mode(OPERANDS) ;
ENDIFDIR                 : '.endif'                                                         -> mode(OPERANDS) ;
IFDEF                    : '.ifdef'                                                         -> mode(OPERANDS) ;
IFNDEF                   : '.ifndef'                                                        -> mode(OPERANDS) ;
GENERICDIRECTIVE         : '.' [A-Z]+                                                       -> mode(OPERANDS) ;

// =============================================================================
// FRAGMENTS & MODIFIERS
// =============================================================================
fragment SFLAG           : 's' ;
fragment X               : 'x' ;
fragment B               : 'b' ;
fragment B16             : 'b16' ;
fragment H               : 'H' ;
fragment T               : 't' ;
fragment R               : 'r' ;
fragment DWORD           : 'd' ;
fragment TYPE            : B | 'SB' | H | SH ;
fragment PREFIX          : SFLAG | 'q' | SH | 'u' | 'uq' | 'uh' ;
fragment ADDRMODE        : 'ia' | 'ib' | 'da' | 'db' ;

SH                       : 'sh' ;
COND                     : 'eq'|'ne'|'cs'|'hs'|'cc'|'lo'|'mi'|'pl'|'vs'|'vc'|'hi'|'ls'|'ge'|'lt'|'gt'|'le'|'al' ;
SPEC                     : ('LE' | 'BE') ;

// =============================================================================
// IDENTIFIERS, NUMBERS & STRINGS
// =============================================================================
ID                       : [A-Za-z_ÄÖÜäöüß][A-Za-z0-9_ÄÖÜäöüß]* ;
INT                      : [0-9]+ ;
INT_HEX                  : '0' [xX] [0-9a-fA-F]+ ;
INT_BIN                  : '0' [bB] [01]+ ;
STRING                   : '"' (~["\r\n])* '"' | '\'' (~['\r\n])* '\'' ;

// =============================================================================
// PUNCTUATION & OPERATORS
// =============================================================================
PLUS                     : '+' ;
MINUS                    : '-' ;
STAR                     : '*' ;
SLASH                    : '/' ;
MOD                      : '%' ;
ET                       : '&' ;
TILDE                    : '~' ;
PIPE                     : '|' ;
XOR                      : '^' ;
LSHIFT                   : '<<' ;
RSHIFT                   : '>>' ;

LPAREN                   : '(' ;
RPAREN                   : ')' ;
LBRACKET                 : '{' ;
RBRACKET                 : '}' ;
LSQRBRACKET              : '[' ;
RSQRBRACKET              : ']' ;
EXCLAMATION              : '!' ;
COMMA                    : ',' ;
COLON                    : ':' ;
HASH                     : '#' ;
ASSIGN                   : '=' ;

EQ                       : '==' ;
NE                       : '!=' ;
LE                       : '<=' ;
GE                       : '>=' ;
LT                       : '<' ;
GT                       : '>' ;

// =============================================================================
// WHITESPACE & COMMENTS
// =============================================================================
WS                       : [ \t\r\n]+                                                       -> skip ;
LINE_COMMENT             : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT            : '/*' .*? '*/'                                                    -> skip ;

// =============================================================================
// OPERANDS MODE
// =============================================================================
mode OPERANDS;

// --- Formatting & Comments ---
OPERANDS_NEWLINE         : ('\r'? '\n')                                                     -> skip, mode(DEFAULT_MODE) ;
OPERANDS_WS              : [ \t]+                                                           -> skip ;
OPERANDS_LINE_COMMENT    : '//' ~[\r\n]* -> skip ;
OPERANDS_BLOCK_COMMENT   : '/*' .*? '*/'                                                    -> skip ;

// --- Registers ---
OPERANDS_PROGRAMCOUNTER  : 'pc'                                                             -> type(PROGRAMCOUNTER) ;
OPERANDS_LINKREGISTER    : 'lr'                                                             -> type(LINKREGISTER) ;
OPERANDS_REGISTER        : ('r' ( [0-9] | '1' [0-5] ))                                      -> type(REGISTER) ;

// --- Numbers & Strings ---
OPERANDS_INT             : [0-9]+                                                           -> type(INT) ;
OPERANDS_INT_HEX         : '0' [xX] [0-9a-fA-F]+                                            -> type(INT_HEX) ;
OPERANDS_INT_BIN         : '0' [bB] [01]+                                                   -> type(INT_BIN) ;
OPERANDS_STRING          : ('"' (~["\r\n])* '"' | '\'' (~['\r\n])* '\'')                    -> type(STRING) ;

// --- Punctuation ---
OPERANDS_HASH            : '#'                                                              -> type(HASH) ;
OPERANDS_ASSIGN          : '='                                                              -> type(ASSIGN) ;
OPERANDS_COMMA           : ','                                                              -> type(COMMA) ;
OPERANDS_COLON           : ':'                                                              -> type(COLON) ;
OPERANDS_EXCL            : '!'                                                              -> type(EXCLAMATION) ;
OPERANDS_LPAREN          : '('                                                              -> type(LPAREN) ;
OPERANDS_RPAREN          : ')'                                                              -> type(RPAREN) ;
OPERANDS_LSQRBRACKET     : '['                                                              -> type(LSQRBRACKET) ;
OPERANDS_RSQRBRACKET     : ']'                                                              -> type(RSQRBRACKET) ;
OPERANDS_LBRACKET        : '{'                                                              -> type(LBRACKET) ;
OPERANDS_RBRACKET        : '}'                                                              -> type(RBRACKET) ;

// --- Math & Logic Operators ---
OPERANDS_PLUS            : '+'                                                              -> type(PLUS) ;
OPERANDS_MINUS           : '-'                                                              -> type(MINUS) ;
OPERANDS_STAR            : '*'                                                              -> type(STAR) ;
OPERANDS_SLASH           : '/'                                                              -> type(SLASH) ;
OPERANDS_MOD             : '%'                                                              -> type(MOD) ;
OPERANDS_TILDE           : '~'                                                              -> type(TILDE) ;
OPERANDS_ET              : '&'                                                              -> type(ET) ;
OPERANDS_PIPE            : '|'                                                              -> type(PIPE) ;
OPERANDS_XOR             : '^'                                                              -> type(XOR) ;
OPERANDS_LSHIFT          : '<<'                                                             -> type(LSHIFT) ;
OPERANDS_RSHIFT          : '>>'                                                             -> type(RSHIFT) ;

// --- Relational Operators ---
OPERANDS_EQ              : '=='                                                             -> type(EQ) ;
OPERANDS_NE              : '!='                                                             -> type(NE) ;
OPERANDS_LE              : '<='                                                             -> type(LE) ;
OPERANDS_GE              : '>='                                                             -> type(GE) ;
OPERANDS_LT              : '<'                                                              -> type(LT) ;
OPERANDS_GT              : '>'                                                              -> type(GT) ;

// --- Keyword Operands ---
OPERANDS_LSL             : 'lsl'                                                            -> type(LSL) ;
OPERANDS_ASR             : 'asr'                                                            -> type(ASR) ;
OPERANDS_LSR             : 'lsr'                                                            -> type(LSR) ;
OPERANDS_ROR             : 'ror'                                                            -> type(ROR) ;
OPERANDS_COND            : ('eq'|'ne'|'cs'|'hs'|'cc'|'lo'|'mi'|'pl'|'vs'|'vc'|'hi'|'ls'|'ge'|'lt'|'gt'|'le'|'al') -> type(COND) ;
OPERANDS_SPEC            : ('LE' | 'BE')                                                    -> type(SPEC) ;

// --- Identifiers ---
OPERANDS_ID              : [A-Za-z_ÄÖÜäöüß][A-Za-z0-9_ÄÖÜäöüß]* -> type(ID) ;