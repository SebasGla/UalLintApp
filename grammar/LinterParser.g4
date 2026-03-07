parser grammar LinterParser;

@header {
    package antlr4gen;
}

options {
    tokenVocab = LinterLexer;
}

// =============================================================================
// TOP-LEVEL PROGRAM STRUCTURE
// =============================================================================

program             : statement* EOF ;

statement
    : reptBlock
    | variableDecl          // label: .word ...
    | dataOnlyDecl          // plain ".word ..." after a label → array elems
    | instruction
    | labelDef instruction?
    | routine
    | equDirective
    | directive
    | MACRO_BLOCK
    | macroCall
    | conditionalBlock
    ;

// =============================================================================
// ROUTINES & CONTROL BLOCKS
// =============================================================================

routine             : THUMBFUNC labelDef routineBody ;

routineBody
    : push routineBlock? pop routineBX                                              #RoutinePopThenBx
    | push routineBlock? pop                                                        #RoutinePopOnly
    | routineBlock? routineBX                                                       #RoutineBxOnly
    ;

routineBlock        : (instruction | labelDef instruction? | conditionalBlock)+ ;

routineBX           : BRANCHX LINKREGISTER ;

regList             : regElem (COMMA regElem)* ;
regElem             : register (MINUS register)? ;

// =============================================================================
// MASTER INSTRUCTION RULE (Alphabetical Order)
// =============================================================================

instruction
    : adrInstr
    | arithmeticInstr
    | bfcInstr
    | bfiInstr
    | bfxInstr
    | bkptInstr
    | branch
    | cbzInstr
    | checkArrayInstr
    | clearExInstr
    | clzInstr
    | compInstr
    | cpsInstr
    | debugInstr
    | divInstr
    | dmbInstr
    | dual16bitmulInstr
    | dualAddInstr
    | dualMultInstr
    | eventInstr
    | extendAddInstr
    | extendInstr
    | itInstr
    | ldrInstr
    | ldrStrMultipleInstr
    | ldrdInstr
    | ldrtInstr
    | logicInstr
    | longMultiplyInstr
    | mov32Instr
    | movInstr
    | movtInstr
    | movwInstr
    | mulASInstr
    | mulInstr
    | negInstr
    | nopInstr
    | packHalfWordInstr
    | parallelOpInstr
    | plInstr
    | pop
    | push
    | reverseInstr
    | rfeInstr
    | rrxIntrstuction
    | saturating16Instr
    | saturatingInstr
    | selInstr
    | setendInstr
    | shiftInstruction
    | smalxyInstr
    | smcInstr
    | smlalxyInstr
    | smlawInstr
    | smmlasInstr
    | smmulInstr
    | smulwInstr
    | smulxyInstr
    | strInstr
    | strdInstr
    | strtInstr
    | svcInstr
    | tbbInstr
    | tbhInstr
    | testInstr
    | umaalInstr
    | usad8Instr
    | usada8Instr
    ;

// =============================================================================
// INSTRUCTION DEFINITIONS
// =============================================================================

// --- Data Movement ---
adrInstr            : ADRL register COMMA labelRef ;
mov32Instr          : MOV32 register COMMA op2 ;
movInstr            : MOV register COMMA flexop2 ;
movtInstr           : MOVT register COMMA immediate ;
movwInstr           : MOVW register COMMA immediate ;

// --- Arithmetic & Logic ---
arithmeticInstr     : op=ADDSUB rd=register COMMA rn=register COMMA flexop2         #ArithmLong
                    | op=ADDSUB rd=register COMMA op2                               #ArithmShort
                    ;
divInstr            : DIV register COMMA register (COMMA register)? ;
logicInstr          : op=LOGIC rd=register COMMA rn=register COMMA rm=flexop2       #LogicLong
                    | op=LOGIC rd=register COMMA rn=op2                             #LogicShort
                    ;
negInstr            : NEG register COMMA register ;
rrxIntrstuction     : RRX register COMMA register ;
shiftInstruction    : op=shift rd=register COMMA rn=register COMMA rm=op2           #ShiftLong
                    | op=shift rd=register COMMA rn=op2                             #ShiftShort
                    ;

