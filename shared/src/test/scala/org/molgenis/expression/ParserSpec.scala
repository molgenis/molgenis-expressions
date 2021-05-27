package org.molgenis.expression

import fastparse._
import org.molgenis.expression
import org.molgenis.expression.Parser.ParseError
import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll, whenever}
import org.scalatest.prop.{TableFor2, TableFor3, Tables}

class ParserSpec extends AnyFlatSpec with Tables {
  val numbers: TableFor2[String, Number] = Table(
    ("string", "value"),
    ("3", 3),
    ("-3", -3),
    ("08", 8),
    ("-08", -8),
    ("2.18", 2.18),
    ("-2.18", -2.18),
    ("2.", 2.0),
    (".18", 0.18),
    ("-2.", -2.0),
    ("-.18", -0.18),
  )
  "Value parser" should "parse numbers" in {
    forAll(numbers)((string, expected) => {
      val Parsed.Success(Constant(actual), _) = parse(string, Parser.constValue(_))
      assert(actual === expected)
    })
  }

  val booleans: TableFor2[String, Boolean] = Table(
    ("string", "value"),
    ("true", true),
    ("false", false),
    ("False", false),
  )
  it should "parse booleans" in {
    forAll(booleans)((string, expected) => {
      val Parsed.Success(Constant(actual), _) = parse(string, Parser.constValue(_))
      assert(actual === expected)
    })
  }

  val strings: TableFor2[String, String] = Table(
    ("string", "value"),
    ("\"a\\nb\"", "a\nb"),
    ("\"Hello \\\"World\"", "Hello \"World"),
    ("'Hello World'", "Hello World"),
    ("'Hello \\\\World'", "Hello \\World"),
    ("\"\\ud83d\\ude00\"", "\uD83D\uDE00")
  )
  it should "parse string literals" in {
    forAll(strings)((string, expected) => {
      val result = parse(string, Parser.constValue(_))
      val Parsed.Success(Constant(value), _) = result
      assert(value === expected)
    })
  }

  val variables: TableFor2[String, Boolean] = Table(
    ("string", "valid"),
    ("abcDef", true),
    ("123String", false),
    ("$", true),
    ("_", true),
    ("foo.bar", true),
    ("compound_int", true),
    ("description-nl", true)
  )

  "Expression parser" should "parse variables" in {
    forAll(variables)((string, valid) => {
      val result = parse(s"{${string}}", Parser.expression(_))
      if (valid) {
        val Parsed.Success(value, _) = result
        assert(value === Variable(string))
      } else {
        assert(!result.isSuccess)
      }
    })
  }

  val arrays: TableFor2[String, expression.Array] = Table(
    ("string", "array"),
    ("[]", expression.Array(List())),
    ("[3, 2.0, 0]", expression.Array(List(Constant(3), Constant(2.0), Constant(0)))),
    ("['foo', 'bar']", expression.Array(List(Constant("foo"), Constant("bar"))))
  )
  it should "parse arrays" in {
    forAll(arrays)((string, array) => {
      val Parsed.Success(actual, _) = parse(string, Parser.expression(_))
      assert(actual === array)
    })
  }

  val unaryOperations: TableFor2[String, UnaryOperation] = Table(
    ("string", "operation"),
    ("!true", UnaryOperation(Negate, Constant(true))),
    ("{foo} empty", UnaryOperation(Empty, Variable("foo")))
  )
  it should "parse unary operations" in {
    forAll(unaryOperations)((string, operation) => {
      val Parsed.Success(actual, _) = parse(string, Parser.expression(_))
      assert(actual === operation)
    })
  }

  val functionCalls: TableFor2[String, FunctionEvaluation] = Table(
    ("string", "function evaluation"),
    ("foo({bar})", FunctionEvaluation("foo", List(Variable("bar")))),
    ("foo({bar}, 'baz')", FunctionEvaluation("foo", List(Variable("bar"), Constant("baz"))))
  )
  it should "parse function calls" in {
    forAll(arrays)((string, expected) => {
      val Parsed.Success(actual, _) = parse(string, Parser.expression(_))
      assert(actual === expected)
    })
  }

  "Binary functions" should "parse contains" in {
    assert(Parser.parseAll("{bar} contains ['foo']").success.value ==
      BinaryOperation(Contains, Variable("bar"), expression.Array(List(Constant("foo")))))
  }

