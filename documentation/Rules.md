UAL Linter Rules Documentation

This document provides an overview of all linting rules implemented in the ARM UAL (Unified Assembler Language) Linter, including their purpose, use cases, and technical implementation details.

1. BlankEof

Description & Purpose: Ensures that the source file does not end with trailing whitespaces or completely blank lines. This prevents messy version control diffs and enforces clean file formatting.

Implementation: This is a static, raw text check (BlankEofRule.check), meaning it runs directly on the source string before the AST is evaluated. It walks backward from the end of the file (sourceText.length() - 1) to detect \n, \r, \t, and spaces, calculating the exact line and column numbers manually to report the findings.

2. ItBlock

Description & Purpose: Ensures that conditional branch instructions (like BEQ or CBZ) are not placed inside IT (If-Then) blocks. Placing branches inside IT blocks is generally invalid or highly discouraged in ARM Thumb assembly as it breaks predictable control flow.

Implementation: The ItBlockLintListener hooks into exitItInstr to initialize a countdown (itRemaining) based on the length of the IT mnemonic (e.g., ITT = 2 following instructions). For every subsequent exitInstruction, the counter decrements. If a branch instruction is parsed while itRemaining > 0, a violation is reported.

3. CondFlags

Description & Purpose: Detects when condition flags are set (e.g., via ADDS, SUBS, CMP) but never actually used. Warns if flags are overwritten, abandoned by an unconditional jump, or just ignored, helping catch dead code and logic bugs.

Implementation: The CondFlagsLintListener tracks a pendingProducer token and a 3-instruction LOOKAHEAD countdown. On every exitInstruction, it classifies the instruction: if it's an isFlagConsumer (like a conditional branch or IT block), the pending state clears. If it's an isFlagClobberer (another CMP or S-suffixed instruction) or an unconditional branch, a warning is emitted.

4. ImmediateHash

Description & Purpose: Enforces the use of the # prefix for immediate numerical values (e.g., #4 instead of 4). This makes it immediately obvious to the reader whether an operand is a memory offset, register, or raw constant.

