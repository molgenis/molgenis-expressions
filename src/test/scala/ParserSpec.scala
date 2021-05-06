package org.molgenis.expression

import fastparse._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll, whenever}
import org.scalatest.prop.{TableFor2, TableFor3, Tables}

import java.text.ParseException

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
    ("\"\\ud83d\\ude00\"", "\uD83D\uDE00")
  )
  it should "parse string literals" in {
    forAll(strings)((string, expected) => {
      val result = parse(string, Parser.constValue(_))
      val Parsed.Success(Constant(value), _) = result
      assert(value === expected)
    })
  }

  val identifiers: TableFor2[String, Boolean] = Table(
    ("string", "valid"),
    ("abcDef", true),
    ("123String", false),
    ("$", true),
    ("_", true)
  )

  "Expression parser" should "parse variables" in {
    forAll(identifiers)((string, valid) => {
      val result = parse(s"{${string}}", Parser.expression(_))
      if (valid) {
        val Parsed.Success(value, _) = result
        assert(value === Variable(string))
      } else {
        assert(!result.isSuccess)
      }
    })
  }

  val arrays: TableFor2[String, Array] = Table(
    ("string", "array"),
    ("[]", Array(List())),
    ("[3, 2.0, 0]", Array(List(Constant(3), Constant(2.0), Constant(0)))),
    ("['foo', 'bar']", Array(List(Constant("foo"), Constant("bar"))))
  )
  it should "parse arrays" in {
    forAll(arrays)((string, array) => {
      val Parsed.Success(actual,_) = parse(string, Parser.expression(_))
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
      val Parsed.Success(actual,_) = parse(string, Parser.expression(_))
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
      val Parsed.Success(actual,_) = parse(string, Parser.expression(_))
      assert(actual === expected)
    })
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
      forAll(comparisonOperators)((sign, _, operator) => {
        val Parsed.Success(result, _) = parse(sign, Parser.comparisonOperator(_))
        assert(result == operator)
      })
    }

    it should "parse comparator names" in {
      forAll(comparisonOperators)((_, name, operator) => {
        val Parsed.Success(result, _) = parse(name, Parser.comparisonOperator(_))
        assert(result == operator)
      })
    }

    it should "parse comparator names case insensitively" in {
      forAll(comparisonOperators)((_, name, operator) => {
        val Parsed.Success(result, _) = parse(name.toUpperCase, Parser.comparisonOperator(_))
        assert(result == operator)
      })
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

    "Parse failures" should "be reported as failures containing a java.text.ParseException" in {
      val exception = Parser.parseAll("{foo").failed.get
      assert(exception.getMessage == "Expected \"}\":1:5, found \"\"")
      assert(exception.asInstanceOf[ParseException].getErrorOffset == 4)
    }
}
