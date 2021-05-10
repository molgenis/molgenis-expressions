package org.molgenis.expression

import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

object Evaluator {

  def integerArithmetic[T](operator: ArithmeticOperator, a: T, b: T)
                          (implicit num: Integral[T], f: T => Long): Try[Long] = {
    operator match {
      case Power => Success(scala.math.pow(a.toDouble, b.toDouble).longValue())
      case Multiply => Success(a * b)
      case Divide => Success(a / b)
      case Add => Success(a + b)
      case Subtract => Success(a - b)
      case Modulo => Success(a % b)
    }
  }

  def arithmetic[T] (operator: ArithmeticOperator, a: T, b: T)
                    (implicit num: Fractional[T], f: T => Double): Try[Double] = {
    operator match {
      case Power => Success(scala.math.pow(a.toDouble, b.toDouble))
      case Multiply => Success(a * b)
      case Divide => Success(a / b)
      case Add => Success(a + b)
      case Subtract => Success(a - b)
      case Modulo =>
        Failure(new IllegalArgumentException("Modulo operation can only be used on integer types"))
    }
  }

  def getVariables(expression: Expression): Set[String] = {
    implicit def listToSet(list: List[String]): Set[String] = list.toSet
    expression match {
      case UnaryOperation(_, operand) => getVariables(operand)
      case BinaryOperation(_, left, right) => getVariables(left) ++ getVariables(right)
      case FunctionEvaluation(_, arguments) => arguments flatMap getVariables
      case Constant(_) => Set()
      case Null => Set()
      case Variable(name) => Set(name)
      case Array(items) => items flatMap getVariables
    }
  }

  def evaluateSetOp(left: Set[Any], right: Set[Any], op: SetOperator): Boolean = op match {
    case Contains => left.intersect(right).nonEmpty
    case NotContains => left.intersect(right).isEmpty
    case AnyOf => left.intersect(right).nonEmpty
    case AllOf => left.intersect(right) == right
  }

  class Evaluator(context: scala.collection.Map[String, Any]) {
    def evaluate(expression: Expression): Try[Any] = expression match {
      case Constant(x) => Success(x)
      case Array(x: List[Expression]) => Try(x.map(evaluate).map(_.get))
      case Variable(name) => Try(context(name))
      case BinaryOperation(binaryOp, leftExpr, rightExpr) =>
        val left = evaluate(leftExpr)
        val right = evaluate(rightExpr)
        (left, right) match {
          case (f: Failure[_], _) => f
          case (_, f: Failure[_]) => f
          case _ =>
            val leftValue = left.get
            val rightValue = right.get
            binaryOp match {
              case op: SetOperator =>
                (leftValue, rightValue) match {
                case (leftList: List[Any], rightList: List[Any]) =>
                  Success(evaluateSetOp(leftList.toSet, rightList.toSet, op))
                case (leftElement, rightList: List[Any]) =>
                  Success(evaluateSetOp(Set(leftElement), rightList.toSet, op))
                case (leftList: List[Any], rightElement) =>
                  Success(evaluateSetOp(leftList.toSet, Set(rightElement), op))
                case (leftElement, rightElement) =>
                  Success(evaluateSetOp(Set(leftElement), Set(rightElement), op))
              }

              case operator: ArithmeticOperator => (leftValue, rightValue) match {
                case (l: Double, r: Number) => arithmetic(operator, l, r.doubleValue())
                case (l: Number, r: Double) => arithmetic(operator, l.doubleValue(), r)
                case (l: Float, r: Number) => arithmetic(operator, l, r.floatValue())
                case (l: Number, r: Float) => arithmetic(operator, l.floatValue(), r)
                case (l: Long, r: Number) => integerArithmetic(operator, l, r.longValue())
                case (l: Number, r: Long) => integerArithmetic(operator, l.longValue(), r)
                case (l: Int, r: Int) => integerArithmetic(operator, l, r)
                case _ => Failure(new IllegalArgumentException(s"Bad operand types for operator ${operator}: ${leftValue.getClass}, ${rightValue.getClass}"))
              }
              case operator: ComparisonOperator => (leftValue, rightValue, operator) match {
                case (l: Number, r: Number, Less) => Success(l.doubleValue() < r.doubleValue())
                case (l: Number, r: Number, LessOrEqual) => Success(l.doubleValue() <= r.doubleValue())
                case (l: Any, r: Any, Equal) => Success(l == r)
                case (l: Any, r: Any, NotEqual) => Success(l != r)
                case (l: Number, r: Number, GreaterOrEqual) => Success(l.doubleValue() >= r.doubleValue())
                case (l: Number, r: Number, Greater) => Success(l.doubleValue() > r.doubleValue())
              }
              case operator: BooleanOperator => (leftValue, rightValue, operator) match {
                case (b1: Boolean, b2: Boolean, And) => Success(b1 && b2)
                case (b1: Boolean, b2: Boolean, Or) => Success(b1 || b2)
                case _ => Failure(new IllegalArgumentException(s"Bad operand types for operator ${operator}: ${leftValue.getClass}, ${rightValue.getClass}"))
              }
            }

        }
      case _ => Failure(new NotImplementedError())
    }
  }
}
