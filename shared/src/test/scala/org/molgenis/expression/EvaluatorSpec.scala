package org.molgenis.expression

import org.molgenis.expression.Evaluator.isTruthy
import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.{TableFor1, TableFor2, Tables}

import scala.Double.{NegativeInfinity, PositiveInfinity}
import scala.util.Success

class EvaluatorSpec extends AnyFlatSpec with Tables {

  val context: Map[String, Any] = Map(
    "foo" -> "foo",
    "bar" -> 4.5,
    "ten" -> 10
  )

  val functions: Map[String, List[Any] => Any] = Map(
    "throw" -> ((_: Seq[Any]) => throw new IllegalArgumentException(""))
  )
  val evaluator = new Evaluator.Evaluator(context, functions)

  "variable lookup" should "retrieve from context" in {
    assert(evaluator.evaluate(Variable("foo")).success.value == "foo")
    assert(evaluator.evaluate(Variable("bar")).success.value == 4.5)
  }

  it should "return null if variable is not found" in {
    assert(evaluator.evaluate(Variable("foobar")).success.value == null)
  }

  "integer expressions" should "evaluate addition" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Add, Constant(2), Constant(3)))
        .success
        .value == 5
    )
  }
  it should "evaluate subtraction" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Subtract, Constant(3), Constant(2L)))
        .success
        .value == 1
    )
  }
  it should "evaluate multiplication" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Multiply, Constant(9), Constant(4)))
        .success
        .value == 36
    )
  }
  it should "evaluate integer division as though they were doubles" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Divide, Constant(9), Constant(4)))
        .success
        .value == 2.25
    )
  }
  it should "evaluate integer modulo" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Modulo, Constant(9), Constant(4)))
        .success
        .value == 1
    )
  }

  "floating point expressions" should "evaluate addition" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Add, Constant(2.1), Constant(3.0)))
        .success
        .value == 5.1
    )
  }
  it should "evaluate subtraction" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Subtract, Constant(3f), Constant(2f)))
        .success
        .value == 1f
    )
  }
  it should "evaluate multiplication" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Multiply, Constant(9f), Constant(4f)))
        .success
        .value == 36f
    )
  }
  it should "evaluate integer division" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Divide, Constant(9f), Constant(4f)))
        .success
        .value == 2.25
    )
  }
  it should "rounds floats to evaluate integer modulo" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Modulo, Constant(9.1), Constant(4)))
        .success
        .value == 1
    )
  }
  it should "fail to evaluate for non-numeric arguments" in {
    assert(
      evaluator
        .evaluate(BinaryOperation(Add, Constant("Hello"), Constant("Blah")))
        .failure
        .exception
        .getMessage == "Cannot Add Hello and Blah"
    )
  }

  "nested expressions" should "evaluate correctly" in {
    assert(
      evaluator.evaluate(
        BinaryOperation(
          Multiply,
          BinaryOperation(Power, Constant(3), Constant(2)),
          Constant(5)
        )
      ) == Success(45)
    )
  }

  "complex boolean expressions" should "evaluate correctly" in {
    assert(
      evaluator.evaluate(
        BinaryOperation(
          And,
          BinaryOperation(Less, Variable("bar"), Variable("ten")),
          BinaryOperation(Greater, Variable("bar"), Constant(4))
        )
      ) == Success(true)
    )
  }

  it should "evaluate Or eagerly" in {
    assert(
      evaluator
        .evaluate(
          BinaryOperation(
            Or,
            BinaryOperation(Greater, Constant(2), Constant(1)),
            FunctionEvaluation("throw", List())
          )
        )
        .success
        .value == true
    )
  }

  it should "fail if right argument of boolean operation is needed but fails" in {
    assert(
      evaluator
        .evaluate(
          BinaryOperation(
            Or,
            Constant(false),
            FunctionEvaluation("throw", List())
          )
        )
        .isFailure
    )
  }

  it should "evaluate And eagerly" in {
    assert(
      evaluator
        .evaluate(
          BinaryOperation(
            And,
            BinaryOperation(Less, Constant(2), Constant(1)),
            FunctionEvaluation("throw", List())
          )
        )
        .success
        .value == false
    )
  }

  "get variables" should "return a set of variable names used" in {
    assert(
      Evaluator.getVariables(
        BinaryOperation(
          And,
          BinaryOperation(Less, Variable("bar"), Variable("ten")),
          BinaryOperation(Greater, Variable("bar"), Constant(4))
        )
      ) == Set("bar", "ten")
    )
  }

  val setExpressions: TableFor2[String, Boolean] = Table(
    ("expression", "value"),
    ("['a', 'c'] anyof ['a', 'b']", true),
    ("['a', 'c'] allof ['a', 'b']", false),
    ("['a', 'c', 'd'] allof ['a', 'c']", true),
    ("[] contains 'a'", false),
    ("['a', 'c'] contains 'a'", true),
    ("['a', 'c'] contains 'b'", false),
    ("['a', 'c'] contains ['a']", true),
    ("['a', 'b'] contains ['a', 'c']", false),
    ("['a', 'b', 'c'] contains ['a', 'c']", true),
    ("['a'] notcontains ['a', 'c']", true),
    ("['a', 'b', 'c'] notcontains ['a', 'c']", false),
    ("['1'] contains '1'", true),
    ("['2'] contains '1'", false)
  )

  "set expressions" should "be parsed and evaluated correctly" in {
    forAll(setExpressions)((expression, expected) => {
      val parsedExpression = Parser.parseAll(expression)
      val parsed = parsedExpression.success.value
      val evaluated = evaluator.evaluate(parsed)
      assert(evaluated.success.value === expected)
    })
  }

  val unaryExpressions: TableFor2[String, Boolean] = Table(
    ("expression", "value"),
    ("['a'] empty", false),
    ("['a'] notempty", true),
    ("! null", true),
    ("! 'foo'", false),
    ("! false", true)
  )

  "unary expressions" should "be parsed and evaluated correctly" in {
    forAll(unaryExpressions)((expression, expected) => {
      val parsedExpression = Parser.parseAll(expression)
      val parsed = parsedExpression.success.value
      val evaluated = evaluator.evaluate(parsed)
      assert(evaluated.success.value === expected)
    })
  }

  "null" should "equal null" in {
    val parsedExpression = Parser.parseAll("null = null")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === true)
  }

  it should "be empty" in {
    val parsedExpression = Parser.parseAll("null empty")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === true)
  }

  it should "not be notempty" in {
    val parsedExpression = Parser.parseAll("null notempty")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === false)
  }

  it should "'notequal' defined values" in {
    val parsedExpression = Parser.parseAll("null != 3")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === true)
  }

  it should "equal undefined" in {
    val parsedExpression = Parser.parseAll("null = undefined")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === true)
  }

  it should "not 'equal' defined values" in {
    val parsedExpression = Parser.parseAll("null = 3")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === false)
  }

  it should "'notcontains' things" in {
    val parsedExpression = Parser.parseAll("null notcontains '3'")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === true)
  }

  it should "not 'contains' things" in {
    val parsedExpression = Parser.parseAll("null contains '3'")
    val parsed = parsedExpression.success.value
    val evaluated = evaluator.evaluate(parsed)
    assert(evaluated.success.value === false)
  }

  "isTruthy" should "be false for falsey values" in {
    val falsey: TableFor1[Any] =
      Table("value", false, 0, 0.0, -0, -0.0f, "", Double.NaN, null)
    forAll(falsey)(x => {
      assert(isTruthy(x) === false)
    })
  }

  it should "be true for truthy values" in {
    val truthy: TableFor1[Any] =
      Table("value", true, 1, 2.18, "Hello", PositiveInfinity, NegativeInfinity)
    forAll(truthy)(x => {
      assert(isTruthy(x))
    })
  }
}
