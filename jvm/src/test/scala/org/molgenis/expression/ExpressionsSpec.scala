package org.molgenis.expression;

import org.scalatest.flatspec.AnyFlatSpec

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
}