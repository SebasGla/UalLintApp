package app;

public enum Rules
{
    BLANKEOF,   // The source file must not end with an empty line.
    CONDFLAGS,  // Only set flags (e.g., adds) when they are tested/branched soon after.
    ITCOND,     // Conditional branches must not appear inside IT-blocks.
    ABSOLUTEADDR,   // Use labels/symbols instead of magic addresses for variables (and constants)
    CONSTANTCOMP,   // Use .equ/expressions for constants, not runtime math.
    REGMANUAL,      //.equ Register names and addresses are he same to the names in the manual
    IMMEDIATE,  // All immediates require the # sigil.
    SHIFTPOW2,    //Multiplication/division by 2ⁿ must use shifts.
    LOGICALOP, // For bitwise ops use and/orr/eor/bic/lsl/lsr, no add/sub tricks.
    FULLINSTRUCTIONS, // Only use Instructions with complete register list
    THUMBFUNC,  //Every subroutine and ISR must be annotated with .thumb_func.
    RECURSION, //Functions must not call themselves directly/indirectly.
    CONDRETURNS, //Functions must have a single unconditional return path.
    ROUTINEPARAMS, // Arguments must be passed in r0–r3 in ascending order (AAPCS).
    ROUTINERETURN //Function results must be returned in r0 (r1:r0 if 64-bit).

}
