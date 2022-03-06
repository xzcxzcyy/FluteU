addi $t0,$zero,0x00
addi $t1,$zero,0x10 # tick 6
addi $t2,$zero,0x20

addi $s0,$zero,0x1f1f # tick +1
addi $s1,$zero,0x2f2f
addi $s2,$zero,0x3f3f # tick +1 = 8

sw $s1,($t1)
sw $s2,($t2) # tick +4
lw $a1,($t1)
lw $a2,($t2) # tick +3

sw $s0,($t0)
lw $a0,($t0) # tick +2