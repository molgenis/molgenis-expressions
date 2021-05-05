package org.molgenis.expression

import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Success

class EvaluatorSpec extends AnyFlatSpec {

  val context: Map[String, Any] = Map("foo" -> "foo", "bar" -> 4.5, "ten" -> 10)
  val evaluator = new Evaluator.Evaluator(context)

  "variable lookup" should "retrieve from context" in {
    assert(evaluator.evaluate(Variable("foo")).get == "foo")
    assert(evaluator.evaluate(Variable("bar")).get == 4.5)
  }

  it should "fail if not variable is not found" in {
    assert(evaluator.evaluate(Variable("foobar")).isFailure)
  }

  "integer expressions" should "evaluate addition" in {
    assert(evaluator.evaluate(BinaryOperation(Add, Constant(2), Constant(3))) == Success(5))
  }
  it should "evaluate subtraction" in {
    assert(evaluator.evaluate(BinaryOperation(Subtract, Constant(3), Constant(2L))) == Success(1))
  }
  it should "evaluate multiplication" in {
    assert(evaluator.evaluate(BinaryOperation(Multiply, Constant(9), Constant(4))) == Success(36))
  }
  it should "evaluate integer division" in {
    assert(evaluator.evaluate(BinaryOperation(Divide, Constant(9L), Constant(4))) == Success(2))
  }
  it should "evaluate integer modulo" in {
    assert(evaluator.evaluate(BinaryOperation(Modulo, Constant(9), Constant(4))) == Success(1))
  }

  "floating point expressions" should "evaluate addition" in {
    assert(evaluator.evaluate(BinaryOperation(Add, Constant(2.1), Constant(3.0))) == Success(5.1))
  }
  it should "evaluate subtraction" in {
    assert(evaluator.evaluate(BinaryOperation(Subtract, Constant(3f), Constant(2f))) == Success(1f))
  }
  it should "evaluate multiplication" in {
    assert(evaluator.evaluate(BinaryOperation(Multiply, Constant(9f), Constant(4f))) == Success(36f))
  }
  it should "evaluate integer division" in {
    assert(evaluator.evaluate(BinaryOperation(Divide, Constant(9f), Constant(4f))) == Success(2.25))
  }
  it should "fail to evaluate integer modulo" in {
    assert(evaluator.evaluate(BinaryOperation(Modulo, Constant(9f), Constant(4))).isFailure)
  }
  it should "fail to evaluate for non-numeric arguments" in {
    assert(evaluator.evaluate(BinaryOperation(Add, Constant("Hello"), Constant(2))).isFailure)
  }

  "nested expressions" should "evaluate correctly" in {
    assert(evaluator.evaluate(BinaryOperation(
      Multiply,
      BinaryOperation(
        Power,
        Constant(3),
        Constant(2)),
      Constant(5))) == Success(45))
  }

  "complex boolean expressions" should "evaluate correctly" in {
    assert(evaluator.evaluate(BinaryOperation(
      And,
      BinaryOperation(
        Less,
        Variable("bar"),
        Variable("ten")),
      BinaryOperation(
        Greater,
        Variable("bar"),
        Constant(4)))) == Success(true))
  }

  "get variables" should "return a set of variable names used" in {
    assert(Evaluator.getVariables(BinaryOperation(
      And,
      BinaryOperation(
        Less,
        Variable("bar"),
        Variable("ten")),
      BinaryOperation(
        Greater,
        Variable("bar"),
        Constant(4)))) == Set("bar", "ten"))
  }
}
