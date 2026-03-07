.thumb_func
bad_push_args:
    push {r0, lr}
    mov r0, #1
    pop {r0, pc}

.thumb_func
bad_missing_save:
    push {lr}
    add r4, r4, #1
    pop {pc}

.thumb_func
ok_save:
    push {r4, lr}
    add r4, r4, #1
    pop {r4, pc}


.thumb_func
logic_bad_1:
	push {r4, r5, lr}
    and r4, r1, r5     // r1 read before write → inferred param r1
    pop {r4,r5, pc}

.thumb_func
logic_ok_1:
    orr r0, r0, r1     // param r0
    eor r0, r1, r2     // param r1
    bx lr

.thumb_func
mul_bad_1:
    mul r4, r2, r5    // r2 read → inferred param r2
    bx lr


.thumb_func
mul_ok_1:
	push {r4, r5, lr}
    mul r4, r0, r5
	mul r4, r2, r5
    mul r4, r1, r5  
	pop {r4, r5, pc}

.thumb_func
div_bad_1:
    div r4, r3, r5    // r3 read → inferred param r3
    bx lr

.thumb_func
div_ok_1:
    div r4, r0, r5
    div r4, r1, r6
    div r4, r2, r7
    div r4, r3, r8
    bx lr
