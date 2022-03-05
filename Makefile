PREFIX != if [ -f "/etc/arch-release" ]; then \
		echo mips-elf; \
	else \
	  echo mips-linux-gnu; \
  fi

TARGET=xor.hexS xor.hex xori.hexS xor_load.hex \
		bgez.hexS bgtz.hexS blez.hexS bltz.hexS \
		bht.hexS beq_bne.hexS jmp.hexS branch.hexS \
		sllv.hexS shift.hexS sltiu.hexS \
		sort.hexS pipe.hexS datarace.hexS \
		lb.hexS lbu.hexS lh.hexS lhu.hexS lui.hexS \
		sb.hexS sh.hexS srav.hexS srlv.hexS subu.hexS \
		multu.hexS mul.hexS divu.hexS benchmark.hexS \
		sb_flat.hexS sw_flat.hexS \
		s1_base.hexS s2_swap.hexS

SRC=src/test/clang
DIR=target/clang
BCC=gcc
SCC=${PREFIX}-gcc
RCC=${PREFIX}-gcc
MCP=${PREFIX}-objcopy

# generate asm code for mips
${DIR}/%.asm: ${SRC}/%.c
	${SCC} -O1 -S -o $@ $^

# generate executable binary code
${DIR}/%.exe: ${SRC}/%.c
	${BCC} -o $@ $^

# generate raw binary code for mips
${DIR}/%.bin: ${SRC}/%.c
	${RCC} -O1 -o ${DIR}/$*.o -c $^
	${MCP} -O binary -j .text ${DIR}/$*.o $@

${DIR}/%.binS: ${SRC}/%.S
	${RCC} -O1 -o ${DIR}/$*.oS -c $^
	${MCP} -O binary -j .text ${DIR}/$*.oS $@

${DIR}/%.hexS: ${DIR}/%.binS
	hexdump -ve '4/1 "%02x"' -e '"\n"' $^ > $@

# generate hex file for mips
${DIR}/%.hex: ${DIR}/%.bin
	hexdump -ve '4/1 "%02x"' -e '"\n"' $^ > $@

# debug
%.debug: ${DIR}/%.bin
	mips-linux-gnu-objdump -d ${DIR}/$*.o
	hexdump -C $^

%.debugS: ${DIR}/%.binS
	mips-linux-gnu-objdump -d ${DIR}/$*.oS
	hexdump -C $^

all: ${DIR} $(addprefix target/clang/,$(TARGET))

${DIR}:
	mkdir -p ${DIR}

clean:
	rm -rf ${DIR}