package app;

public enum Rules
{
    BlankEof,   // The source file must not end with an empty line.
    CondFlags,  // Only set flags (e.g., adds) when they are tested/branched soon after.
    ItBlock,     // Conditional branches must not appear inside IT-blocks.
    AbsoluteAddress,   // Use labels/symbols instead of magic addresses for variables (and constants)
    CONSTANTCOMP,   // Use .equ/expressions for constants, not runtime math.
    RegisterFromManualEqu,      //.equ Register names and addresses are he same to the names in the manual
    ImmediateHash,  // All immediates require the # sigil.
    ShiftPow2,    //Multiplication/division by 2ⁿ must use shifts.
    LogicalOp, // For bitwise ops use and/orr/eor/bic/lsl/lsr, no add/sub tricks.
    ShortInstructionForm, // Only use Instructions with complete register list
    ThumbFunc,  //Every subroutine and ISR must be annotated with .thumb_func.
    RecursiveRoutine, //Functions must not call themselves directly/indirectly.
    NoCondReturns, //Functions must have a single unconditional return path.
    ParamsAAPCS, // Arguments must be passed in r0–r3 in ascending order (AAPCS).
    RoutineReturn //Function results must be returned in r0 (r1:r0 if 64-bit).

}
