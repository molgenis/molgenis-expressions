package org.molgenis.expression

import java.text.ParseException
import scala.util.Try
import scala.util.parsing.combinator._

object Parser extends JavaTokenParsers {

  def nullValue: Parser[Null.type] = ("null" | "undefined") ^^ (_ => Null)

  def arithmeticValue: Parser[Double] =
    (decimalNumber ^^ (s => s.toDouble)) |
      (wholeNumber ^^ (s => s.toLong))

  def logicValue: Parser[Boolean] =
    """(?i)true""".r ^^ (_ => true) |
      """(?i)false""".r ^^ (_ => false)

  def singleQuoteStringLiteral: Parser[String] = ("'" + """([^"\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*""" + "'").r

  def stringValue: Parser[String] = (stringLiteral | singleQuoteStringLiteral) ^^ (s => s.substring(1, s.length - 1))

  def constValue: Parser[Constant] = (arithmeticValue | logicValue | stringValue) ^^ (x => Constant(x))

  def variable: Parser[Variable] = "{" ~> ident <~ "}" ^^ (name => Variable(name))

  def atom: Parser[Expression] = nullValue | variable | constValue

  private def unFunction: Parser[UnaryOperator] =
    """(?i)empty""".r ^^ (_ => Empty) |
      """(?i)notempty""".r ^^ (_ => NotEmpty)
  def unaryOperation: Parser[UnaryOperation] =
    ("""!|(?i)negate""".r ^^ (_ => Negate)) ~> expression ^^ (expr => UnaryOperation(Negate, expr)) |
      atom ~ unFunction ^^ { case operand ~ op => UnaryOperation(op, operand) }

  def functionOperation: Parser[FunctionEvaluation] =
    (ident ~ ("(" ~> repsep(expression, ",") <~ ")")) ^^ { case name ~ args => FunctionEvaluation(name, args) }

  def array: Parser[Array] = "[" ~> repsep(expression, ",") <~ "]" ^^ (values => Array(values))

  def factor: Parser[Expression] = ("(" ~> expression <~ ")") | functionOperation | unaryOperation | atom | array

  def binFunctions: Parser[BinaryOperator] =
    """(?i)\*=|contains?""".r ^^ (_ => Contains) |
      """(?i)notcontains?""".r ^^ (_ => NotContains) |
      """(?i)anyof""".r ^^ (_ => AnyOf) |
      """(?i)allof""".r ^^ (_ => AllOf)

  def binaryFuncOp: Parser[Expression] = (factor ~ opt(binFunctions ~ factor) ^^ {
    case left ~ None => left
    case left ~ Some(op ~ right) => BinaryOperation(op, left, right)
  })

  // a - b + c => (a-b) + c
  def buildTreeLeftAssociative[T <: BinaryOperator] (head: Expression, tail: List[~[T, Expression]]): Expression =
    tail.foldLeft(head){ case (a, op ~ b) => BinaryOperation(op, a, b) }

  // This hierarchy enforces the operator precedence ^ > */% > +- > comparisons
  def powerSigns: Parser[Power.type] = """\^|(?i)power""".r ^^ (_ => Power)
  def mulDivOps: Parser[Expression] = binaryFuncOp ~ rep(powerSigns ~ binaryFuncOp) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }
  def mulDivSigns: Parser[BinaryOperator] = ("*" ^^ (_ => Multiply)) | ("/" ^^ (_ => Divide)) | ("%" ^^ (_ => Modulo))
  def plusMinusOps: Parser[Expression] = mulDivOps ~ rep(mulDivSigns ~ mulDivOps) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }
  def plusMinusSigns: Parser[BinaryOperator] = ("+" ^^ (_ => Add)) | ("-" ^^ (_ => Subtract))
  def compOps: Parser[Expression] = plusMinusOps ~ rep(plusMinusSigns ~ plusMinusOps) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }
  def comparisonOperator: Parser[BinaryOperator] =
    """<=|(?i)lessorequal""".r ^^ (_ => LessOrEqual) |
      """>=|(?i)greaterorequal""".r ^^ (_ => GreaterOrEqual) |
      """<|(?i)less""".r ^^ (_ => Less) |
      """>|(?i)greater""".r ^^ (_ => Greater) |
      """=|(?i)equal""".r ^^ (_ => Equal) |
      """!=|(?i)notequal""".r ^^ (_ => NotEqual)
  def logicAnd: Parser[Expression] = compOps ~ rep(comparisonOperator ~ compOps) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }
  def andSign: Parser[BinaryOperator] = """(?i)and|&&""" ^^ (_ => And)
  def logicOr: Parser[Expression] = logicAnd ~ rep(andSign ~ logicAnd) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }
  def orSign: Parser[BinaryOperator] = """(?i)or|||""" ^^ (_ => And)
  def expression: Parser[Expression] = logicOr ~ rep(orSign ~ logicOr) ^^ {
    case a ~ b => buildTreeLeftAssociative(a, b)
  }

  def parseAll(in: CharSequence): Try[Expression] = parseAll(expression, in) match {
    case Success(expression, _) => scala.util.Success(expression)
    case Failure(msg, input) => scala.util.Failure(new ParseException(msg, input.offset))
  }
}
