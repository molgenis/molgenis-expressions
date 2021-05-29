package org.molgenis.expression

import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec

import java.time.{LocalDate, ZoneOffset}
import scala.scalajs.js
import scala.util.Try

class ExpressionsSpec extends AnyFlatSpec {
  private val threeYearsAgo: LocalDate = LocalDate.now(ZoneOffset.UTC).minusYears(3)

  "expression.evaluate" should "compute age" in {
    val context = js.Dictionary("dob" -> threeYearsAgo.toString.asInstanceOf[js.Any])
    assert(Try(Expressions.evaluate("age({dob})", context)).success.value == 3)
  }
  
  "create context" should "filter out undefined" in {
    assert(Expressions.createContext(js.Dictionary("x" -> js.undefined)).isEmpty)
  }
  it should "filter out null" in {
    assert(Expressions.createContext(js.Dictionary("x" -> null)).isEmpty)
  }

  "ageConvert" should "Convert string" in {
    assert(Expressions.ageConvert(List(threeYearsAgo.toString)) == 3)
  }
  it should "Convert js Date" in {
    val time = new js.Date(threeYearsAgo.atStartOfDay(ZoneOffset.UTC).toEpochSecond * 1000.0)
    assert(Expressions.ageConvert(List(time)) == 3)
  }
}
