package org.molgenis.expression

import fastparse.CharPredicates.isPrintableChar
import fastparse.SingleLineWhitespace._
import fastparse._

import scala.util.Try

object Parser {
  private def precedence(op: BinaryOperator): Int = op match {
    case _: SetOperator => 7
    case Power => 6
    case Multiply | Divide | Modulo => 5
    case Add | Subtract => 4
    case _: ComparisonOperator => 3
    case And => 2
    case Or => 1
  }

  private sealed trait Associativity

  private case object LeftAssociative extends Associativity

  private case object RightAssociative extends Associativity

  private def associativity(op: BinaryOperator): Associativity = op match {
    case Power => RightAssociative
    case _ => LeftAssociative
  }

  def nullValue[_: P]: P[Null.type] = P(StringIn("null", "undefined").map(_ => Null))

  def digit[_: P]: P[String] = P(CharIn("0-9").!)

  def wholeNumber[_: P]: P[Number] = P((P("-").? ~ digit.rep(1)).!
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

  private def escapedChar[_: P]: P[String] = P("\\" ~~ CharIn("""'"\\bfnrt""").!).map {
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

  def identifier[_: P]: P[String] = P((
    CharIn("A-Z", "a-z", "$", "_") ~
      CharIn("A-Z", "a-z", "0-9", "$", "_").rep() ~
      ("-" ~ CharIn("a-z").rep(2)).? // -nl, -fr etc
    ).!)
    .opaque("<identifier>")

  def variable[_: P]: P[Variable] = P("{" ~/ (identifier ~ ("." ~/ identifier).rep()).! ~ "}") map Variable

  def atom[_: P]: P[Expression] = P(nullValue | variable | constValue | array)

  private def unFunction[_: P]: P[UnaryOperator] = P(
    IgnoreCase("empty").map(_ => Empty) |
      IgnoreCase("notempty").map(_ => NotEmpty))

  def unaryOperation[_: P]: P[Expression] =
    P((("!" | IgnoreCase("negate")) ~/ factor.map {
      UnaryOperation(Negate, _)
    }) |
      (atom ~ unFunction.?).map {
        case (atom, None) => atom
        case (operand, Some(op)) => UnaryOperation(op, operand)
      })

  def functionOperation[_: P]: P[FunctionEvaluation] =
    P((identifier ~ "(" ~/ expression.rep(sep = ",") ~ ")")
      .map { case (name, args) => FunctionEvaluation(name, args.toList) })

  def array[_: P]: P[Array] = P(("[" ~/ expression.rep(sep = ",") ~ "]").map(items => Array(items.toList)))

  def factor[_: P]: P[Expression] = P("(" ~/ expression ~ ")" | functionOperation | unaryOperation)

  def binFunctions[_: P]: P[BinaryOperator] = P(
    ("*=" | IgnoreCase("contains")).map(_ => Contains) |
      IgnoreCase("notcontains").map(_ => NotContains) |
      IgnoreCase("anyof").map(_ => AnyOf) |
      IgnoreCase("allof").map(_ => AllOf) |
      ("^" | IgnoreCase("power")).map(_ => Power) |
      P("*").map(_ => Multiply) |
      P("/").map(_ => Divide) |
      P("%").map(_ => Modulo) |
      P("+").map(_ => Add) |
      P("-").map(_ => Subtract) |
      ("<=" | IgnoreCase("lessorequal")).map(_ => LessOrEqual) |
      (">=" | IgnoreCase("greaterorequal")).map(_ => GreaterOrEqual) |
      ("<" | IgnoreCase("less")).map(_ => Less) |
      (">" | IgnoreCase("greater")).map(_ => Greater) |
      ("=" | IgnoreCase("equal")).map(_ => Equal) |
      ("!=" | IgnoreCase("notequal")).map(_ => NotEqual) |
      ("&&" | IgnoreCase("and")).map(_ => And) |
      ("||" | IgnoreCase("or")).map(_ => Or)
  )

  def expression[_: P]: P[Expression] = (factor ~ (binFunctions ~/ factor).rep()).map({
    case (pre, fs) =>
      // Use precedence climbing algorithm to shape the operator tree
      var remaining = fs

      def climb(minPrec: Int, current: Expression): Expression = {
        var result = current
        while (remaining.headOption match {
          case None => false
          case Some((op, next)) =>
            val prec: Int = precedence(op)
            if (prec < minPrec) false
            else {
              remaining = remaining.tail
              val nextPrecedence = if (associativity(op) == LeftAssociative) prec + 1 else prec
              val rhs = climb(nextPrecedence, next)
              result = BinaryOperation(op, result, rhs)
              true
            }
        }) ()
        result
      }

      climb(0, pre)
  })

  def expressionEnd[_: P]: P[Expression] = P(expression ~ End)

  case class ParseError(msg: String, index: Int) extends Exception(msg)

  def parseAll(in: String): Try[Expression] = parse(in, expressionEnd(_)) match {
    case Parsed.Success(expression, _) => scala.util.Success(expression)
    case f: Parsed.Failure =>
      scala.util.Failure(ParseError(f.trace().msg, f.index))
  }
}
