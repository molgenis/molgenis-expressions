package org.molgenis.expression;

import org.molgenis.expression.Parser.ParseException
import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpec

import java.time.{LocalDate, ZoneOffset}
import java.util
import scala.util.{Success, Try}

class ExpressionsSpec extends AnyFlatSpec {
  val expressions: Expressions = new Expressions(1000)

  "Get Variable Names" should "work with java collections" in {
    assert(expressions.getAllVariableNames(util.List.of("{foo} = {bar}", "{a} + {b}")) ==
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

  "regex" should "evaluate regular expression" in {
    assert(expressions.parseAndEvaluate(util.List.of(
      """regex('^[1-9][0-9]{3}[\\s]?[A-Za-z]{2}$','6226 BC')"""),
      util.Map.of())
      .get(0).success.value === true)
  }

  it should "evaluate regular expression with flags" in {
    assert(expressions.parseAndEvaluate(util.List.of(
      """regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'i')"""),
      util.Map.of())
      .get(0).success.value === true)
  }

  it should "fail when encountering unknown flags" in {
    assert(expressions.parseAndEvaluate(util.List.of(
      """regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'q')"""),
      util.Map.of())
      .get(0).failure.exception.getMessage == "Unknown regex flag: q")
  }

  "tryGetVariableNames" should "throw an exception if it fails to parse the expression" in {
    assert(Try(expressions.getVariableNames("{foo")).failure.exception == ParseException("Expected \"}\":1:5, found \"\"", 4))
  }

  "tryGetVariableNames" should "return variable names" in {
    assert(Try(expressions.getVariableNames("{foo} = {bar}")).success.value ==
      util.Set.of("foo", "bar"))
  }

  "empty" should "Work for java collections" in {
    assert(expressions.parseAndEvaluate(util.List.of("{foo} empty", "{foo} notempty"),
      util.Map.of("foo", util.List.of())) == util.List.of(Success(true), Success(false)))
  }

  it should "Work for java Strings" in {
    assert(expressions.parseAndEvaluate(util.List.of("{foo} empty", "{foo} notempty"),
      util.Map.of("foo", new java.lang.String(""))) == util.List.of(Success(true), Success(false)))
  }
}