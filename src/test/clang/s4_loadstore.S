addi $t0,$zero,0x10
addi $s0,$zero,0x1f # +6

sb $s0,($t0) # +4
lw $a0,($t0) # +1 = 11

addi $t0,$t0,0x01
sb $s0,($t0)

addi $t0,$zero,0x10
lw $a1,($t0)        # +8