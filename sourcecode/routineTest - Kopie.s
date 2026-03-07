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
	
	
