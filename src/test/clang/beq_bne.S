# includes 16 instructions : 5 R type ,11 I type.
# benchmark for beq and bne inst instructions
.text
addi $s0,$zero,1
addi $s2,$zero,3
addi $s1,$zero,2
addi $s3,$zero,4
beq $s0, $s2, Next1
beq $s0, $s0, Next1
addi $s1,$zero,255    # can not execute
addi $s2,$zero,255
addi $s3,$zero,255

Next1:

 bne $s1, $s1, Next2
 bne $s1, $s2, Next2

 addi $s1,$zero,254    # can not execute
 addi $s2,$zero,254
 addi $s3,$zero,254

Next2:
 nop
