#include <iostream>

#include "VCPUTop.h"
#include "verilated.h"

int main(int argc, char** argv, char** env) {
    Verilated::commandArgs(argc, argv);

    VCPUTop * top = new VCPUTop;

    while (!Verilated::gotFinish()) {
        top->eval();
    }

    top->final();

    delete top;

    return 0;
}