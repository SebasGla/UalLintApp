bad_routine1:
    push {lr}
    mov r0, #1
    pop {pc}


main:
    add r4, r5, r6
    nop

bad_routine2:
    sub r4, r5, r7
    bx lr

.thumb_func
good_routine1:
    nop
    bx lr

.thumb_func
good_routine2:
    push {pc}
    nop
    pop {lr}

bad_routine3:
    sub r4, r5, #5
    bx lr
	
	
.thumb_func
good_routine3:
	push {r4-r11}
	
	mul r1, r4, r5
	
	pop {r4-r11}
	bx lr
	
		
bad_routine4:
	push {r4-r11}
	
	mul r1, r4, r5
	
	pop {r4-r11}
	bx lr
	
	
.thumb_func
Input: 			//Init Unterfunktion fuer Eingabe der Vektoren V1/V2 
	push {r4, r5, lr}			//pushen der benoetigten Register und LinkR (Fuer Ruecksprung) auf Stack 
	add r5, r6,#5
	pop {r4, r5, lr}			//"Rueckholen" der Registerinhalte vom Stack 
	
	bx lr		
	
.thumb_func
matrix4x4_product_trace:
    push {r4-r10}
	
	  smlad r2, r5, r9, r2
    smlad r2, r6, r10, r2

    // Register wiederherstellen und Ergebnis zurueckgeben
    pop {r4-r10}
    mov r0, r2
    bx lr