// Include-Dateien
.include "k1514.inc"			// Praktikumsspezifische Definitionen

// Assemblerdirektiven
.text							// hier beginnt ein Code-Segment
.align	2						// Ausrichtung an eine gerade Adresse

// Konstantendeklarationen	
.equ COUNTER, 0x10			
mov r11, r0

	// Es werden jeweils nur 8 Bit aus v1 und v2
	// veglichen und dann jeweils in min oder max gesetzt

// label and b instruction
main:
    ldr r0, =str
	b main
	bne main
	bx lr
	bx r1
	blx lr

    cmp r11, #1
    cmnge r11, r0

	//arithmetic Instructions
	add r0, r1, #3
	adds r0, r1
	subs r3, pc, #4
	subsge lr, r12, r5

	//ADR instruction
    adr r0, text
    adrl lr, text2
	/*
// bl instruction
	bl	uart_init			
	*/
     orr r1, r1, #12
	//load and store
	ldr r5, =var
    ldr r0, [r5]
    ldr r5, [r0, #4]
    ldr r5, [r0, #4]!
    ldr r5, [r0], #12

    ldrd r4, r5, #2999
    ldrd r4, r5, [r6]
    ldrd r4, r5, [r6, #2]!
    ldrd r4, r5, [r6], #12

    str r0, [r5]
    str r5, [r0, #4]
    str r5, [r0, #4]!
    str r5, [r0], #12

    strd r4, r5, #2999
    strd r4, r5, [r6]
    strd r4, r5, [r6, #2]!
    strd r4, r5, [r6], #12

    ldr r5, [r0, r4, lsl #2]

	//move instructions
	mov r4, #COUNTER		
	movge r5, #1
	movs  r5, r4

	//Breakpoint
	bkpt #1000

	// compare and brach
	cbnz r0,    hello
     // Hier fuer Jahre + Jahrhunderte
    movw r2, #YEARS_CENTURIES_MAX
    usub8 r2, r1, r2
    sel r1, r2, r1
	//multipy
	mulsne r4, r5
	mulge r6, r4, r5

	mlasne r4, r5, r6 , pc
	mls r5,r6,r7,r0

	//cpe instruction
	cpsie a
	cpsid i, #3

	//PKHBT
	PKHBT r4, r2, r5, lsl #2
	PKHBT r4, r2, r5, asr #2
	PKHBT r4, r2, r5

	// rev
	rev16 r0, r5
	revshge r0, r0
	SMMULGE r6, r4, r3
	SMMULR r6, r4, r3
	SMMLS r4, r5, r6, r7
	SMMLAR r4, r5, r6, r7

	//extend and extend add
	SXTH r3, r9
	SXTH r3, r9, ROR #16
    UXTAB16EQ r0, r0, r4, ROR #16

    //events
    SEVGE
    WFI


/*** Datenbereich (ab 0x20000000) ***/
.data

/*bytearray:
.rept COUNTER
.byte 0
.endr
*/

maximum: .byte 0x0
str_prompt: .asciz "\n>0x"
chka r4, r5
dbg #4
dbg 3
dmb abd
dmb
ldrht r4, [r5]
ldrt r4, [r5, #4]
strbt r5, [r5]

MOV32 r3, #0xABCDEF12
MOV32 r1, Trigger+12
plDne [r5]
pldw [r6, -r6, lsl #2]
pli hallo
rfeIAne r4!
SETEND BE
smcge #5
SMLALTB r2, r3, r7, r1
SMLALBTVS r0, r1, r9, r2
SMLALDXLE r10, r11, r5, r1
SMLSLD r3, r0, r5, r1
SMLAWT r3, r0, r5, r1
SMULWB r3, r5, r5
USATNE r0, #7, r5
svc #3
USAD8 r2, r4, r6
USADA8 r0, r3, r5, r2
USADA8VS r0, r4, r0, r1
add r4, r5
CMPGT pc, r7, LSL #2
add r3, r5, r5, lsl #4


.thumb_func
spi_wait_eoqf:
	push {lr}
	ldr r0, =SPI_SR_EOQF
wait_eoqf:
	ldr r1, [r0]
	tst r1, #1
	beq wait_eoqf        // EOQF noch nicht gesetzt

	mov r1, #1
	str r1, [r0]

	pop {pc}

spi_wait_eoqf:
	push {lr}
	ldr r0, =SPI_SR_EOQF
wait_eoqf:
	ldr r1, [r0]
	tst r1, #1
	beq wait_eoqf        // EOQF noch nicht gesetzt

	mov r1, #1
	str r1, [r0]
	pop {pc} 