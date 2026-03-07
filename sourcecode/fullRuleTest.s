// ============================================================================
// RULE 02 - .thumb_func before routines
// GOOD: routine has .thumb_func directly before label.
// BAD:  routine label appears without .thumb_func.
// ============================================================================

.syntax unified
.thumb

.text

rule02_bad:
    nop
    bx lr

.thumb_func
rule02_good:
    nop
    bx lr

rule02_bad:
    push {lr}
    nop
    pop {pc}


// ============================================================================
// RULE 03 - Set flags only if used
// GOOD: adds sets flags and is tested soon after (bne).
// BAD:  adds sets flags but no conditional use follows -> flags unused.
// ============================================================================

.thumb_func
rule03_good:
    mov r0, #1
    adds r0, r0, #1
    bne rule03_not_zero
    bx lr

.thumb_func
rule03_bad:
    mov r2, #1
    adds r2, r2, #4
    ldr r3, =addr
    str r2, [r3]
    bx lr

// ============================================================================
// RULE 04 - No conditional branch in IT-block
// GOOD: conditional branch is outside IT-block.
// BAD:  conditional branch is inside IT-block (it ne + bne).
// ============================================================================

.thumb_func
rule04_good:
    cmp r0, #0
    it ne
    mov r1, r1
    bne rule04_done
    bx lr

.thumb_func
rule04_bad:
    cmp r0, #0
    it ne
    bne rule04_done_bad
    bx lr


// ============================================================================
// RULE 05 - No conditional returns (single unconditional return path)
// GOOD: one unconditional return.
// BAD:  early conditional return creates multiple exits.
// ============================================================================

.thumb_func
rule05_good:
    mov r0, #0
    add r0, r0, #1
    pop {pc}

.thumb_func
rule05_bad:
    cmp r0, #0
    beq rule05_early_exit

    mov r0, #1
    bx lr

rule05_early_exit:
    bx lr

// ============================================================================
// RULE 06 - No absolute addresses
// GOOD: uses symbol/label via literal load.
// BAD:  uses magic absolute address.
// ============================================================================

.thumb_func
rule06_good:
    ldr r0, =rule06_buffer
    ldr r1, [r0]
    bx lr

.thumb_func
rule06_bad:
    ldr r0, =0x20001000
    ldr r1, [r0]
    bx lr

// ============================================================================
// RULE 07 - Only Langform (UAL long form only)
// GOOD: explicit 3-operand forms.
// BAD:  short forms with implicit source register.
// ============================================================================

.thumb_func
rule07_good:
    mov r0, #1
    add r0, r0, #1
    lsl r0, r0, #2
    bx lr

.thumb_func
rule07_bad:
    mov r0, #1
    add r0, #1
    lsl r0, #2
    bx lr

// ============================================================================
// RULE 08 - Immediate must use #
// GOOD: immediates use #.
// BAD:  immediates omit #.
// ============================================================================

.thumb_func
rule08_good:
    add r0, r0, #3
    mov r1, #10
    bx lr

.thumb_func
rule08_bad:
    add r0, r0, 3
    mov r1, 10
    bx lr

// ============================================================================
// RULE 09 - Use shifts for ×/÷ 2^n
// GOOD: uses lsl/lsr.
// BAD:  uses mul/udiv with power-of-two constant via register.
// ============================================================================

.thumb_func
rule09_good:
    mov r2, r0
    lsl r0, r2, #3

    mov r2, r0
    lsr r0, r2, #2
    bx lr

.thumb_func
rule09_bad:
    mov r2, r0
    mov r1, #8
    mul r0, r2, r1
	
//This will not trigger, because there is one instruction in between the mov and shift. 
//The current look window is hardcoded to 1 at the moment
    mov r1, #4		
    mov r2, r0			//one instrc in between
    udiv r0, r2, r1
    bx lr

// ============================================================================
// RULE 10 - Register names from the manual (.equ name↔address LUT)
// GOOD: name/address match LUT.
// BAD:  (1) address exists but name wrong, (2) name exists but address wrong.
// ============================================================================

// good: example address from your earlier CSV snippet
.equ SIM_SCGC5, 0x40048038

// bad (1): wrong name for a known address
.equ SIM_SCGC6, 0xe0100030

// bad (2): wrong address for a known name
.equ SIM_SCGC5_BAD, 0x40048038

// ============================================================================
// RULE 11 - Params in r0..r3 (AAPCS ascending)
// GOOD: prepares r0 then r1 before bl.
// BAD:  prepares r1 without r0; prepares r2 with missing r1 (gap).
// ============================================================================

.thumb_func
rule11_good_caller:
    push {r4, r5, lr}
    mov r4, r0
    mov r5, r1
    add r0, r4, r5
    pop {r4, r5, pc}

.thumb_func
rule11_bad_caller:
    push {lr}
    mov r4, r0
    mov r5, r1
    add r0, r4, r5
    pop {pc}

.thumb_func
rule11_badOrder
    add r0, r1, r2		//<-- r1 and r2 were recognized as paramters
    bx lr

// ============================================================================
// RULE 01 - No blank line at EOF
// ============================================================================

.thumb_func
rule11_callee_b:
    bx lr 