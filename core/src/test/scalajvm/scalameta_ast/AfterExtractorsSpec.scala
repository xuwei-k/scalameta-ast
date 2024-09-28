package scalameta_ast

import com.google.common.reflect.ClassPath
import com.google.common.reflect.ClassPath.ClassInfo
import org.scalatest.freespec.AnyFreeSpec
import scala.jdk.CollectionConverters._

class AfterExtractorsSpec extends AnyFreeSpec {
  def findAllClasses(filter: ClassInfo => Boolean): List[Class[?]] = {
    ClassPath
      .from(scala.meta.Term.Apply.After_4_6_0.getClass.getClassLoader)
      .getAllClasses
      .asList()
      .asScala
      .withFilter(filter)
      .map(_.load())
      .toList
      .distinct
      .sortBy(_.getName)
  }

  "After extractors" in {
    AfterExtractor.values.groupBy(_.tree.structure).foreach { case (k, v) =>
      assert(v.size == 1, k)
    }
    val values = findAllClasses { info =>
      info.getName.contains("After_") && info.getPackageName.contains("scala.meta") && !info.getName.contains(
        "LowPriority"
      )
    }
    val expect = values
      .map(c =>
        c.getEnclosingClass.getCanonicalName.replace("$", "").drop("scala.meta.".length) -> c.getSimpleName
          .replace("$", "")
      )
      .groupBy(_._1)
      .values
      .map(
        _.sortBy(_._1).last
      )
      .toList
      .sorted
    val actual = AfterExtractor.values.map(x => (x.tree.toString, x.extractor))
    assert(actual == expect)
  }
}
