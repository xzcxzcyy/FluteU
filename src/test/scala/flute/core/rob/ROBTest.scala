package flute.core.rob

import flute.util.BaseTestHelper
import firrtl.AnnotationSeq
import treadle.TreadleTester

import chisel3._
import chisel3.util._
import chisel3.stage._
import firrtl.options.TargetDirAnnotation

private class TestHelper(fileName: String)
    extends BaseTestHelper(fileName, () => new ROB(64, 2, 2, 2)) {}
