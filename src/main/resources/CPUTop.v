`timescale 1ns / 1ps

module CPUTop(
    input CLK100MHZ,
    input  SW,
    output [7:0] AN,
    output [0:7] SEG,
    output [15:0] LED
);
    wire rst, clk_N;
    divider #(2) f_divider(.clk(CLK100MHZ), .clk_N(clk_N));
    
    (* DONT_TOUCH = "TRUE" *)wire [31:0] a0;
    (* DONT_TOUCH = "TRUE" *)wire [31:0] pc;
    
    assign rst = SW;
    assign LED = pc[15:0];
    
    BUFG bufg(
        .I(clk_N),
        .O(cpuclk)
    );
    
    CoreTester core(
        .clock(cpuclk),
        .reset(rst),
        .io_debug_4(a0),
        .io_pc(pc)
    );
    
    FPGADigit fpgadigit(
        .clkx(CLK100MHZ),
        .AN(AN),
        .SEG(SEG),
        .dig(a0)
    );
    
endmodule

