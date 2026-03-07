
/*
	ITCOND:Conditional branches 
	must not appear inside IT-blocks.
*/
	
	//trigger on bne
	it ne
	bne target

	// jump in it-Block
	subs r3, #1
	Ite eq 
	moveq r3, #0
	bne main 
	
	// no jump in it-Block
	subs r3, #1
	Ite eq 
	moveq r3, #0
	movne r4, #1
	
	cmp r4, #1
	beq main 
	
	//must not trigger
	it ne
	movne r1, r1
	bne target
	
	//must not trigger
	it ne
	b target
	
	//must trigger on cbnz
	
	ittt eq
	moveq r0, r0
	moveq r1, r1
	cbnz r2, target

	/*
	    Only set flags (e.g., adds) when they are tested/branched soon after. Rule 2
	*/

	//Flag set, then never used:

    adds r0, r0, #1
    mov  r1, r2
    ldr  r3, [r4]
    nop

    //Flags set, then overwritten by another flag-setting op before use:

    adds r0, r0, #1
    subs r2, r2, #1
    mov  r3, r3
    bne  somewhere

   // Flags set, then destroyed by cmp/tst before any use:

    adds r0, r0, #1
    mov  r5, r5
    cmp  r1, #0
    beq  somewhere

    //Flags set, then an unconditional branch occurs (flags become irrelevant):

    adds r0, r0, #1
    b    somewhere

   // Flags set, many unrelated instructions, then a branch (too late):

    adds r0, r0, #1
    mov  r4, r4
    mov  r5, r5
    mov  r6, r6
    bne  somewhere

    //These should not trigger

    //Flags set, immediately used by conditional branch:
    adds r0, r0, #1
    bne  somewhere

    //Flags set, used by IT (predication):
    adds r0, r0, #1
    it   ne
    movne r1, r2

    //Flags set, used after one harmless instruction:
    adds r0, r0, #1
    mov  r7, r7
    bne  somewhere

/*
    Testing if immediatas have '#'
*/

//should report
movw r0, 12
add r0, r1, 12
mov r0, constVal
orr r5, r5, (1 << 3)

//should not report
movw r0, #12
add r0, r1, r2
add r0, r1, #12
ldr r0, =12
mov r0, #constVal

/*
    Reporting of usage of Absolute Adresses
*/
//should report
ldr r0, =0x20000000
ldr r0, =(0x20000000 + 0x10)
//should not report
ldr r0, =myVar
ldr r0, =(myVar + 4)

/*
    Reporting of short instruction form
*/
add r0, #1              // illegal
and r5, #0xff           // illegal
asr r0, #2              // illegal
lsl r2, r2, #1           // legal
sub r3, r3, r5, lsl #4           // legal
ror r3, r3, #4           // legal

/*
    Use shift instead of multiplying if it is power of 2
*/

 mov r2, #8
 mul r0, r1, r2              // warn → use: lsl r0, r1, #3

 // binary and hex constants via constExpr
 mov r2, #0b1000
 mul r0, r1, r2              // warn → use: lsl r0, r1, #3

 mov r3, #0x20
 mul r4, r5, r3              // warn → use: lsl r4, r5, #5

mov r3, #3
udiv r4, r5, r3             // ok
lsl r0, r1, #3              // ok

/*
Building Constants/Adresses
*/
  // arithmetic build-up
    mov r0, #0x40000000
    add r0, r0, #0x1000        // warn: constant computed at runtime

    mov r1, #16
    sub r1, r1, #1             // warn: constant computed at runtime

    // logic build-up
    mov r2, #0xFF00
    orr r2, r2, #0x00FF        // warn: constant computed at runtime

    mov r3, #0xFFFF
    bic r3, r3, #0x00FF        // warn

    // shift build-up
    mov r4, #1
    lsl r4, r4, #12            // warn: builds 0x1000 at runtime

    mov r5, #0b1
    lsl r5, r5, #8             // warn

    // chained constant folding
    mov r6, #1
    lsl r6, r6, #4             // warn
    add r6, r6, #2             // warn again

    // via literal load
    ldr r7, =0x100
    add r7, r7, #0x20          // warn

    // ------------------------------------------------------------
    // SHOULD NOT WARN: legitimate runtime computation
    // ------------------------------------------------------------

    // variable arithmetic
    add r0, r1, #1             // ok: r1 not known constant
    add r2, r3, r4             // ok

    // address arithmetic / scaling
    lsl r0, r1, #2             // ok: r1 variable

    // movw / movt constant materialization (allowed)
    movw r0, #0x1234           // ok
    movt r0, #0x5678           // ok

    // use of constants without computation
    mov r1, #42                // ok
    mov r2, #0x1000            // ok
    ldr r3, =0x2000            // ok

    // overwrite breaks constant chain
    mov r4, #8
    add r4, r4, #1             // warn
    mov r4, r0                 // clobber
    add r4, r4, #1             // ok