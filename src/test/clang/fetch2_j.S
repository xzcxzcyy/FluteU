.text

addi $s0,$zero,1
addi $s1,$zero,2
addi $s2,$zero,3
addi $s3,$zero,4
j Next1
addi $s0,$zero,5
addi $s1,$zero,6
Next1:
jal Next2
addi $s3,$zero,8
Next2:
nop
