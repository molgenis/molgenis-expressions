package org.molgenis.expression;

import org.scalatest.flatspec.AnyFlatSpec

import java.time.{LocalDate, ZoneOffset}
import java.util
import scala.util.Success

class ExpressionsSpec extends AnyFlatSpec {
  val expressions: Expressions = new Expressions(1000)

  "Get Variable Names" should "work with java collections" in {
    assert(expressions.getVariableNames(util.List.of("{foo} = {bar}", "{a} + {b}")) ==
      util.Set.of("foo", "bar", "a", "b"))
  }

  "Parse and evaluate" should "work with java collections" in {
    val javaExpressions = util.List.of("{foo} = {bar}")
    val javaContext = util.Map.of[String, Any]("foo", 2, "bar", 2f)
    assert(expressions.parseAndEvaluate(javaExpressions, javaContext) == util.List.of(Success(true)))
  }

  "function 'regex'" should "test string" in {
    val javaExpressions = util.List.of("""regex('^\\w+(,(ASC|DESC))?(;\\w+(,(ASC|DESC))?)*$','foo,ASC;bar,DESC')""")
    val javaContext = util.Map.of[String, Any]()
    assert(expressions.parseAndEvaluate(javaExpressions, javaContext) == util.List.of(Success(true)))
  }

  "age" should "be one if your first birthday is today" in {
    val todayAYearAgo: LocalDate = LocalDate.now(ZoneOffset.UTC).minusYears(1)
    assert(expressions.age(todayAYearAgo) == 1)
  }

  it should "be zero if your birthday is tomorrow" in {
    val tomorrowAYearAgo: LocalDate = LocalDate.now(ZoneOffset.UTC).minusYears(1).plusDays(1)
    assert(expressions.age(tomorrowAYearAgo) == 0)
  }
}