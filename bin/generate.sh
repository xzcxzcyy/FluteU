# set -x
# Prepare softwares

export VIVADO=/home/xuzichen/software/Vivado2021/Vivado/2021.1/bin/vivado
# export TEMPLATE=/home/xuzichen/project/standard/template_project
export SCRIPT=/home/xuzichen/tclscripts/generate_bitstream_auto.tcl
export SBT_BIN=/home/xuzichen/software/sbt/bin/sbt

# Prepare files

export PROJ_DIR=$PWD/$1-viv
export SRCS_DIR=$PWD/$1-srcs
export CONS_DIR=$PWD/$1-cons

mkdir tmp
mkdir -p $PROJ_DIR
mkdir -p $SRCS_DIR
mkdir -p $CONS_DIR
git clone git@git.haslab.org:donghu/FluteU.git
mv FluteU $1-git

# Generate Verilog Codes

cd $1-git

git config user.name "BUILD BOT"
git config user.email "bot@flute.org"
git fetch origin $1:$1
git checkout $1
mkdir -p target/verilog
$SBT_BIN 'runMain flute.core.CoreTesterGen'
cd ..

# Copy files

# copy .v .mem .xdr files respectively
cp $1-git/src/main/resources/*.v $SRCS_DIR
cp $1-git/target/verilog/*.v $SRCS_DIR
cp $1-git/src/main/resources/*.mem $SRCS_DIR
cp $1-git/src/main/resources/*.xdc $CONS_DIR

# Generate bitstream

cd tmp
$VIVADO -mode batch -source $SCRIPT -tclargs $PROJ_DIR $SRCS_DIR $CONS_DIR
cd ..

# Upload Results

mkdir -p $1-git/bitstream
cp $1-viv/FluteU1.runs/impl_1/CPUTop.bit $1-git/bitstream/$1.bit
cd $1-git
git add -f bitstream/$1.bit
git commit -m "generated bitstream"
git push origin $1
cd ..
# rm -rf $1-git
# rm -r $1-viv
# rm -r $1-cons
# rm -r $1-srcs
# rm -r tmp
