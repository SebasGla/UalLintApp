lexer grammar LinterLexer;
@header {
package antlr4gen;
}

options { caseInsensitive=true; }

MACRO_BLOCK: '.macro' .*? '.endm';
MACRO_NAME: '__' ID -> mode(OPERANDS);

// Label definitions
LABEL_DEF: ID ':';
ADDSUB: ('add'|'sub'|'adc'|'rsb'|'sbc'|'rsc') SFLAG? COND? -> mode(OPERANDS);
ADRL: ('ADR' | 'ADRL') COND? WIDTH?-> mode(OPERANDS);               //ADR(L){cond}{.W}
BRANCH:   B COND? -> mode(OPERANDS);
BRANCHLINK: 'bl' COND? -> mode(OPERANDS);                 //branch with link
BRANCHLINKX: 'blx' COND? -> mode(OPERANDS);              //Branch with link, and exchange instruction set
BRANCHX: 'bx' COND? -> mode(OPERANDS);                  //Branch and exchange instruction set
BRANXJAZELLE: 'bxj' COND? -> mode(OPERANDS);            //Branch, and change to Jazelle execution
BKPT: 'bkpt' -> mode(OPERANDS);                       //Breakpoint instruction
CPS: 'cps' ('ie' | 'id') -> mode(OPERANDS);          //change processor stategie
NEG: 'neg' SFLAG? -> mode(OPERANDS);                  //No ARM/THUMB UAL documentation found
CBNZ: ('cbnz' | 'cbz') -> mode(OPERANDS);
CLZ: 'CLZ' COND? -> mode(OPERANDS);
//compare and branch instruction
CMP : ('cmp'|'cmn') COND? -> mode(OPERANDS);

//Compare and Compare Negative
MOV: ('mov'|'mvn') SFLAG? COND? -> mode(OPERANDS);

MOVT: 'movt' COND? -> mode(OPERANDS);                 //Move Top
MOVW: 'movw' COND? -> mode(OPERANDS);             //No ARM/THUMB UAL documentation found
MOV32: 'mov32' COND? -> mode(OPERANDS);
MUL: 'mul' SFLAG? COND? -> mode(OPERANDS);       //Multiply
MLA: 'mla' SFLAG? COND? -> mode(OPERANDS);        //Multiply and accumulate
MLS: 'mls' COND? -> mode(OPERANDS);           //Multiply and Substract
DIV: ('udiv'|'sdiv') COND? -> mode(OPERANDS);

