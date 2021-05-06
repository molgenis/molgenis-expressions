package org.molgenis.expression

import fastparse.CharPredicates.isPrintableChar
import fastparse.SingleLineWhitespace._
import fastparse._

import java.text.ParseException
import scala.util.Try

object Parser {
  def nullValue[_: P]: P[Null.type] = P(StringIn("null", "undefined").map(_ => Null))

  def digit[_: P]: P[String] = P(CharIn("0-9").!)

  def wholeNumber[_: P]: P[Number] = P(("-".? ~ digit.rep(1)).!
    .opaque("<whole number>").map {
    _.toLong
  })

  def decimalNumber[_: P]: P[Number] =
    ("-".? ~ ((digit.rep(1) ~ "." ~ digit.rep()) | (digit.rep() ~ "." ~ digit.rep(1)))).!
      .opaque("<decimal number>").map {
      _.toDouble
    }

  def arithmeticValue[_: P]: P[Number] = P(decimalNumber | wholeNumber)

  def logicValue[_: P]: P[Boolean] = IgnoreCase("true").map(_ => true) | IgnoreCase("false").map(_ => false)

  private def escapedChar[_: P]: P[String] = P("\\" ~~ CharIn("""'\"bfnrt""").!).map {
    case "b" => "\b"
    case "f" => "\f"
    case "n" => "\n"
    case "r" => "\r"
    case "t" => "\t"
    case x => x
  }

  private def hexDigit[_: P] = P(CharIn("0-9", "a-f", "A-F"))

  private def unicodeEscape[_: P]: P[String] = P("\\u" ~~/ hexDigit.repX(4, max = 4).!)
    .opaque("<unicode escape sequence>").map {
    Integer.parseInt(_, 16).toChar.toString
  }

  private def singleQuoteStringLiteral[_: P]: P[String] = P("'" ~~/ (
    escapedChar | unicodeEscape | CharPred(c => c != '\'' && c != '\\' && isPrintableChar(c)).!
    ).repX() ~~ "'").map(_.mkString)

  private def doubleQuoteStringLiteral[_: P] = P("\"" ~~/ (
    escapedChar | unicodeEscape | CharPred(c => c != '"' && c != '\\' && isPrintableChar(c)).!
    ).repX() ~~ "\"").map(_.mkString)

  def stringValue[_: P]: P[String] = singleQuoteStringLiteral | doubleQuoteStringLiteral

  def constValue[_: P]: P[Constant] = (arithmeticValue | logicValue | stringValue) map Constant

  def identifier[_: P]: P[String] = P((CharIn("A-Z", "a-z", "$", "_") ~ CharIn("A-Z", "a-z", "0-9", "$", "_").rep()).!)
    .opaque("<identifier>")

  def variable[_: P]: P[Variable] = P("{" ~/ identifier.! ~ "}") map Variable

  def atom[_: P]: P[Expression] = P(nullValue | variable | constValue)

  private def unFunction[_: P]: P[UnaryOperator] = P(
    IgnoreCase("empty").map(_ => Empty) |
      IgnoreCase("notempty").map(_ => NotEmpty))

  def unaryOperation[_: P]: P[Expression] =
    P((("!" | IgnoreCase("negate")) ~/ expression.map {UnaryOperation(Negate, _)} )|
      (atom ~ unFunction.?).map {
        case (atom, None) => atom
        case (operand, Some(op)) => UnaryOperation(op, operand)
      })
  def functionOperation[_:P]: P[FunctionEvaluation] =
    P((identifier ~ "(" ~/ expression.rep(sep=",") ~ ")")
      .map { case(name, args) => FunctionEvaluation(name, args.toList)})
  def array[_:P]: P[Array] = P(("[" ~/ expression.rep(sep=",") ~ "]").map( items => Array(items.toList)))
  def factor[_:P]: P[Expression] = P("(" ~/ expression ~ ")" | functionOperation | unaryOperation | array )

  def binFunctions[_:P]: P[BinaryOperator] = P(
    ("*=" | IgnoreCase("contains")).map(_ => Contains) |
      IgnoreCase("notcontains").map(_ => NotContains) |
      IgnoreCase("anyof").map(_ => AnyOf) |
      IgnoreCase("allof").map(_ => AllOf)
  )
  def binaryFuncOp[_:P] : P[Expression] = (factor ~ (binFunctions ~/ factor).?).map {
    case (left, None) => left
    case (left, Some((op, right))) => BinaryOperation(op, left, right)
  }
  // a - b + c => (a-b) + c
  def buildTreeLeftAssociative[T <: BinaryOperator] (head: Expression, tail: Seq[(T, Expression)]): Expression =
    tail.foldLeft(head){ case (a, (op, b)) => BinaryOperation(op, a, b) }

  // This hierarchy enforces the operator precedence ^ > */% > +- > comparisons
  def powerSigns[_:P]: P[Unit] = P(IgnoreCase("power") | "^")
  def mulDivOps[_:P]: P[Expression] = P((binaryFuncOp ~ (powerSigns ~/ binaryFuncOp).?).map {
    case(left, None) => left
    case (left, Some(right)) => BinaryOperation(Power, left, right)
  })

  def mulDivSigns[_:P]: P[BinaryOperator] = P(
    P("*").map(_ => Multiply) |
    P("/").map(_ => Divide) |
    P("%").map(_ => Modulo))
  def plusMinusOps[_:P]: P[Expression] = P(mulDivOps ~ (mulDivSigns ~/ mulDivOps).rep() map {
    case (a, b) => buildTreeLeftAssociative(a, b)
  })

  def plusMinusSigns[_:P]: P[BinaryOperator] = P(P("+").map(_ => Add) | P("-").map(_ => Subtract))
  def compOps[_:P]: P[Expression] = P(plusMinusOps ~ (plusMinusSigns ~/ plusMinusOps).rep() map {
    case (a, b) => buildTreeLeftAssociative(a, b)
  })

  def comparisonOperator[_:P]: P[BinaryOperator] = P(
    ("<=" | IgnoreCase("lessorequal")).map(_ => LessOrEqual) |
      (">=" | IgnoreCase("greaterorequal")).map(_ => GreaterOrEqual) |
      ("<" | IgnoreCase("less")).map(_ => Less) |
      (">" | IgnoreCase("greater")).map(_ => Greater) |
      ("=" | IgnoreCase("equal")).map(_ => Equal) |
      ("!=" | IgnoreCase("notequal")).map(_ => NotEqual)
  )
  def logicAnd[_:P]: P[Expression] = P(compOps ~ (comparisonOperator ~/ compOps).rep() map {
    case (a, b) => buildTreeLeftAssociative(a, b)
  })
  private def andSign[_:P]: P[BinaryOperator] = P((IgnoreCase("and") | "&&").map(_ => And))
  def logicOr[_:P]: P[Expression] = P(logicAnd ~ (andSign ~/ logicAnd).rep() map {
    case (a, b) => buildTreeLeftAssociative(a, b)
  })
  private def orSign[_:P]: P[BinaryOperator] = P((IgnoreCase("or") | "||").map(_ => Or))
  def expression[_:P]: P[Expression] = P(logicOr ~ (orSign ~/ logicOr).rep() map {
    case (a, b) => buildTreeLeftAssociative(a, b)
  })

  def parseAll(in: String): Try[Expression] = parse(in, expression(_)) match {
    case Parsed.Success(expression, _) => scala.util.Success(expression)
    case f: Parsed.Failure =>
      scala.util.Failure(new ParseException(f.trace().msg, f.index))
  }
}
