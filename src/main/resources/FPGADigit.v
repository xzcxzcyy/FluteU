`timescale 1ns / 1ps

module divider (clk, clk_N);
    input           clk;
    output reg      clk_N;
    parameter      N = 500_000;
    reg [31:0]      counter;
        initial begin
            counter <= 32'h0;
            clk_N <= 1'b0;
        end
    always @(posedge clk)  begin
        if(counter==N) begin
            clk_N <= ~clk_N;
            counter <= 32'h0;
        end
        else
            counter <= counter + 1;
    end
endmodule

module counter_3(clk, out);
    input                   clk;
    output reg [2:0]        out;
        initial begin
            out <= 3'b000;
        end
    always @(posedge clk)  begin
        out <= out + 1;
    end
endmodule

module decoder3_8(num, sel);
    input  [2: 0] num;
    output reg [7:0] sel;
    always @(num) begin
        case(num)
            3'd0: sel = 8'b11111110;
            3'd1: sel = 8'b11111101;
            3'd2: sel = 8'b11111011;
            3'd3: sel = 8'b11110111;
            3'd4: sel = 8'b11101111;
            3'd5: sel = 8'b11011111;
            3'd6: sel = 8'b10111111;
            3'd7: sel = 8'b01111111;
            default: sel = 8'b11111111;
        endcase
    end
endmodule

module display_sel(num, dig, code);
    input   [2: 0]    num;
    input   [31:0]    dig;
    output reg [3:0]  code;
    always @* begin
        case(num)
            3'd0: code = dig[3:0];
            3'd1: code = dig[7:4];
            3'd2: code = dig[11:8];
            3'd3: code = dig[15:12];
            3'd4: code = dig[19:16];
            3'd5: code = dig[23:20];
            3'd6: code = dig[27:24];
            3'd7: code = dig[31:28];
            default: code = 4'h0;
        endcase
    end
endmodule

module sevenseg_dec(
    input [3:0]         data,
    output reg [7:0]    segments
);
	 always @(data) begin
        case(data)
            4'h0: segments = 8'b000_0001_1;
            4'h1: segments = 8'b100_1111_1;
            4'h2: segments = 8'b001_0010_1;
            4'h3: segments = 8'b000_0110_1;
            4'h4: segments = 8'b100_1100_1;
            4'h5: segments = 8'b010_0100_1;
            4'h6: segments = 8'b010_0000_1;
            4'h7: segments = 8'b000_1111_1;
            4'h8: segments = 8'b000_0000_1;
            4'h9: segments = 8'b000_1100_1;
            4'ha: segments = 8'b000_1000_1;
            4'hb: segments = 8'b110_0000_1;
            4'hc: segments = 8'b111_0010_1;
            4'hd: segments = 8'b100_0010_1;
            4'he: segments = 8'b011_0000_1;
            4'hf: segments = 8'b011_1000_1;
            default: segments = 8'b111_1111_1;
        endcase
    end
endmodule

// clkx: 100MHz
module FPGADigit( clkx,
                  dig,
                  AN,
                  SEG);

   input  clkx;
   input[31:0]  dig;

   output[7:0] AN;
   output[7:0] SEG;


   wire clk_N;
   wire[3:0] data;
   wire[2:0] num;

   // clk_N: 20KHz

    divider #(5000) u_divider(.clk(clkx), .clk_N(clk_N));
    counter_3 u_counter(.clk(clk_N), .out(num));
    decoder3_8 u_d38(.num(num), .sel(AN));
    display_sel u_ds(.num(num), .dig(dig), .code(data));
    sevenseg_dec u_sd(.data(data), .segments(SEG));

endmodule

