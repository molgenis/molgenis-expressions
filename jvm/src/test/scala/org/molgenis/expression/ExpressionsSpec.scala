package org.molgenis.expression

import org.molgenis.expression.Parser.ParseException
import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.{TableFor2, Tables}

import java.time.{Instant, LocalDate, ZoneOffset}
import java.util
import scala.util.{Success, Try}

class ExpressionsSpec extends AnyFlatSpec with Tables {
  val expressions: Expressions = new Expressions(1000)

  "Get Variable Names" should "work with java collections" in {
    assert(
      expressions
        .getAllVariableNames(util.List.of("{foo} = {bar}", "{a} + {b}")) ==
        util.Set.of("foo", "bar", "a", "b")
    )
  }

  "Parse and evaluate" should "work with java collections" in {
    val javaExpressions = util.List.of("{foo} = {bar}")
    val javaContext = util.Map.of[String, Any]("foo", 2, "bar", 2f)
    assert(
      expressions.parseAndEvaluate(javaExpressions, javaContext) == util.List
        .of(Success(true))
    )
  }

  "Parse and evaluate single expression" should "return single result" in {
    val javaContext = util.Map.of[String, Any]("foo", 2, "bar", 2f)
    assert(
      expressions.parseAndEvaluate("{foo} = {bar}", javaContext) == true
    )
  }

  it should "throw an error if evaluation fails" in {
    val javaContext = util.Map.of[String, Any]("foo", 2, "bar", 2f)
    assertThrows[ParseException](
      expressions.parseAndEvaluate("unparseable", javaContext)
    )
  }

  "function 'regex'" should "test string" in {
    val javaExpressions = util.List.of(
      """regex('^\\w+(,(ASC|DESC))?(;\\w+(,(ASC|DESC))?)*$','foo,ASC;bar,DESC')"""
    )
    val javaContext = util.Map.of[String, Any]()
    assert(
      expressions.parseAndEvaluate(javaExpressions, javaContext) == util.List
        .of(Success(true))
    )
  }

  "age" should "be one if your first birthday is today" in {
    val todayAYearAgo: LocalDate = LocalDate.now(ZoneOffset.UTC).minusYears(1)

    assert(
      expressions
        .parseAndEvaluate(
          util.List.of("age({dob})"),
          util.Map.of("dob", todayAYearAgo)
        )
        .get(0)
        .success
        .value == 1
    )
  }

  it should "be zero if your birthday is tomorrow" in {
    val tomorrowAYearAgo: LocalDate =
      LocalDate.now(ZoneOffset.UTC).minusYears(1).plusDays(1)
    assert(
      expressions
        .parseAndEvaluate(
          util.List.of("age({dob})"),
          util.Map.of("dob", tomorrowAYearAgo)
        )
        .get(0)
        .success
        .value == 0
    )
  }

  "currentYear" should "return the current year" in {
    assert(
      expressions
        .parseAndEvaluate(util.List.of("currentYear()"), util.Map.of())
        .get(0)
        .success
        .value == LocalDate.now(ZoneOffset.UTC).getYear
    )
  }

  "regex" should "evaluate regular expression" in {
    assert(
      expressions
        .parseAndEvaluate(
          util.List
            .of("""regex('^[1-9][0-9]{3}[\\s]?[A-Za-z]{2}$','6226 BC')"""),
          util.Map.of()
        )
        .get(0)
        .success
        .value === true
    )
  }

  it should "evaluate regular expression with flags" in {
    assert(
      expressions
        .parseAndEvaluate(
          util.List
            .of("""regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'i')"""),
          util.Map.of()
        )
        .get(0)
        .success
        .value === true
    )
  }

  it should "fail when encountering unknown flags" in {
    assert(
      expressions
        .parseAndEvaluate(
          util.List
            .of("""regex('^[1-9][0-9]{3}[\\s]?[a-z]{2}$','6226 BC', 'q')"""),
          util.Map.of()
        )
        .get(0)
        .failure
        .exception
        .getMessage == "Unknown regex flag: q"
    )
  }

  "tryGetVariableNames" should "throw an exception if it fails to parse the expression" in {
    assert(
      Try(
        expressions.getVariableNames("{foo")
      ).failure.exception == ParseException("Expected \"}\":1:5, found \"\"", 4)
    )
  }

  "tryGetVariableNames" should "return variable names" in {
    assert(
      Try(expressions.getVariableNames("{foo} = {bar}")).success.value ==
        util.Set.of("foo", "bar")
    )
  }

  "empty" should "Work for java collections" in {
    assert(
      expressions.parseAndEvaluate(
        util.List.of("{foo} empty", "{foo} notempty"),
        util.Map.of("foo", util.List.of())
      ) == util.List.of(Success(true), Success(false))
    )
  }

  it should "Work for java Strings" in {
    assert(
      expressions.parseAndEvaluate(
        util.List.of("{foo} empty", "{foo} notempty"),
        util.Map.of("foo", new java.lang.String(""))
      ) == util.List.of(Success(true), Success(false))
    )
  }

  "evaluate" should "compare dates" in {
    assert(
      expressions.parseAndEvaluate(
        util.List.of("{date} >= '2010-08-13'", "{date} < '2010-08-15'"),
        util.Map.of("date", LocalDate.parse("2010-08-14"))
      ) == util.List.of(Success(true), Success(true))
    )
  }

  val valueMappings: TableFor2[Any, Any] = Table(
    ("java", "scala"),
    (java.lang.Byte.MIN_VALUE, Byte.MinValue),
    (java.lang.Short.MIN_VALUE, Short.MinValue),
    (java.lang.Character.MIN_VALUE, Char.MinValue),
    (java.lang.Integer.MIN_VALUE, Int.MinValue),
    (java.lang.Long.MIN_VALUE, Long.MinValue),
    (java.lang.Float.MIN_VALUE, Float.MinPositiveValue),
    (java.lang.Double.MIN_VALUE, Double.MinPositiveValue),
    (java.lang.Boolean.TRUE, true),
    (util.List.of("hello"), List("hello")),
    (util.Map.of("foo", "bar"), Map("foo" -> "bar")),
    (LocalDate.parse("2020-08-23"), "2020-08-23"),
    (Instant.parse("2020-08-23T02:18:22Z"), "2020-08-23T02:18:22Z")
  )

  "context conversion" should "map java to scala" in {
    forAll(valueMappings)((java, scala) => {
      assert(
        expressions.parseAndEvaluate(
          util.List.of("{foo}"),
          util.Map.of("foo", java)
        ) == util.List.of(Success(scala))
      )
    })
  }

}