// --- Multiply & DSP ---
dual16bitmulInstr   : DUAL16BITMUL register COMMA register COMMA register COMMA register ;
dualAddInstr        : DUALADD register COMMA register COMMA register COMMA register ;
dualMultInstr       : DUALMULT register COMMA register COMMA register ;
longMultiplyInstr   : LONGMULTIPLY register COMMA register COMMA register COMMA register ;
mulASInstr          : (MLA | MLS) register COMMA register COMMA register COMMA register ;
mulInstr            : op=MUL rd=register COMMA rn=register COMMA rm=register        #MulLong
                    | op=MUL rd=register COMMA rn=register                          #MulShort
                    ;
parallelOpInstr     : PARALLELOP register COMMA register COMMA register ;
saturating16Instr   : SATURATING16 register COMMA immediate COMMA register ;
saturatingInstr     : SATURATING register COMMA immediate COMMA register (COMMA shiftOperand)? ;
smalxyInstr         : SMALXY register COMMA register COMMA register COMMA register ;
smlalxyInstr        : SMLALXY register COMMA register COMMA register COMMA register ;
smlawInstr          : SMLAW register COMMA register COMMA register COMMA register ;
smmlasInstr         : SMMLAS register COMMA register COMMA register COMMA register ;
smmulInstr          : SMMUL register COMMA register COMMA register ;
smulwInstr          : SMULW register COMMA register COMMA register ;
smulxyInstr         : SMULXY register COMMA register COMMA register ;
umaalInstr          : UMAAL register COMMA register COMMA register COMMA register ;
usad8Instr          : USAD8 register COMMA register COMMA register ;
usada8Instr         : USADA8 register COMMA register COMMA register COMMA register ;

// --- Memory Access ---
ldrInstr            : LDR register COMMA (nondwordOption | memOption2 | memOption3 | memOption4 | shiftOption) ;
ldrdInstr           : LDRD register COMMA register COMMA (dwordOption | memOption2 | memOption3 | memOption4) ;
ldrStrMultipleInstr : LDMSTM register EXCLAMATION? COMMA LBRACKET regList RBRACKET XOR? ;
ldrtInstr           : LDRT register COMMA memOption2 ;
plInstr             : PRELOADDATA (memOption2 | shiftOption | labelRef) ;
pop                 : POP LBRACKET regList RBRACKET ;
push                : PUSH LBRACKET regList RBRACKET ;
strInstr            : STR register COMMA (nondwordOption | memOption2 | memOption3 | memOption4 | shiftOption) ;
strdInstr           : STRD register COMMA register COMMA (dwordOption | memOption2 | memOption3 | memOption4) ;
strtInstr           : STRT register COMMA memOption2 ;

// --- Control Flow ---
branch              : BRANCH labelRef
                    | BRANCHLINK labelRef
                    | BRANCHLINKX (labelRef | register)
                    | BRANCHX register
                    | BRANXJAZELLE register
                    ;
cbzInstr            : CBNZ register COMMA labelRef ;
compInstr           : CMP register COMMA flexop2 ;
itInstr             : IT COND ;
tbbInstr            : TBB LSQRBRACKET register COMMA register RSQRBRACKET ;
tbhInstr            : TBH LSQRBRACKET register COMMA register COMMA LSL immediate RSQRBRACKET ;
testInstr           : TEST register COMMA flexop2 ;

// --- Bitfield & Byte Manipulation ---
bfcInstr            : BFC register COMMA immediate COMMA immediate ;
bfiInstr            : BFI register COMMA register COMMA immediate COMMA immediate ;
bfxInstr            : BFX register COMMA register COMMA immediate COMMA immediate ;
clzInstr            : CLZ register COMMA register ;
extendAddInstr      : EXTENDADD register COMMA register COMMA register (COMMA ROR immediate)? ;
extendInstr         : EXTEND register COMMA register (COMMA ROR immediate)? ;
packHalfWordInstr   : PKH register COMMA register COMMA register (COMMA (LSL | ASR) immediate)? ;
reverseInstr        : REV register COMMA register ;
selInstr            : SEL register COMMA register COMMA register ;

// --- System & Debug ---
bkptInstr           : BKPT immediate ;
checkArrayInstr     : CHKA register COMMA register ;
clearExInstr        : CLREX ;
cpsInstr            : CPS cpsFlags=ID (COMMA immediate)? ;
debugInstr          : DBG immediate ;
dmbInstr            : DMB constPrimary? ;
eventInstr          : EVENTS ;
nopInstr            : NOP ;
rfeInstr            : RFE register EXCLAMATION? ;
setendInstr         : SETEND SPEC ;
smcInstr            : SMC immediate ;
svcInstr            : SVC immediate ;

