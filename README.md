FluteU
=======================

注意sbt版本（可在build.sbt中查看）。使用了新特性，因此需要比较新的构建。

Visual Studio Code 可以安装插件 `Scala(Metals)` 后使用；测试时在 `sbt` shell中输入 `testOnly` 并按下 `Tab` 以测试各个测试文件。


## 测试

直接使用chiseltest, 详见官方文档https://github.com/ucb-bar/chiseltest

chiseltest从测试独立性的角度出发，前后端设计独立性，可以选用不同的后端：

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
