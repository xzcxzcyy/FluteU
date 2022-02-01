package config

import chisel3._


object CpuConfig extends WidthConfig with AmountConfig {

  object StoreMode {
    val disable  = 0.U(storeModeWidth.W)
    val word     = 1.U(storeModeWidth.W)
    val byte     = 2.U(storeModeWidth.W)
    val halfword = 3.U(storeModeWidth.W)
  }

  object BranchCond {
    val none = 0.U(branchCondWidth.W)
    val eq   = 1.U(branchCondWidth.W)
    val ge   = 2.U(branchCondWidth.W)
    val gez  = 3.U(branchCondWidth.W)
    val geu  = 4.U(branchCondWidth.W)
    val gt   = 5.U(branchCondWidth.W)
    val gtz  = 6.U(branchCondWidth.W)
    val gtu  = 7.U(branchCondWidth.W)
    val le   = 8.U(branchCondWidth.W)
    val leu  = 9.U(branchCondWidth.W)
    val lt   = 10.U(branchCondWidth.W)
    val ltu  = 11.U(branchCondWidth.W)
    val ne   = 12.U(branchCondWidth.W)
  }

  object ALUOp {
    // Empty Op
    val none = 0.U(aluOpWidth.W)
    // Bitwise Ops
    val and = 1.U(aluOpWidth.W)
    val or  = 2.U(aluOpWidth.W)
    val xor = 3.U(aluOpWidth.W)
    val nor = 4.U(aluOpWidth.W)
    // Shift Ops
    /** Logical shift left: rd ← rt << shamt. Fills bits from right with zeros. Logical shift right:
      * rd ← rt >> shamt. Fills bits from left with zeros. Arithmetic shift right: If rt is negative,
      * the leading bits are filled in with ones instead of zeros: rd ← rt >> shamt.
      *
      * Mind the order of shift oprands.
      */
    val sll = 5.U(aluOpWidth.W) // shift left  logically
    val srl = 6.U(aluOpWidth.W) // shift right logically
    val sra = 7.U(aluOpWidth.W) // shift right arithmetically
    // Set Ops
    val slt  = 8.U(aluOpWidth.W) // set less than (signed)
    val sltu = 9.U(aluOpWidth.W) // set less than (unsigned)
    // Sub and Add Ops
    val add  = 10.U(aluOpWidth.W)
    val sub  = 11.U(aluOpWidth.W)
    val addu = 12.U(aluOpWidth.W)
    val subu = 13.U(aluOpWidth.W)
  }

  object JCond {
    val j   = 0.U(jCondWidth.W)
    val jr = 1.U(jCondWidth.W)
    val b  = 2.U(jCondWidth.W)
  }

  object RegDst {
    val rt = 0.U(regDstWidth.W)
    val rd = 1.U(regDstWidth.W)
    val GPR31 = 2.U(regDstWidth.W)
  }

  object RsRtRecipe {
    val normal = 0.U(rsrtRecipeWidth.W)
    val link   = 1.U(rsrtRecipeWidth.W)
    val lui    = 2.U(rsrtRecipeWidth.W)
  }

  object ImmRecipe {
    val sExt = 0.U(immRecipeWidth.W)
    val uExt = 1.U(immRecipeWidth.W)
    val lui  = 2.U(immRecipeWidth.W)
  }
}