LDR: 'ldr'  TYPE? COND? WIDTH? -> mode(OPERANDS);
LDRD: 'ldrd' COND? -> mode(OPERANDS);
STR: 'str'  TYPE? COND? -> mode(OPERANDS);
STRD: 'strd' COND? -> mode(OPERANDS);
LDRT: 'ldr' TYPE? T COND? -> mode(OPERANDS); //ldr unprivileged
STRT: 'str' TYPE? T COND? -> mode(OPERANDS); // str unprivileged
LSL:'lsl'  SFLAG? COND? -> mode(OPERANDS);
ASR: 'asr'  SFLAG? COND? -> mode(OPERANDS);
LSR: 'lsr' SFLAG? COND? -> mode(OPERANDS);
ROR:'ror' SFLAG? COND? -> mode(OPERANDS);
RRX: 'rrx' SFLAG? COND? -> mode(OPERANDS);
LOGIC: ('and'|'orr'|'eor'|'bic'|'orn') SFLAG? COND? -> mode(OPERANDS);
TEST: ('tst'|'teq') COND? -> mode(OPERANDS);
IT: 'it' ('t'|'e')?  ('t'|'e')?  ('t'|'e')? -> mode(OPERANDS);
SEL: 'sel' COND? -> mode(OPERANDS);
BFX: ('SBFX' | 'UBFX') COND? -> mode(OPERANDS);
BFC: 'bfc' COND? -> mode(OPERANDS);
BFI: 'bfi' COND? -> mode(OPERANDS);
NOP: 'nop' COND? -> mode(OPERANDS);
LDMSTM: ('ldm' | 'stm') ADDRMODE? COND? -> mode(OPERANDS); //Load and Store Multiple registers
DUALMULT: ('SMUAD'|'SMUSD') X? COND? -> mode(OPERANDS);
DUALADD: ('SMLAD'|'SMLSD') X? COND? -> mode(OPERANDS);  //Dual 16-bit Signed Multiply with Addition or Subtraction of products and 32-bit accumulation
SATURATING16: ('SSAT16' | 'USAT16') COND? -> mode(OPERANDS);
SATURATING: ('SSAT' | 'USAT') COND? -> mode(OPERANDS);
SMULXY: 'smul' (B|T) (B|T) COND? -> mode(OPERANDS);
SMALXY: 'smla' (B|T) (B|T) COND? -> mode(OPERANDS);
PKH:    ('pkhbt' | 'pkhtb') COND?;  //Halfword Packing instructions.
REV: ('rev'| 'rev16'| 'revsh' | 'rbit') COND? -> mode(OPERANDS);
SMMUL: 'SMMUL' R? COND? -> mode(OPERANDS);
SMMLAS: ('SMMLA' | 'SMMLS') R? COND? -> mode(OPERANDS);
EXTEND: ('SXT' | 'UXT') (B16 | B | H) COND? -> mode(OPERANDS);
EXTENDADD: ('SXTA' | 'UXTA') (B16 | B | H) COND? -> mode(OPERANDS);
EVENTS: ('SEV' | 'WFE' | 'WFI' |'YIELD') COND? -> mode(OPERANDS);
PARALLELOP: PREFIX ('add8' | 'add16' | 'sub8' | 'sub16' | 'asx' | 'sax') COND? -> mode(OPERANDS); // <prefix>op{cond} {Rd}, Rn, Rm
LONGMULTIPLY: ('UMULL' | 'UMLAL' | 'SMULL' | 'SMLAL') SFLAG? COND? -> mode(OPERANDS); //Signed and Unsigned Long Multiply, with optional Accumulate
TBB: 'tbb' -> mode(OPERANDS);
TBH: 'tbh'-> mode(OPERANDS);
CHKA: 'CHKA' -> mode(OPERANDS); //CHKA (Check Array) compares the unsigned values in two registers
CLREX: 'CLREX' COND?; // Clear Exclusive.
DBG: 'DBG' COND?; //DEBUG
DMB: ('DMB' | 'DSB' | 'ISB') COND? -> mode(OPERANDS); //Data Memory Barrier.
PRELOADDATA: ('PLD' | 'PLDW' | 'PLI') COND? -> mode(OPERANDS); //Preload Data and Preload Instruction
RFE: 'RFE' ADDRMODE? COND? -> mode(OPERANDS);
SETEND: 'SETEND' -> mode(OPERANDS);
SMC: 'SMC' COND?  -> mode(OPERANDS);
SMLALXY: 'SMLAL' (B | T) ( B| T) COND? -> mode(OPERANDS);
DUAL16BITMUL: ('SMLALD' | 'SMLSLD') X? COND?-> mode(OPERANDS);
SMULW: ('SMULW') (B | T) COND? -> mode(OPERANDS);
SMLAW: ('SMLAW') (B | T) COND? -> mode(OPERANDS);
SVC: 'svc' COND? -> mode(OPERANDS);   //supervisor call
UMAAL: 'UMAAL' COND?-> mode(OPERANDS);  //Unsigned Multiply Accumulate Accumulate Long.
USAD8: 'USAD8' COND?-> mode(OPERANDS); //Unsigned Sum of Absolute Differences,
USADA8: 'USADA8' COND?-> mode(OPERANDS); //and Accumulate unsigned sum of absolute differences.

PROGRAMCOUNTER: 'pc';
LINKREGISTER: 'lr';

//special directives
CONSTANT: '.equ' -> mode(OPERANDS);

INCLUDE: '.include' -> mode(OPERANDS);

TEXT: '.text' -> mode(OPERANDS);

DATA:  '.data' -> mode(OPERANDS);

THUMBFUNC: '.thumb_func' -> mode(OPERANDS);

REPT: '.rept' -> mode(OPERANDS);

ENDR: '.endr' -> mode(OPERANDS);

DATATYPE: ('.byte'  | '.asciz' | '.word' | '.hword' | '.quad' | '.4byte' | '.2byte' | '.long') -> mode(OPERANDS);

WIDTH: '.W';                //is an optional instruction width specifier to force the use of a 32-bit B instruction in Thumb-2.

IFDIR     : '.if' -> mode(OPERANDS);

ELSEIFDIR : '.elseif' -> mode(OPERANDS);

ELSEDIR   : '.else' -> mode(OPERANDS);

ENDIFDIR  : '.endif' -> mode(OPERANDS);

IFDEF      : '.ifdef' -> mode(OPERANDS);

IFNDEF     : '.ifndef' -> mode(OPERANDS);

//everything else directives
GENERICDIRECTIVE: '.' [A-Z]+ -> mode(OPERANDS);

fragment SFLAG: 's';
fragment X: 'x';
fragment B: 'b';
fragment B16: 'b16';
fragment H: 'H';
fragment T: 't';
fragment R: 'r';
SH: 'sh';
fragment DWORD: 'd';
COND:'eq'|'ne'|'cs'|'hs'|'cc'|'lo'|'mi'|'pl'|'vs'|'vc'|'hi'|'ls'|'ge'|'lt'|'gt'|'le'|'al';
fragment TYPE: B | 'SB' | H | SH;
fragment PREFIX: SFLAG | 'q'  | SH | 'u' | 'uq' | 'uh';
PUSH: 'push' COND? -> mode(OPERANDS);

POP: 'pop' COND? -> mode(OPERANDS);

fragment ADDRMODE: 'ia' | 'ib' | 'da' | 'db';


REGISTER: ('r' ( [0-9] | '1' [0-5] ));  // r0..r15 or lr


