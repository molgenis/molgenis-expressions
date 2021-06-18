package org.molgenis.expression

import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec

import scala.scalajs.js
import scala.scalajs.js.Dictionary
import scala.util.Try

class ExpressionsSpec extends AnyFlatSpec {
  "expression.evaluate" should "compute age" in {
    val threeYearsAgo: js.Date = new js.Date
    threeYearsAgo.setFullYear(threeYearsAgo.getFullYear - 3)
    val context =
      js.Dictionary("dob" -> threeYearsAgo.toString.asInstanceOf[js.Any])
    assert(Try(Expressions.evaluate("age({dob})", context)).success.value == 3)
  }

  "age" should "be one if your first birthday is today" in {
    val todayAYearAgo: js.Date = new js.Date
    todayAYearAgo.setFullYear(todayAYearAgo.getFullYear - 1)
    assert(Expressions.age(todayAYearAgo, new js.Date()) == 1)
  }

  it should "be zero if your birthday is tomorrow" in {
    val tomorrowAYearAgo: js.Date = new js.Date
    tomorrowAYearAgo.setFullYear(tomorrowAYearAgo.getFullYear - 1)
    tomorrowAYearAgo.setTime(tomorrowAYearAgo.getTime + 24 * 3600 * 1000)
    assert(Expressions.age(tomorrowAYearAgo, new js.Date()) == 0)
  }

  "ageConvert" should "compute age for yyyy-mm-dd string" in {
    val threeYearsAgo: js.Date = new js.Date
    threeYearsAgo.setFullYear(threeYearsAgo.getFullYear - 3)
    val dob = threeYearsAgo.toISOString.substring(0, 10)
    assert(Expressions.ageConvert(List(dob)) == 3)
  }

  it should "compute age relative to given date" in {
    assert(Expressions.ageConvert(List("2010-08-13", "2021-08-12")) == 10)
    assert(Expressions.ageConvert(List("2010-08-13", "2021-08-13")) == 11)
  }

  "create context" should "filter out undefined" in {
    assert(
      Expressions.createContext(js.Dictionary("x" -> js.undefined)).isEmpty
    )
  }
  it should "filter out null" in {
    assert(Expressions.createContext(js.Dictionary("x" -> null)).isEmpty)
  }
  it should "map js array to scala list" in {
    assert(
      Expressions.createContext(js.Dictionary("array" -> js.Array()))
        === Map("array" -> List())
    )
  }

  "regex" should "evaluate regular expression" in {
    assert(
      Expressions.evaluate(
        """regex('^[1-9][0-9]{3}[\\s]?[A-Za-z]{2}$','6226 BC')""",
        Dictionary()
      ) === true
    )
  }

  it should "evaluate regular expression with flags" in {
    assert(
      Expressions.evaluate(
        """regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'i')""",
        Dictionary()
      ) === true
    )
  }

  it should "fail when encountering unknown flags" in {
    assert(
      Try(
        Expressions.evaluate(
          """regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'q')""",
          Dictionary()
        )
      ).failure.exception.getMessage == "Unknown regex flag: q"
    )
  }

  "currentYear" should "return the current year" in {
    assert(
      Expressions.evaluate("currentYear()", Dictionary()) === new js.Date()
        .getUTCFullYear()
    )
  }

  "evaluate" should "compare dates" in {
    assert(
      Expressions.evaluate(
        "{date} >= '2010-08-13'",
        Dictionary("date" -> new js.Date("2010-08-14"))
      ) == true
    )
  }

  "evaluate" should "compare date with today()" in {
    assert(
      Expressions.evaluate("'2010-08-13' <= today() ", Dictionary()) == true
    )
  }
}
