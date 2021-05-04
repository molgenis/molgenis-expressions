package org.molgenis.expression

sealed trait UnaryOperator
case object NotEmpty extends UnaryOperator
case object Empty extends UnaryOperator
case object Negate extends UnaryOperator

sealed trait BinaryOperator
// binary functions
sealed trait BinaryFunction extends BinaryOperator
case object Contains extends BinaryOperator
case object NotContains extends BinaryOperator
case object AnyOf extends BinaryOperator
case object AllOf extends BinaryOperator
// arithmetic operations
case object Power extends BinaryOperator
case object Multiply extends BinaryOperator
case object Divide extends BinaryOperator
case object Modulo extends BinaryOperator
case object Add extends BinaryOperator
case object Subtract extends BinaryOperator
// comparison
case object LessOrEqual extends BinaryOperator
case object GreaterOrEqual extends BinaryOperator
case object Less extends BinaryOperator
case object Greater extends BinaryOperator
case object Equal extends BinaryOperator
case object NotEqual extends BinaryOperator
// boolean logic
case object And extends BinaryOperator
case object Or extends BinaryOperator

sealed trait Expression

final case class ArrayExpression(items: List[Expression]) extends Expression
final case class UnaryOperation(operator: UnaryOperator, operand: Expression) extends Expression
final case class BinaryOperation(operator: BinaryOperator, left: Expression, right: Expression) extends Expression
final case class FunctionEvaluation(name: String, arguments: List[Expression]) extends Expression
final case class Constant(expression: Any) extends Expression
case object Null extends Expression
final case class Variable(name: String) extends Expression
final case class Array(items: List[Expression]) extends Expression
