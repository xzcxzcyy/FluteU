FluteU
=======================

# 这是什么

- 乱序双发射MIPS处理器，已上板验证测试通过
- 指令列表：[config/Instructions.scala](src/main/scala/flute/config/Instructions.scala)
- 地址管理方式：静态线性地址段，没有MMU
- 核心支持 Pipelined Cache & Branch Predictors; 但这两个元件没有实现（其功能与核心解耦）
- AXI和乘除法器部分借鉴了华东师范大学[amadeus-mips](https://github.com/amadeus-mips/amadeus-mips), 早期项目的一些结构也有这个项目的影子

# 如何运行

## 快速上手

注意sbt版本（可在build.sbt中查看）。使用了新特性，因此需要比较新的构建。

Visual Studio Code 可以安装插件 `Scala(Metals)` 后使用；测试时在 `sbt` shell中输入 `testOnly` 并按下 `Tab` 以测试各个测试文件。

```shell
runMain flute.FluteGen  # 生成verilog
```

## 测试

直接使用chiseltest, 详见官方文档https://github.com/ucb-bar/chiseltest

chiseltest从测试独立性的角度出发，前后端设计具有独立性，可以选用不同的后端：

- treadle： 默认，full bindings；启动快，仿真慢；支持VCD波形输出
- verilator：使用`VerilatorBackendAnnotation`选项开启，full bindings；启动满，仿真快；支持VCD, FST波形输出

### 生成波形
添加`WriteVcdAnnotation`选项即可输出VCD波形，输出目录在`/test_run_dir`，可以使用`GTKWave`等工具阅读波形文件


示例：
```scala
package flute.core.issue

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AluIssueQueueTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "AluIssueQueue"

  it should "......" in {
    val detectWidth = 4
    test(new AluCompressIssueQueue(UInt(32.W), 20, detectWidth)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // ...

    }
  }
}

```

# 致谢

- [amadeus-mips](https://github.com/amadeus-mips/amadeus-mips) 在项目结构和Chisel基本使用方面给了我们最初的引导，没有这个项目，本项目的初期将非常艰难；
- [nontrivial-mips](https://github.com/trivialmips/nontrivial-mips) 启发我如何设计 cp0 协处理器；
- [XiangShan](https://github.com/OpenXiangShan/XiangShan) 在项目架构上有很大帮助。
