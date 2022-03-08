__asm__(
  "addi $1,$0,0x1;"
  "addi $2,$0,0x1;"
  "beq $1,$2,0x4;"
  "addu $2,$2,$1;"
  "addu $4,$2,$1;"
  "nop;"
  "nop;"
);