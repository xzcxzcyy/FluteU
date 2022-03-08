#SB测试 无分支

.text

addi $t1,$zero,0     #init_addr 
addi $s1,$zero, 0x00  #
addi $s2,$zero, 0x01  #

sb $s1,0x4($t1)
add $s1,$s1,$s2          #data +1
addi $t1,$t1,1           # addr +1

sb $s1,0x4($t1)
add $s1,$s1,$s2          #data +1
addi $t1,$t1,1           # addr +1

sb $s1,0x4($t1)
add $s1,$s1,$s2          #data +1
addi $t1,$t1,1           # addr +1

sb $s1,0x4($t1)
add $s1,$s1,$s2          #data +1
addi $t1,$t1,1           # addr +1

addi $t1,$zero,0x4    # addr
lw $s1,($t1)       #读出数据 
add $a0,$0,$s1

# 最终检查$a0 == 0x03020100