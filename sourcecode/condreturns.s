.thumb_func
ok_single_return:
    mov r0, #1
    bx lr

.thumb_func
ok_pop_pc:
    push {r4, lr}
    mov r0, #0
    pop {r4, pc}


.thumb_func
ok_common_epilogue:
    cmp r0, #0
    beq ret
    mov r0, #1
ret:
    bx lr

.thumb_func
ok_push_pop_bx:
    push {r4-r7}
    add r0, r1, r2
    pop {r4-r7}
    bx lr

.thumb_func
bad_multiple_returns:
    cmp r0, #0
    beq early
    mov r0, #1
    bx lr
early:
    mov r0, #0
    bx lr
	
	
.thumb_func
bad_pop_and_bx:
    cmp r0, #0
    beq early
    mov r0, #1
    bx lr
early:
    pop {pc}


.thumb_func
bad_conditional_bx:
    cmp r0, #0
    bxeq lr
    mov r0, #1
    bx lr
	
.thumb_func
bad_it_return:
    cmp r0, #0
    it eq
    bx lr
    mov r0, #1
    bx lr

.thumb_func
bad_conditional_pop:
    cmp r0, #0
    popeq {pc}
    mov r0, #1
    bx lr

.thumb_func
ok_no_return:
loop:
    b loop
	
.thumb_func
ok_tail_call:
    bl other_function
    bx lr

.thumb_func
outer:
    push {r4-r11}
    mul r1, r4, r5
    pop {r4-r11}
    bx lr

inner:
    push {r4}
    pop {r4}
    bx lr