ID: [A-Za-z_ÄÖÜäöüß][A-Za-z0-9_ÄÖÜäöüß]*;   //variables/labels letters and numbers
INT: [0-9]+;
INT_HEX  : '0' [xX] [0-9a-fA-F]+ ;
INT_BIN  : '0' [bB] [01]+ ;
PLUS     : '+' ;
MINUS    : '-' ;
STAR      : '*' ;
SLASH      : '/' ;
MOD    : '%' ;
ET      : '&' ;
TILDE:  '~';
PIPE  : '|' ;
XOR      : '^' ;
LSHIFT   : '<<' ;
RSHIFT   : '>>' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '{';
RBRACKET : '}';
LSQRBRACKET: '[';
RSQRBRACKET: ']';
EXCLAMATION: '!';
COMMA: ',';
COLON: ':';
HASH: '#';
ASSIGN: '=';
EQ  : '==';
NE  : '!=';
LE  : '<=';
GE  : '>=';
LT  : '<';
GT  : '>';
SPEC: ('LE' | 'BE');


STRING: '"' (~["\r\n])* '"' | '\'' (~['\r\n])* '\'';

// Whitespace & comments
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
// ---------- operands mode ----------
mode OPERANDS;

OPERANDS_NEWLINE: ('\r'? '\n') -> skip, mode(DEFAULT_MODE);
OPERANDS_WS: [ \t]+ -> skip;

OPERANDS_LINE_COMMENT: '//' ~[\r\n]* -> skip;
OPERANDS_BLOCK_COMMENT: '/*' .*? '*/' -> skip;

// Common tokens needed in operand contexts
OPERANDS_PROGRAMCOUNTER: 'pc' -> type(PROGRAMCOUNTER);
OPERANDS_LINKREGISTER: 'lr' -> type(LINKREGISTER);
OPERANDS_REGISTER: ('r' ( [0-9] | '1' [0-5] )) -> type(REGISTER);

OPERANDS_INT: [0-9]+ -> type(INT);
OPERANDS_INT_HEX  : '0' [xX] [0-9a-fA-F]+ -> type(INT_HEX);
OPERANDS_INT_BIN  : '0' [bB] [01]+ -> type(INT_BIN);

OPERANDS_STRING
    : ( '"'  (~["\r\n])*  '"'
      | '\'' (~['\r\n])* '\''
      ) -> type(STRING)
    ;

OPERANDS_HASH   : '#' -> type(HASH);
OPERANDS_ASSIGN : '=' -> type(ASSIGN);
OPERANDS_COMMA  : ',' -> type(COMMA);
OPERANDS_COLON  : ':' -> type(COLON);
OPERANDS_EXCL   : '!' -> type(EXCLAMATION);

OPERANDS_LPAREN : '(' -> type(LPAREN);
OPERANDS_RPAREN : ')' -> type(RPAREN);
OPERANDS_LSQRBRACKET: '[' -> type(LSQRBRACKET);
OPERANDS_RSQRBRACKET: ']' -> type(RSQRBRACKET);

OPERANDS_LBRACKET: '{' -> type(LBRACKET);
OPERANDS_RBRACKET: '}' -> type(RBRACKET);

OPERANDS_PLUS   : '+' -> type(PLUS);
OPERANDS_MINUS  : '-' -> type(MINUS);
OPERANDS_STAR   : '*' -> type(STAR);
OPERANDS_SLASH  : '/' -> type(SLASH);
OPERANDS_MOD    : '%' -> type(MOD);
OPERANDS_TILDE  : '~' -> type(TILDE);

OPERANDS_ET     : '&' -> type(ET);
OPERANDS_PIPE   : '|' -> type(PIPE);
OPERANDS_XOR    : '^' -> type(XOR);
OPERANDS_LSHIFT : '<<' -> type(LSHIFT);
OPERANDS_RSHIFT : '>>' -> type(RSHIFT);

OPERANDS_EQ     : '==' -> type(EQ);
OPERANDS_NE     : '!=' -> type(NE);
OPERANDS_LE     : '<=' -> type(LE);
OPERANDS_GE     : '>=' -> type(GE);
OPERANDS_LT     : '<'  -> type(LT);
OPERANDS_GT     : '>'  -> type(GT);

OPERANDS_LSL: 'lsl' -> type(LSL);
OPERANDS_ASR: 'asr' -> type(ASR);
OPERANDS_LSR: 'lsr' -> type(LSR);
OPERANDS_ROR: 'ror' -> type(ROR);

OPERANDS_COND
    : ('eq'|'ne'|'cs'|'hs'|'cc'|'lo'|'mi'|'pl'|'vs'|'vc'|'hi'|'ls'|'ge'|'lt'|'gt'|'le'|'al') -> type(COND)
    ;

OPERANDS_SPEC: ('LE' | 'BE') -> type(SPEC);
// Any identifier (including mnemonic spellings) is an ID in operands mode
OPERANDS_ID:  [A-Za-z_ÄÖÜäöüß][A-Za-z0-9_ÄÖÜäöüß]* -> type(ID);
