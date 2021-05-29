package org.molgenis.expression

import java.time.{LocalDate, ZoneOffset}
import scala.Double.NaN
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Evaluator {
  def regex: List[Any] => Boolean = {
    case List(_, null) => false
    case List(a: String, b: String) => a.r.matches(b)
  }

  def arithmetic(operator: ArithmeticOperator, a: Double, b: Double): Try[Double] = {
    operator match {
      case Power => Try(scala.math.pow(a, b))
      case Multiply => Try(a * b)
      case Divide => Try(a / b)
      case Add => Try(a + b)
      case Subtract => Try(a - b)
      case Modulo => Try(a.toLong % b.toLong)
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
    case NotContains => left.intersect(right) != right
    case AnyOf => left.intersect(right).nonEmpty
    case Contains | AllOf => left.intersect(right) == right
  }

  class Evaluator(context: scala.collection.Map[String, Any],
                  functions: scala.collection.Map[String, List[Any] => Any]) {
    private def handleBoolean(op: BooleanOperator, left: Boolean, right: Boolean): Success[Boolean] = op match {
      case Or => Success(left | right)
      case And => Success(left & right)
    }

    private def handleSet(op: SetOperator, left: Any, right: Any): Success[Boolean] = (left, right) match {
      case (leftList: List[Any], rightList: List[Any]) =>
        Success(evaluateSetOp(leftList.toSet, rightList.toSet, op))
      case (leftElement, rightList: List[Any]) =>
        Success(evaluateSetOp(Set(leftElement), rightList.toSet, op))
      case (leftList: List[Any], rightElement) =>
        Success(evaluateSetOp(leftList.toSet, Set(rightElement), op))
      case (leftElement, rightElement) =>
        Success(evaluateSetOp(Set(leftElement), Set(rightElement), op))
    }

    private def handleArithmetic(op: ArithmeticOperator, left: Any, right: Any): Try[Double] = (left, right) match {
      case (l: Number, r: Number) => arithmetic(op, l.doubleValue(), r.doubleValue())
      case _ => Failure(new IllegalArgumentException(s"Cannot $op $left and $right"))
    }

    private def handleComparison(op: ComparisonOperator, left: Any, right: Any) = (left, right, op) match {
      case (l: Number, r: Number, Less) => Success(l.doubleValue() < r.doubleValue())
      case (l: Number, r: Number, LessOrEqual) => Success(l.doubleValue() <= r.doubleValue())
      case (l: Number, r: Number, GreaterOrEqual) => Success(l.doubleValue() >= r.doubleValue())
      case (l: Number, r: Number, Greater) => Success(l.doubleValue() > r.doubleValue())
      case (l: Any, r: Any, Equal) => Success(l == r)
      case (l: Any, r: Any, NotEqual) => Success(l != r)
      case (null, null, Equal) => Success(true)
      case (null, _, Equal) => Success(false)
      case (_, null, Equal) => Success(false)
      case (null, null, NotEqual) => Success(false)
      case (null, _, NotEqual) => Success(true)
      case (_, null, NotEqual) => Success(true)
      case _ => Failure(new IllegalArgumentException(s"Cannot $op $left and $right"))
    }

    def isTruthy(x: Any): Boolean = x match {
      case false => false
      case 0.0 => false
      case null => false
      case s: String => s.nonEmpty
      case d:Double => !d.isNaN
      case _ => true
    }

    private def handleBinary(op: BinaryOperator, leftExpr: Expression, rightExpr: Expression): Try[Any] = {
      val left = evaluate(leftExpr).map(isTruthy)
      if (left.isFailure) return left
      (left.get, op) match {
        // Eager evaluation
        case (x, And) if !isTruthy(x) => Success(false)
        case (x, Or) if isTruthy(x) => Success(true)
        case _ =>
          val right = evaluate(rightExpr)
          if (right.isFailure) return right
          op match {
            case op: BooleanOperator => handleBoolean(op, isTruthy(left.get), isTruthy(right.get))
            case op: SetOperator => handleSet(op, left.get, right.get)
            case op: ArithmeticOperator => handleArithmetic(op, left.get, right.get)
            case op: ComparisonOperator => handleComparison(op, left.get, right.get)
          }
      }
    }

    private def handleUnary(op: UnaryOperator, operand: Expression) = {
      evaluate(operand) match {
        case Failure(f) => Failure(f)
        case Success(value) => (op, value) match {
          case (Negate, null) => Success(true)
          case (Negate, b: Boolean) => Success(!b)
          case (Empty, c: Iterable[Any]) => Success(c.isEmpty)
          case (Empty, s: String) => Success(s.isEmpty)
          case (Empty, null) => Success(true)
          case (Empty, _) => Success(false)
          case (NotEmpty, c: Iterable[Any]) => Success(c.nonEmpty)
          case (NotEmpty, s: String) => Success(s.nonEmpty)
          case (NotEmpty, null) => Success(false)
          case (NotEmpty, _) => Success(true)
          case _ => Failure(new IllegalArgumentException(s"Cannot $op $value"))
        }
      }
    }

    def evaluate(expression: Expression): Try[Any] = expression match {
      case Null => Success(null)
      case Constant(x) => Success(x)
      case Array(x: List[Expression]) => Try(x.map(evaluate).map(_.get))
      case Variable(name) => Try(context.get(name).orNull)
      case BinaryOperation(binaryOp, leftExpr, rightExpr) => handleBinary(binaryOp, leftExpr, rightExpr)
      case UnaryOperation(op, operand) => handleUnary(op, operand)
      case FunctionEvaluation(name, args) => Try(functions(name).apply(args.map(evaluate).map(_.get)))
    }
  }
}
