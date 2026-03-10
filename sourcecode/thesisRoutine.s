.thumb_func
valid_routine:
    push {r4, lr}
    add r0, r4, r1
    pop {r4, pc}

invalid_routine1:
	push {r4, lr}
    sub r0, r4, r1
    pop {r4, pc}
	
invalid_routine2:
    sub r0, r4, r1
    bx lr
	
