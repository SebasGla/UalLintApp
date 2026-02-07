parser grammar LinterParser;
@header {
package antlr4gen;
}
options { tokenVocab=LinterLexer;}

// -------- Parser rules --------
program: statement* EOF;

statement:
           reptBlock
         | variableDecl   // label: .word ...
         | dataOnlyDecl   // plain ".word ..." after a label → array elems
         | instruction
         | labelDef instruction?
         | routine
         | equDirective
         | directive
         | MACRO_BLOCK
         | macroCall
         | conditionalBlock
         ;

instruction:  arithmeticInstr
            | movInstr
            | branch
            | adrInstr
            | ldrInstr
            | ldrdInstr
            | strInstr
            | strdInstr
            | logicInstr
            | bkptInstr
            | cbzInstr
            | mulInstr
            | mulASInstr
            | cpsInstr
            | compInstr
            | itInstr
            | bfxInstr
            | bfiInstr
            | bfcInstr
            | shiftInstruction
            | rrxIntrstuction
            | divInstr
            | movtInstr
            | testInstr
            | nopInstr
            | parallelOpInstr
            | selInstr
            | ldrStrMultipleInstr
            | dualMultInstr
            | dualAddInstr
            | saturatingInstr
            | push
            | pop
            | smulxyInstr
            | smalxyInstr
            | packHalfWordInstr
            | reverseInstr
            | smmulInstr
            | smmlasInstr
            | extendAddInstr
            | extendInstr
            | eventInstr
            | movwInstr
            | negInstr
            | clzInstr
            | tbbInstr
            | tbhInstr
            | longMultiplyInstr
            | checkArrayInstr
            | clearExInstr
            | debugInstr
            | dmbInstr
            | ldrtInstr
            | strtInstr
            | mov32Instr
            | plInstr
            | rfeInstr
            | setendInstr
            | smcInstr
            | smlalxyInstr
            | dual16bitmulInstr
            | smlawInstr
            | smulwInstr
            | saturating16Instr
            | svcInstr
            | umaalInstr
            | usad8Instr
            | usada8Instr
            ;

routine
    : THUMBFUNC labelDef routineBody;

routineBody
    :push routineBlock? pop routineBX                      #RoutinePopThenBx
    | push routineBlock? pop                                #RoutinePopOnly
    | routineBlock? routineBX                              #RoutineBxOnly
    ;

routineBlock: (instruction |  labelDef instruction? | conditionalBlock)+;
push: PUSH LBRACKET regList RBRACKET;
pop: POP LBRACKET regList RBRACKET;
routineBX: BRANCHX (LINKREGISTER);

regList: regElem (COMMA regElem)*;
regElem: register (MINUS register)?;

reptBlock
    : labelDef? REPT constExpr
       statement*
      ENDR
    ;

variableDecl: labelDef datatype (COMMA datatype)*;

arithmeticInstr: op=ADDSUB rd=register COMMA rn=register COMMA flexop2  #ArithmLong
                 |op=ADDSUB rd=register COMMA op2                         #ArithmShort; //op{S}{cond} {Rd}, Rn, Operand2
mulInstr: op=MUL rd=register COMMA rn=register COMMA rm=register #MulLong
          |op=MUL rd=register COMMA rn=register                  #MulShort; // MUL{S}{cond} {Rd}, Rn, Rm
