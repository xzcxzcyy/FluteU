BCC=gcc
SCC=mips-linux-gnu-gcc
RCC=mips-linux-gnu-gcc
SRC=src/test/clang
DIR=target/clang

# generate asm code for mips
${DIR}/%.asm: ${SRC}/%.c
	${SCC} -O1 -S -o $@ $^

# generate executable binary code
${DIR}/%.exe: ${SRC}/%.c
	${BCC} -o $@ $^

# generate raw binary code for mips
${DIR}/%.bin: ${SRC}/%.c
	${RCC} -O1 -o $@.bin -c $^
	mips-linux-gnu-objcopy -O binary -j .text $@.bin $@

# generate hex file for mips
${DIR}/%.hex: ${DIR}/%.bin
	hexdump -ve '4/1 "%02x"' -e '"\n"' $^ > $@

# debug
${DIR}/%.debug: ${DIR}/%.bin
	mips-linux-gnu-objdump -d $^.bin
	hexdump -C $^

all: ${DIR} ${DIR}/xor.hex

${DIR}:
	mkdir -p ${DIR}

clean:
	rm -rf ${DIR}