package org.molgenis.expression

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll, whenever}
import org.scalatest.prop.{TableFor3, Tables}

class ParserSpec extends AnyFlatSpec with Tables {
  "Value parser" should "parse integers" in {
    assert(Parser.parseAll(Parser.arithmeticValue, "3").get == 3)
  }
  it should "parse doubles" in {
    assert(Parser.parseAll(Parser.arithmeticValue, "2.18").get == 2.18)
  }

  "Logic value parser" should "parse booleans" in {
    assert(Parser.parseAll(Parser.logicValue, "true").get == true)
    assert(Parser.parseAll(Parser.logicValue, "false").get == false)
  }

  it should "be case insensitive" in {
    assert(Parser.parseAll(Parser.logicValue, "True").get == true)
    assert(Parser.parseAll(Parser.logicValue, "False").get == false)
  }

  it should "parse double quoted strings" in {
    assert(Parser.parseAll(Parser.constValue, """"Hello World"""").get == Constant("Hello World"))
  }

  it should "parse single quoted strings" in {
    assert(Parser.parseAll(Parser.constValue, """'Hello World'""").get == Constant("Hello World"))
  }

  ignore should "parse escaped strings" in {
    assert(Parser.parseAll(Parser.constValue, """"Hello \"World""").get == Constant("Hello \"World"))
  }

  "Array parser" should "parse values" in {
    assert(Parser.parseAll(Parser.array, "[3, 2.0, 0]").get == Array(List(Constant(3), Constant(2.0), Constant(0))))
  }

  "Unary operation" should "parse negation" in {
    assert(Parser.parseAll("! true").get ==
      UnaryOperation(Negate, Constant(true))
    )
  }

  it should "parse empty operation" in {
    assert(Parser.parseAll("{foo} empty").get ==
      UnaryOperation(Empty, Variable("foo"))
    )
  }

  "Function evaluation" should "parse function call with one argument" in {
    assert(Parser.parseAll("foo({bar})").get ==
      FunctionEvaluation("foo", List(Variable("bar")))
    )
  }

  it should "parse function call with two arguments" in {
    assert(Parser.parseAll("foo({bar}, 'baz')").get ==
      FunctionEvaluation("foo", List(Variable("bar"), Constant("baz")))
    )
  }

  "Binary functions" should "parse contains" in {
    assert((Parser.parseAll("{bar} contains ['foo']")).get ==
      BinaryOperation(Contains, Variable("bar"), Array(List(Constant("foo")))))
  }

  "Power" should "parse" in {
    assert(Parser.parseAll("3 power 2").get == BinaryOperation(Power, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 ^ 2").get == BinaryOperation(Power, Constant(3), Constant(2)))
  }

  "Multiplication and Division" should "parse" in {
    assert(Parser.parseAll("3 * 2").get == BinaryOperation(Multiply, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 / 2").get == BinaryOperation(Divide, Constant(3), Constant(2)))
  }

  it should "be left-associative" in {
    assert(Parser.parseAll("3 / 2 * 4").get ==
      BinaryOperation(Multiply, BinaryOperation(Divide, Constant(3), Constant(2)), Constant(4)))
  }

  "Addition and subtraction" should "parse" in {
    assert(Parser.parseAll("3 + 2").get == BinaryOperation(Add, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 - 2").get == BinaryOperation(Subtract, Constant(3), Constant(2)))
  }

  it should "be left-associative" in {
    assert(Parser.parseAll("3 - 2 + 4").get ==
      BinaryOperation(Add, BinaryOperation(Subtract, Constant(3), Constant(2)), Constant(4)))
  }

  it should "give power higher precedence than multiplication and division" in {
    assert(Parser.parseAll("3 ^ 2 * 5").get ==
      BinaryOperation(
        Multiply,
        BinaryOperation(
          Power,
          Constant(3),
          Constant(2)),
        Constant(5)))

    assert(Parser.parseAll("5 * 3 ^ 2").get ==
      BinaryOperation(
        Multiply,
        Constant(5),
        BinaryOperation(
          Power,
          Constant(3),
          Constant(2))))
  }

  val comparisonOperators: TableFor3[String, String, BinaryOperator] = Table(
    ("sign", "name", "operator"),
    ("<", "less", Less),
    ("<=", "lessorequal", LessOrEqual),
    ("=", "equal", Equal),
    (">=", "greaterorequal", GreaterOrEqual),
    (">", "greater", Greater)
  )

  "Comparison Operator parser" should "parse comparator signs" in {
    forAll(comparisonOperators)((sign, _, operator) =>
      assert(Parser.parseAll(Parser.comparisonOperator, sign).get == operator)
    )
  }

  it should "parse comparator names" in {
    forAll(comparisonOperators)((_, name, operator) => assert(
      Parser.parseAll(Parser.comparisonOperator, name).get == operator)
    )
  }

  it should "parse comparator names case insensitively" in {
    forAll(comparisonOperators)((_, name, operator) => assert(
      Parser.parseAll(Parser.comparisonOperator, name.toUpperCase).get == operator)
    )
  }

  "Comparisons" should "parse simple expressions" in {
    forAll(comparisonOperators)((sign, _, operator) => assert(
      Parser.parseAll(s"3 ${sign} 2").get == BinaryOperation(operator, Constant(3), Constant(2)))
    )
  }

  val binaryFunctions: TableFor3[Option[String], String, BinaryOperator] = Table(
    ("sign", "name", "operator"),
    (Some("*="), "contains", Contains),
    (None, "notcontains", NotContains),
    (None, "anyof", AnyOf),
    (None, "allof", AllOf)
  )

  "Binary functions" should "parse array expression" in {
    forAll(binaryFunctions)((_, name, operator) => assert(
      Parser.parseAll(s"{foo} ${name} [1, 2]").get ==
        BinaryOperation(operator, Variable("foo"), Array(List(Constant(1), Constant(2))))
    ))
  }

  it should "parse array expression by sign" in {
    forAll(binaryFunctions)((sign, _, operator) =>
      whenever(sign.isDefined) {
        assert(
          Parser.parseAll(s"{foo} ${sign.get} [1, 2]").get ==
            BinaryOperation(operator, Variable("foo"), Array(List(Constant(1), Constant(2))))
        )
      })
  }
}