mulASInstr: (MLA | MLS) register COMMA register COMMA register COMMA register; // MLA{S}{cond} Rd, Rn, Rm, Ra or MLS{cond} Rd, Rn, Rm, Ra
adrInstr: ADRL register COMMA labelRef;
movInstr:  MOV register COMMA flexop2;
movtInstr: MOVT register COMMA immediate;
mov32Instr: MOV32 register COMMA op2;       //MOV32{cond} Rd, expr with expr being a symbol, #const or symbol + const
branch: BRANCH labelRef | BRANCHLINK labelRef | BRANCHLINKX (labelRef | register) | BRANCHX register | BRANXJAZELLE register;
bkptInstr: BKPT immediate;
cpsInstr: CPS cpsFlags=ID (COMMA immediate)?;
cbzInstr: CBNZ register COMMA labelRef;
compInstr: CMP register COMMA flexop2;
ldrInstr: LDR register COMMA (nondwordOption | memOption2 | memOption3 |memOption4 | shiftOption);   //LDR{type}{cond}{.W} Rt, label
strInstr: STR register COMMA (nondwordOption | memOption2 | memOption3 |memOption4 | shiftOption);
ldrdInstr: LDRD register COMMA register COMMA (dwordOption | memOption2 | memOption3 | memOption4); //LDRD{cond} Rt, Rt2, label
strdInstr: STRD register COMMA register COMMA (dwordOption | memOption2 | memOption3 | memOption4); //LDRD{cond} Rt, Rt2, label
ldrtInstr: LDRT register COMMA (memOption2);
strtInstr: STRT register COMMA (memOption2);
itInstr: IT COND;
shift: (LSL | ASR | LSR | ROR);
shiftInstruction: op=shift rd=register COMMA rn=register COMMA rm=op2 #ShiftLong
                  | op=shift rd=register COMMA rn=op2 #ShiftShort;
rrxIntrstuction: RRX register COMMA register;
bfxInstr: BFX register COMMA register COMMA immediate COMMA immediate;
bfiInstr: BFI register COMMA register COMMA immediate COMMA immediate;
bfcInstr: BFC register COMMA immediate COMMA immediate;
divInstr: DIV register COMMA register (COMMA register)?;
testInstr: TEST register COMMA flexop2;
nopInstr: NOP;
selInstr: SEL register COMMA register COMMA register;
plInstr: PRELOADDATA (memOption2 | shiftOption | labelRef);
//options for memory access instructions
nondwordOption: (literal | immediate);      //only applies to ldr
dwordOption: (labelRef | immediate);   //only applies to ldrd
memOption2: LSQRBRACKET register (COMMA (PLUS | MINUS)? op2)? RSQRBRACKET; // ldr r1, [r4] or ldr r1, [r4, #3]
memOption3: LSQRBRACKET register COMMA (PLUS | MINUS)? op2 RSQRBRACKET EXCLAMATION; // ldr r0, [r4, +r6]!
memOption4: LSQRBRACKET register RSQRBRACKET COMMA op2; // ldr r1, [r4], r5
shiftOperand: shift op2;
shiftOption: LSQRBRACKET register COMMA (PLUS | MINUS)? op2 COMMA shiftOperand RSQRBRACKET; //[Rn, +/-Rm {, shift}]

logicInstr: op=LOGIC rd=register COMMA rn=register COMMA rm=flexop2 #LogicLong
            | op=LOGIC rd=register COMMA rn=op2 #LogicShort;    //op{S}{cond} Rd, Rn, Operand2
clzInstr: CLZ register COMMA register;
parallelOpInstr: PARALLELOP register COMMA register COMMA register;
ldrStrMultipleInstr: LDMSTM register EXCLAMATION? COMMA LBRACKET regList RBRACKET XOR?; //op{addr_mode}{cond} Rn{!}, reglist{^}
dualMultInstr: DUALMULT register COMMA register COMMA register;
dualAddInstr: DUALADD register COMMA register COMMA register COMMA register;
saturating16Instr: SATURATING16 register COMMA immediate COMMA register;
saturatingInstr: SATURATING register COMMA immediate COMMA register (COMMA shiftOperand)?;
smulxyInstr: SMULXY register COMMA register COMMA register;
smalxyInstr: SMALXY register COMMA register COMMA register COMMA register;
smmulInstr: SMMUL register COMMA register COMMA register;
smmlasInstr: SMMLAS register COMMA register COMMA register COMMA register;
extendInstr: EXTEND register COMMA register (COMMA ROR immediate)?;
extendAddInstr: EXTENDADD register COMMA register COMMA register (COMMA ROR immediate)?;
eventInstr: EVENTS;
movwInstr: MOVW register COMMA immediate;
negInstr: NEG  register COMMA register;
tbbInstr: TBB LSQRBRACKET register COMMA register RSQRBRACKET;
tbhInstr: TBH LSQRBRACKET register COMMA register COMMA LSL immediate RSQRBRACKET;
packHalfWordInstr: PKH register COMMA register COMMA register (COMMA (LSL | ASR) immediate)?;
reverseInstr: REV register COMMA register;
longMultiplyInstr: LONGMULTIPLY register COMMA register COMMA register COMMA register ;
checkArrayInstr: CHKA register COMMA register;
clearExInstr: CLREX;
debugInstr: DBG immediate;
dmbInstr: DMB constPrimary?; // this checks DMB only for a following string, but doesnt determine if the string is correct
rfeInstr: RFE register EXCLAMATION?;
setendInstr: SETEND SPEC;
smcInstr: SMC immediate;
smlalxyInstr: SMLALXY register COMMA register COMMA register COMMA register;
dual16bitmulInstr: DUAL16BITMUL register COMMA register COMMA register COMMA register;
smulwInstr: SMULW register COMMA register COMMA register;
smlawInstr: SMLAW register COMMA register COMMA register COMMA register;
svcInstr: SVC immediate;
umaalInstr: UMAAL register COMMA register COMMA register COMMA register;
usad8Instr: USAD8 register COMMA register COMMA register;
usada8Instr: USADA8 register COMMA register COMMA register COMMA register;

