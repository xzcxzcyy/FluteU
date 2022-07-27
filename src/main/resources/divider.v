`timescale 1ns/100ps
//module of 32 bit divider
module bit32_divider (dividend,divider,s,quotient,remainder,error);

input [31:0] dividend;
input [31:0] divider;
input s;//switch between unsigned(s == 0) and signed(s == 1) division mode
output [31:0] quotient;
output [31:0] remainder;
output error;//error sign, report error when divider == 0

reg [31:0] quotient;
reg [31:0] remainder;
reg error;
reg [31:0] tdividend;//temporarily save dividend
reg [31:0] tdivider;//temporarily save divider
reg [4:0] L1; //MSB of dividend
reg [4:0] L2; //MSB of divider
reg [4:0] d;  //difference, d = L1 - L2
wire s;
reg sign;
reg i;//an indicator

always @(dividend or divider)
begin
/*save the two operands into temporary registers and manipulate the temps afterwads*/
  tdividend <= dividend;
  tdivider <= divider;
end

always @(tdividend or tdivider)
begin
  error = 0;
  //specical operands
  if (tdivider == 0)
      error = 1;//report error when divider == 0
  
  else if (tdividend == 0)
    begin
      quotient = 0;
      remainder = 0;
    end
  
  //general operands
  else
  begin
      //in signed mode,save the signs and convert the operands to unsigned mode
      if (s == 1)
      begin
      sign = tdividend[31] - tdivider[31];
      if (tdividend[31] == 1)
        begin
        tdividend = tdividend - 1;
        tdividend = ~tdividend;
        end
      if (tdivider[31] == 1)
        begin
        tdivider = tdivider - 1;
        tdivider = ~tdivider;
        end
      end

      //find MSB of dividend
      L1 = 5'd31;
      for (i = 1; L1 >= 0 && i > 0; L1 = L1 - 1)
      begin
      if (tdividend[L1] == 1)
        begin
        i = 0;
	L1 = L1 + 1;
	end
      end
      //find MSB of divider
      L2 = 5'd31;
      for (i = 1; L2 >= 0 && i > 0; L2 = L2 - 1)
      begin
      if (tdivider[L2] == 1)
	begin
	i = 0;	
	L2 = L2 + 1;
	end
      end
	      
      d = L1 - L2;
      remainder = tdividend;
      tdivider = tdivider<<d;
      quotient = 0;
  
      //calculate quotient and remainder in the loop
      for (d = d; d != 5'd31; d = d - 1)
      begin
       if (remainder >= tdivider)
	 begin
	   quotient = {quotient[30:0],1'b0} + 1;
	   remainder = remainder - tdivider;
	   tdivider = tdivider>>1;
	 end
	else
	 begin
	   quotient = {quotient[30:0],1'b0} + 0;
	   remainder = remainder;
	   tdivider = tdivider>>1;
	 end
      end   
    end
    //convert quotient and remainder back to signed mode
    if (s == 1)
    begin
      if (sign == 1)
        begin
        quotient = ~quotient;
        quotient = quotient + 1;
        quotient = {sign,quotient[30:0]};
        end
      remainder = ~remainder;
      remainder = remainder + 1;
    end
end

endmodule