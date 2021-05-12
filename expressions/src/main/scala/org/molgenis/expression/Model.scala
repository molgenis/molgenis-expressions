package org.molgenis.expression

sealed trait UnaryOperator
case object NotEmpty extends UnaryOperator
case object Empty extends UnaryOperator
case object Negate extends UnaryOperator

sealed trait BinaryOperator

sealed trait SetOperator extends BinaryOperator
case object Contains extends SetOperator
case object NotContains extends SetOperator
case object AnyOf extends SetOperator
case object AllOf extends SetOperator

sealed trait ArithmeticOperator extends BinaryOperator
case object Power extends ArithmeticOperator
case object Multiply extends ArithmeticOperator
case object Divide extends ArithmeticOperator
case object Modulo extends ArithmeticOperator
case object Add extends ArithmeticOperator
case object Subtract extends ArithmeticOperator

sealed trait ComparisonOperator extends BinaryOperator
case object LessOrEqual extends ComparisonOperator
case object GreaterOrEqual extends ComparisonOperator
case object Less extends ComparisonOperator
case object Greater extends ComparisonOperator
case object Equal extends ComparisonOperator
case object NotEqual extends ComparisonOperator

sealed trait BooleanOperator extends BinaryOperator
case object And extends BooleanOperator
case object Or extends BooleanOperator

sealed trait Expression

final case class UnaryOperation(operator: UnaryOperator, operand: Expression) extends Expression
final case class BinaryOperation(operator: BinaryOperator, left: Expression, right: Expression) extends Expression
final case class FunctionEvaluation(name: String, arguments: List[Expression]) extends Expression
final case class Constant(expression: Any) extends Expression
case object Null extends Expression
final case class Variable(name: String) extends Expression
final case class Array(items: List[Expression]) extends Expression