  "Power" should "parse" in {
    assert(Parser.parseAll("3 power 2").success.value == BinaryOperation(Power, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 ^ 2").success.value == BinaryOperation(Power, Constant(3), Constant(2)))
  }

  it should "be right-associative" in {
    assert(Parser.parseAll("3^4^5").success.value ==
      BinaryOperation(Power, Constant(3), BinaryOperation(Power, Constant(4), Constant(5))))
  }

  "Multiplication and Division" should "parse" in {
    assert(Parser.parseAll("3 * 2").success.value == BinaryOperation(Multiply, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 / 2").success.value == BinaryOperation(Divide, Constant(3), Constant(2)))
  }

  it should "be left-associative" in {
    assert(Parser.parseAll("3 / 2 * 4").success.value ==
      BinaryOperation(Multiply, BinaryOperation(Divide, Constant(3), Constant(2)), Constant(4)))
  }

  "Addition and subtraction" should "parse" in {
    assert(Parser.parseAll("3 + 2").success.value == BinaryOperation(Add, Constant(3), Constant(2)))
    assert(Parser.parseAll("3 - 2").success.value == BinaryOperation(Subtract, Constant(3), Constant(2)))
  }

  it should "be left-associative" in {
    assert(Parser.parseAll("3 - 2 + 4").success.value ==
      BinaryOperation(Add, BinaryOperation(Subtract, Constant(3), Constant(2)), Constant(4)))
  }

  it should "give power higher precedence than multiplication and division" in {
    assert(Parser.parseAll("3 ^ 2 * 5").success.value ==
      BinaryOperation(
        Multiply,
        BinaryOperation(
          Power,
          Constant(3),
          Constant(2)),
        Constant(5)))

    assert(Parser.parseAll("5 * 3 ^ 2").success.value ==
      BinaryOperation(
        Multiply,
        Constant(5),
        BinaryOperation(
          Power,
          Constant(3),
          Constant(2))))

    assert(Parser.parseAll("3+2=4+1").success.value ==
      BinaryOperation(
        Equal,
        BinaryOperation(
          Add,
          Constant(3),
          Constant(2)),
        BinaryOperation(
          Add,
          Constant(4),
          Constant(1))))
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
    forAll(comparisonOperators)((sign, _, operator) => {
      val Parsed.Success(result, _) = parse(sign, Parser.binFunctions(_))
      assert(result == operator)
    })
  }

  it should "parse comparator names" in {
    forAll(comparisonOperators)((_, name, operator) => {
      val Parsed.Success(result, _) = parse(name, Parser.binFunctions(_))
      assert(result == operator)
    })
  }

  it should "parse comparator names case insensitively" in {
    forAll(comparisonOperators)((_, name, operator) => {
      val Parsed.Success(result, _) = parse(name.toUpperCase, Parser.binFunctions(_))
      assert(result == operator)
    })
  }

  "Comparisons" should "parse simple expressions" in {
    forAll(comparisonOperators)((sign, _, operator) => assert(
      Parser.parseAll(s"3 ${sign} 2").get == BinaryOperation(operator, Constant(3), Constant(2)))
    )
  }

  "Unary functions" should "parse postfix operator after variable" in {
    assert(Parser.parseAll("{foo} notempty").success.value == UnaryOperation(NotEmpty, Variable("foo")))
  }

  it should "parse postfix operator after list" in {
    assert(Parser.parseAll("['foo'] notempty").success.value == expression.UnaryOperation(NotEmpty, expression.Array(List(Constant("foo")))))
  }

  it should "parse unary functions in logical expression" in {
    assert(Parser.parseAll("{row.user_id} notempty and {roles} notempty").success.value ==
      BinaryOperation(And,
        UnaryOperation(NotEmpty, Variable("row.user_id")),
        UnaryOperation(NotEmpty, Variable("roles"))))
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
        expression.BinaryOperation(operator, Variable("foo"), expression.Array(List(Constant(1), Constant(2))))
    ))
  }

  it should "parse array expression by sign" in {
    forAll(binaryFunctions)((sign, _, operator) =>
      whenever(sign.isDefined) {
        assert(
          Parser.parseAll(s"{foo} ${sign.get} [1, 2]").get ==
            expression.BinaryOperation(operator, Variable("foo"), expression.Array(List(Constant(1), Constant(2))))
        )
      })
  }

  it should "give unary operands higher precedence than binary operands" in {
    assert(Parser.parseAll("!{foo} or {bar}").success.value ==
      BinaryOperation(Or, UnaryOperation(Negate, Variable("foo")), Variable("bar")))
  }

  "Parse failures" should "be reported as failures containing a ParseError" in {
    val exception = Parser.parseAll("{foo").failure.exception
    assert(exception.getMessage == "Expected \"}\":1:5, found \"\"")
    assert(exception.asInstanceOf[ParseError].index == 4)
  }
}