register: REGISTER | LINKREGISTER | PROGRAMCOUNTER;
labelDef: LABEL_DEF;
labelRef: ID;
equDirective: CONSTANT ID COMMA constExpr;
datatype:  DATATYPE (constExpr (COMMA constExpr)*)?;
dataOnlyDecl: datatype ;
genericDirective: GENERICDIRECTIVE constExpr?| GENERICDIRECTIVE constExpr (COMMA constExpr)*;
include: INCLUDE STRING;
directive: include  | TEXT | DATA | genericDirective ;

immediate: HASH constExpr #ImmediateHash
         | constExpr    #ImmediateMissingHash
         ;

literal: ASSIGN constExpr;
op2: register | immediate | constExpr;
flexop2: op2 |(register (COMMA shiftOperand))?;
macroCall: MACRO_NAME (constExpr (COMMA constExpr)*)?;  //this rule is only to find macros with __

// -------- conditional assembly (.if, .ifdef, .ifndef, .elseif, .else, .endif) --------

conditionalBlock
    : ifBlock
    | ifdefBlock
    | ifndefBlock
    ;

ifBlock
    : IFDIR constExpr          // .if <const expression>
      statement*               // body for .if
      elseIfBlock*             // optional .elseif branches
      elseBlock?               // optional .else
      ENDIFDIR                 // .endif
    ;

elseIfBlock
    : ELSEIFDIR constExpr      // .elseif <const expression>
      statement*
    ;

elseBlock
    : ELSEDIR                  // .else
      statement*
    ;

ifdefBlock
    : IFDEF ID                 // .ifdef SYMBOL
      statement*
      elseBlock?               // optional .else
      ENDIFDIR
    ;

ifndefBlock
    : IFNDEF ID                // .ifndef SYMBOL
      statement*
      elseBlock?               // optional .else
      ENDIFDIR
    ;

constExpr
      // equality: ==, !=
        : constExpr op=(EQ | NE) constExpr               #ConstEq

        // relational: <, <=, >, >=
        | constExpr op=(LT | LE | GT | GE) constExpr     #ConstRel

        // bitwise OR: |
        | constExpr op=PIPE constExpr                    #ConstOr

        // bitwise XOR: ^
        | constExpr op=XOR constExpr                     #ConstXor

        // bitwise AND: &
        | constExpr op=ET constExpr                      #ConstAnd

        // shifts: <<, >>
        | constExpr op=(LSHIFT | RSHIFT) constExpr       #ConstShift

        // add/sub: +, -
        | constExpr op=(PLUS | MINUS) constExpr          #ConstAdd

        // mul/div/mod: *, /, %
        | constExpr op=(STAR | SLASH | MOD) constExpr    #ConstMul

        // unary +, -, ~
        | op=(PLUS | MINUS | TILDE) constExpr                 #ConstUnary

        // base case
        | constPrimary                                   #ConstAtom
        ;

// Leaves: numbers, labels, parentheses, maybe strings
constPrimary
    : INT
    | INT_HEX
    | INT_BIN
    | ID
    | STRING
    | LPAREN constExpr RPAREN
    ;