Implementation: Driven entirely by the ANTLR grammar. The parser explicitly categorizes hashed and unhashed immediates into separate branches (#ImmediateHash vs #ImmediateMissingHash). The ImmediateHashLintListener simply listens for exitImmediateMissingHash and flags the token.

5. AbsoluteAddress

Description & Purpose: Discourages the use of "magic" hardcoded memory addresses (e.g., LDR r0, =0x40000000). Suggests using named symbols, labels, or .equ definitions instead for better maintainability.

Implementation: The AbsoluteAddressLintListener hooks into exitLiteral. It recursively scans the AST's TerminalNode children inside a ConstExprContext. If it finds a hexadecimal integer token (INT_HEX) but no identifier tokens (ID), it deduces the value is an anonymous hardcoded address.

6. ShortInstructionForm (LongForm)

Description & Purpose: Forbids shorthand assembly forms (like ADD R1, R2) in favor of the explicit, long-form syntax (like ADD R1, R1, R2). This removes ambiguity and makes the code strictly explicit.

Implementation: The LongFormLintListener hooks into exitInstruction and checks if the AST branch matches ANTLR labels indicating shorthand forms (#ArithmShort, #LogicShort, #ShiftShort, #MulShort). It dynamically reconstructs the correct long-form text and includes it in the warning message.

7. ShiftPow2

Description & Purpose: Optimizes performance by warning when multiplication (MUL) or division (UDIV, SDIV) by a power of two is used. Suggests using the equivalent, much faster bit-shift instructions (LSL, LSR, ASR).

Implementation: The ShiftPow2LintListener tracks register states using a ConstBinding map with a short Time-To-Live (TTL = 2 instructions). It "learns" constants from MOV, MOVW, and LDR literals. If a subsequent MUL or DIV references a tracked register, and its value is 2^n, it calculates log2(value) to suggest the exact shift instruction.

8. ThumbFunc

Description & Purpose: Ensures that the .thumb_func directive precedes routine labels. Without this, the assembler might not interwork ARM/Thumb states correctly, and the parser might "swallow" distinct functions into one massive block.

Implementation: Because missing .thumb_func directives break the parser's concept of a RoutineContext, the ThumbFuncLintListener relies heavily on raw token stream heuristics. It hooks into exitPush, exitBranch, and exitLabelDef. It scans backward through the CommonTokenStream to ensure labels have the directive above them. It also looks ahead for patterns like PUSH {lr} followed by BX LR to detect swallowed routines.

9. NoCondReturns

Description & Purpose: Enforces MISRA compliance and strict control flow by requiring a single, unconditional return point per function. Bans multiple BX LR statements or conditional returns like BXNE LR.

Implementation: The NoCondReturnsLintListener maintains a RoutineState across enterRoutine and exitRoutine. It hooks into exitRoutineBX, exitPop (checking if pc is popped), and exitBranch. It counts the number of return sites and evaluates the length of the branch mnemonic (e.g., length > 2 for bxeq) to flag conditionals and multiples.

10. ParamsAAPCS

Description & Purpose: Enforces the ARM Architecture Procedure Call Standard (AAPCS). Ensures that callee-saved registers (r4-r11) are preserved on the stack if used, and that parameter registers (r0-r3) are utilized in strictly ascending order without gaps.

Implementation: A complex state tracker (RoutineState) spanning enterRoutine and exitRoutine. exitPush records registers saved in the prologue. exitRegister records every register usage. The listener traverses up the AST (isDestinationRegisterOccurrence) to determine if r0-r3 are written to or read from first, allowing it to infer which registers act as incoming parameters versus local variables.

11. RegisterFromManualEqu

Description & Purpose: Cross-references user-defined constants (.equ) with a known Hardware Register Look-Up Table (LUT). Prevents hardware typos by warning if an address maps to a different peripheral name, or if a known peripheral name is mapped to the wrong address.

Implementation: The RegisterManualEquLintListener hooks into exitEquDirective. It maintains addrToName and nameToAddr HashMaps. When it parses an .equ, it extracts the raw hex address and validates it bidirectionally against the LUT, issuing specific error messages depending on whether the name or the address is faulty.

12. RoutineReturn

Description & Purpose: Validates AAPCS return conventions, ensuring that a function properly stores its return value in r0 (or r1:r0 for 64-bit values) immediately before exiting.

Implementation: The RoutineReturnLintListener evaluates the function body during exitRoutine. It isolates the return site (BX or POP PC), then walks backward through the RoutineBlockContext instructions. It tracks the last two instructions that wrote to registers, actively ignoring registers that were just being restored (poppedRegs) or were consumed by intermediate steps (isConsumedLater). Finally, it verifies if the surviving writes targeted r0 or r1.

13. LogicalOp

Description & Purpose: Detects when mathematical operations are misused for bitwise masking logic. For example, adding #4 instead of ORR #4, or multiplying by #0xFFFFFFFF instead of using BIC or AND.

Implementation: The LogicalOpLintListener maintains short-lived state (lastMaskMovLine, lastMaskMovReg) across instructions. exitMovInstr flags if a register is loaded with a mask-like value (power of 2, or 2^n - 1). Subsequent calls to exitMulLong, exitDivInstr, or exitArithmLong check if they utilize the tracked mask register, emitting warnings advising logical alternatives.

14. RecursiveRoutine

Description & Purpose: Detects recursive function calls. Recursion is prohibited in many embedded standards because it causes unpredictable stack depth and risks stack overflow on resource-constrained hardware.

Implementation: The RecursiveRoutineLintListener hooks into enterRoutine. It extracts the routine's label name. It then uses a custom AST walker (collectBranches) to find every BranchContext nested inside the function's RoutineBodyContext. It compares the string targets of those branches against the parent function's name.