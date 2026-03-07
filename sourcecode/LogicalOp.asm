.syntax unified
.thumb

.text
.global test_logicalop_v2
.thumb_func
test_logicalop_v2:
    push {lr}

    // ============================================================
    // ADD/SUB: SHOULD NOT WARN (exception for #1)
    // ============================================================

    add r0, r0, #1
    sub r1, r1, #1
    adds r2, r2, #1
    subs r3, r3, #1

    // ============================================================
    // ADD/SUB: SHOULD WARN (pow2 immediates except 1)
    // ============================================================

    add r4, r4, #2
    sub r5, r5, #4
    adds r6, r6, #8
    subs r7, r7, #16

    add r0, r0, #0x20
    sub r1, r1, #0x80
    add r2, r2, #0x100
    sub r3, r3, #0x80000000

    // pow2 via evaluable constExpr
    add r4, r4, #(1 << 5)        // 32
    sub r5, r5, #(2 * 8)         // 16
    add r6, r6, #((4 + 4))       // 8
    sub r7, r7, #((0x10))        // 16

    // ============================================================
    // ADD/SUB: SHOULD NOT WARN (not pow2)
    // ============================================================

    add r0, r0, #3
    sub r1, r1, #6
    adds r2, r2, #12
    subs r3, r3, #0x30
    add r4, r4, #0xff
    sub r5, r5, #0x81

    // ============================================================
    // MUL/DIV: SHOULD WARN (previous line MOV loads mask-like const,
    //                       and MUL/DIV uses that register)
    // ============================================================

    mov r6, #8
    mul r0, r1, r6

    mov r7, #0x20
    mul r2, r3, r7

    mov r4, #0xff
    udiv r5, r5, r4

    mov r2, #0xf
    sdiv r1, r1, r2

    // pow2-1 via expression
    mov r3, #((1 << 8) - 1)
    udiv r0, r0, r3

    // ============================================================
    // MUL/DIV: SHOULD NOT WARN (mask-like MOV exists, but MUL/DIV does
    //                           not use that register)
    // ============================================================

    mov r7, #0xff
    mul r0, r1, r6

    mov r2, #8
    udiv r3, r3, r4

    // ============================================================
    // MUL/DIV: SHOULD NOT WARN (MOV is not immediately previous line)
    // ============================================================

    mov r5, #8
    nop
    mul r0, r1, r5

    mov r6, #0xff
    nop
    udiv r2, r2, r6

    // ============================================================
    // MUL/DIV: SHOULD NOT WARN (MOV previous line but not mask-like)
    // ============================================================

    mov r4, #7
    mul r0, r1, r4

    mov r6, #10
    udiv r2, r2, r6

    // ============================================================
    // constExpr not evaluable: should not create mask-mov trigger
    // ============================================================

    mov r7, #some_symbol
    mul r0, r1, r7

    pop {pc}

.data
some_symbol:
    .word 0
