package org.molgenis.expression

import org.molgenis.expression
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec

import java.time.{LocalDate, ZoneOffset}
import scala.scalajs.js
import scala.util.Try

class packageTest extends AnyFlatSpec {
  private val threeYearsAgo: LocalDate = LocalDate.now(ZoneOffset.UTC).minusYears(3)

  "expression.evaluate" should "compute age" in {
    val context = js.Dictionary("dob" -> threeYearsAgo.toString.asInstanceOf[js.Any])
    assert(Try(expression.evaluate("age({dob})", context)).success.value == 3)
  }
  "ageConvert" should "Convert string" in {
    assert(expression.ageConvert(List(threeYearsAgo.toString)) == 3)
  }
  "ageConvert" should "Convert js Date" in {
    val time = new js.Date(threeYearsAgo.atStartOfDay(ZoneOffset.UTC).toEpochSecond * 1000.0)
    assert(expression.ageConvert(List(time)) == 3)
  }
}
