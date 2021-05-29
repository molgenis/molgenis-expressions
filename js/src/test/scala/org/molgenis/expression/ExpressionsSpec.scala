package org.molgenis.expression

import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec

import scala.scalajs.js
import scala.util.Try

class ExpressionsSpec extends AnyFlatSpec {
  "expression.evaluate" should "compute age" in {
    val threeYearsAgo: js.Date = new js.Date
    threeYearsAgo.setFullYear(threeYearsAgo.getFullYear - 3)
    val context = js.Dictionary("dob" -> threeYearsAgo.toString.asInstanceOf[js.Any])
    assert(Try(Expressions.evaluate("age({dob})", context)).success.value == 3)
  }

  "age" should "be one if your first birthday is today" in {
    val todayAYearAgo: js.Date = new js.Date
    todayAYearAgo.setFullYear(todayAYearAgo.getFullYear - 1)
    assert(Expressions.age(todayAYearAgo) == 1)
  }

  it should "be zero if your birthday is tomorrow" in {
    val tomorrowAYearAgo: js.Date = new js.Date
    tomorrowAYearAgo.setFullYear(tomorrowAYearAgo.getFullYear - 1)
    tomorrowAYearAgo.setTime(tomorrowAYearAgo.getTime + 24 * 3600 * 1000)
    assert(Expressions.age(tomorrowAYearAgo) == 0)
  }

  "ageConvert" should "compute age for yyyy-mm-dd string" in {
    val threeYearsAgo: js.Date = new js.Date
    threeYearsAgo.setFullYear(threeYearsAgo.getFullYear - 3)
    val dob = threeYearsAgo.toISOString.substring(0, 10)
    assert(Expressions.ageConvert(List(dob)) == 3)
  }
  
  "create context" should "filter out undefined" in {
    assert(Expressions.createContext(js.Dictionary("x" -> js.undefined)).isEmpty)
  }
  it should "filter out null" in {
    assert(Expressions.createContext(js.Dictionary("x" -> null)).isEmpty)
  }
}
