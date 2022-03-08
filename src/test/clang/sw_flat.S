#SB测试 无分支
#check $a0 == 0x12

.text
addi $t1,$zero,0     #init_addr 
addi $s1,$zero, 0x1234  #

sw $s1,($t1)
lw $a0,($t1)