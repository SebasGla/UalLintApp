.syntax unified
.thumb

// ------------------------------------------------------------
// rule 13 testcases
// function results must be returned in r0 (r1:r0 if 64-bit)
// ------------------------------------------------------------

// ------------------------------------------------------------
// ok: 32-bit return in r0
// ------------------------------------------------------------
.thumb_func
ret_ok_r0:
	mov r0, #42
	bx lr

// ------------------------------------------------------------
// fail: returns value in r1, r0 is untouched
// expected: diagnostic (return must be in r0)
// ------------------------------------------------------------
.thumb_func
ret_fail_r1:
	mov r1, #42
	bx lr

// ------------------------------------------------------------
// fail: returns value in r2
// expected: diagnostic (return must be in r0)
// ------------------------------------------------------------
.thumb_func
ret_fail_r2:
	mov r2, #7
	bx lr

// ------------------------------------------------------------
// ok: value computed in r2 but copied to r0 before return
// ------------------------------------------------------------
.thumb_func
ret_ok_copy_to_r0:
	mov r2, #7
	mov r0, r2
	bx lr

// ------------------------------------------------------------
// ok: 64-bit return in r1:r0
// ------------------------------------------------------------
.thumb_func
ret_ok_r1_r0:
	mov r0, #0x78
	mov r1, #0x56
	bx lr

// ------------------------------------------------------------
// fail: 64-bit return in wrong pair (r3:r2)
// expected: diagnostic
// ------------------------------------------------------------
.thumb_func
ret_fail_r3_r2_pair:
	mov r2, #0x11
	mov r3, #0x22
	bx lr

// ------------------------------------------------------------
// ok: last meaningful write is in r0 even if earlier wrote r1
// should not complain
// ------------------------------------------------------------
.thumb_func
ret_ok_last_write_r0:
	mov r1, #99
	mov r0, #1
	bx lr

// ------------------------------------------------------------
// ok: store uses r2, so r2 write is not "dead"
// return value is set in r0
// ------------------------------------------------------------
.thumb_func
ret_ok_store_uses_r2:
	ldr r2, =0x20000000
	mov r0, #5
	str r0, [r2]
	bx lr

// ------------------------------------------------------------
// fail: last dead write is r3 (looks like return is in r3)
// expected: diagnostic
// ------------------------------------------------------------
.thumb_func
ret_fail_dead_write_r3:
	mov r0, #1
	add r3, r0, #2
	bx lr

// ------------------------------------------------------------
// ok: computed in r3, but moved into r0 at the end
// ------------------------------------------------------------
.thumb_func
ret_ok_move_from_r3:
	mov r0, #1
	add r3, r0, #2
	movne r0, r3
	bx lr

// ------------------------------------------------------------
// ok: "void-like" routine (no obvious return value)
// your heuristic should usually produce no diagnostic
// ------------------------------------------------------------
.thumb_func
ret_ok_void_like:
	push {lr}
	nop
	pop {pc}

.thumb_func
wait_for_IICIF:
	ldr r0, =2000				// initialize loop count
	ldr r1, =I2C0_S				// load address of I2C0_S
	mov r3, #0x02				// save bit position of I2C0_S[IICIF]
loop_wait_for_IICIF:
	ldrb r2, [r1]				// check if I2C0_S[IICIF] is set
	ands r2, r2, r3
	ittt ne
	strbne r3, [r1]				// clear I2C0_S[IICIF]
	movne r0, #1				// return value "success"
	bne endof_wait_for_IICIF	// return
	
	subs r0, r0, #1				// decrement counter
	bne loop_wait_for_IICIF		// repeat loop until we reach counter value 0
endof_wait_for_IICIF:
	mov r3, #0x10				// save bit position of I2C0_S[ARBL]
	ldrb r2, [r1]				// check if I2C0_S[ARBL] is set
	ands r2, r2, r3
	itt ne
	strbne r3, [r1]				// clear I2C0_S[ARBL]
	movne r0, #0				// return value "failure"

	bx lr						// return