// =============================================================================
// MEMORY ACCESS OPTIONS
// =============================================================================

nondwordOption      : literal | immediate ; // only applies to ldr
dwordOption         : labelRef | immediate ; // only applies to ldrd
memOption2          : LSQRBRACKET register (COMMA (PLUS | MINUS)? op2)? RSQRBRACKET ;
memOption3          : LSQRBRACKET register COMMA (PLUS | MINUS)? op2 RSQRBRACKET EXCLAMATION ;
memOption4          : LSQRBRACKET register RSQRBRACKET COMMA op2 ;
shiftOperand        : shift op2 ;
shiftOption         : LSQRBRACKET register COMMA (PLUS | MINUS)? op2 COMMA shiftOperand RSQRBRACKET ;

// =============================================================================
// DIRECTIVES & DECLARATIONS
// =============================================================================

variableDecl        : labelDef datatype (COMMA datatype)* ;
dataOnlyDecl        : datatype ;
datatype            : DATATYPE (constExpr (COMMA constExpr)*)? ;

equDirective        : CONSTANT ID COMMA constExpr ;
include             : INCLUDE STRING ;
genericDirective    : GENERICDIRECTIVE constExpr?
                    | GENERICDIRECTIVE constExpr (COMMA constExpr)*
                    ;
directive           : include | TEXT | DATA | genericDirective ;

// =============================================================================
// OPERANDS & BASICS
// =============================================================================

register            : REGISTER | LINKREGISTER | PROGRAMCOUNTER ;
labelDef            : LABEL_DEF ;
labelRef            : ID ;

immediate           : HASH constExpr                                                #ImmediateHash
                    | constExpr                                                     #ImmediateMissingHash
                    ;

literal             : ASSIGN constExpr ;
op2                 : register | immediate | constExpr ;
flexop2             : op2 | (register (COMMA shiftOperand))? ;
shift               : LSL | ASR | LSR | ROR ;

macroCall           : MACRO_NAME (constExpr (COMMA constExpr)*)? ;

// =============================================================================
// MACROS & CONDITIONALS
// =============================================================================

reptBlock
    : labelDef? REPT constExpr
      statement*
      ENDR
    ;

conditionalBlock
    : ifBlock
    | ifdefBlock
    | ifndefBlock
    ;

ifBlock             : IFDIR constExpr statement* elseIfBlock* elseBlock? ENDIFDIR ;
elseIfBlock         : ELSEIFDIR constExpr statement* ;
elseBlock           : ELSEDIR statement* ;
ifdefBlock          : IFDEF ID statement* elseBlock? ENDIFDIR ;
ifndefBlock         : IFNDEF ID statement* elseBlock? ENDIFDIR ;

// =============================================================================
// CONSTANT EXPRESSIONS
// =============================================================================

constExpr
    // equality: ==, !=
    : constExpr op=(EQ | NE) constExpr                                              #ConstEq

    // relational: <, <=, >, >=
    | constExpr op=(LT | LE | GT | GE) constExpr                                    #ConstRel

    // bitwise OR: |
    | constExpr op=PIPE constExpr                                                   #ConstOr

    // bitwise XOR: ^
    | constExpr op=XOR constExpr                                                    #ConstXor

    // bitwise AND: &
    | constExpr op=ET constExpr                                                     #ConstAnd

    // shifts: <<, >>
    | constExpr op=(LSHIFT | RSHIFT) constExpr                                      #ConstShift

    // add/sub: +, -
    | constExpr op=(PLUS | MINUS) constExpr                                         #ConstAdd

    // mul/div/mod: *, /, %
    | constExpr op=(STAR | SLASH | MOD) constExpr                                   #ConstMul

    // unary +, -, ~
    | op=(PLUS | MINUS | TILDE) constExpr                                           #ConstUnary

    // base case
    | constPrimary                                                                  #ConstAtom
    ;

constPrimary
    : INT
    | INT_HEX
    | INT_BIN
    | ID
    | STRING
    | LPAREN constExpr RPAREN
    ;