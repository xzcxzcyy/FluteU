void xor_load() {
    __asm__(
        "xori $1,$0,0xffff;"
        "xori $2,$0,0x00ff;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
        "nop;"
    );
}