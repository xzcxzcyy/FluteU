addi $t0,$zero,0x00
addi $t1,$zero,0x10 # tick 6
addi $t2,$zero,0x20

addi $s0,$zero,0x1f1f # tick +1
addi $s1,$zero,0x2f2f
addi $s2,$zero,0x3f3f # tick +1 = 8

sb $s1,($t1) # tick +4
sb $s2,($t2) # tick +1
lw $a1,($t1) # tick +1
lw $a2,($t2) # tick +1

sb $s0,($t0) # tick +1
lw $a0,($t0) # tick +1