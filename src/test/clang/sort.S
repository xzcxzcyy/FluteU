 .text
sort_init:
   
 addi $s0,$s0,1
 
 add $s0,$zero,$zero   
 addi $s1,$zero,60   #排序区间
sort_loop:
 lw $s3,0($s0)     
 lw $s4,0($s1)
 slt $t0,$s3,$s4
 beq $t0,$0,sort_next   #降序排序
 sw $s3, 0($s1)
 sw $s4, 0($s0)   
sort_next:
 addi $s1, $s1, -4   
 bne $s0, $s1, sort_loop  
 
 addi $s0,$s0,4
 addi $s1,$zero,60
 bne $s0, $s1, sort_loop

 ori $t8,$zero,0
 ori $t9,$zero,64
loop:
 lw $a0,($t8)
 addi $t8,$t8,4
 bne $t8, $t9, loop
nop
 #MIPS处理器实现中请用停机指令实现syscall
