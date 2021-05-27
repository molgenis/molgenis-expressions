package org.molgenis.expression

import java.time.{LocalDate, ZoneOffset}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Evaluator {
  def age: LocalDate => Int = (d: LocalDate) => d.until(LocalDate.now(ZoneOffset.UTC)).getYears

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
    def evaluate(expression: Expression): Try[Any] = expression match {
      case Null => Success(null)
      case Constant(x) => Success(x)
      case Array(x: List[Expression]) => Try(x.map(evaluate).map(_.get))
      case Variable(name) => Try(context.get(name).orNull)
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
                case (l: Number, r: Number) => arithmetic(operator, l.doubleValue(), r.doubleValue())
                case _ => Failure(new IllegalArgumentException(s"Cannot ${operator} ${leftValue} and ${rightValue}"))
              }
              case operator: ComparisonOperator => (leftValue, rightValue, operator) match {
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
                case _ => Failure(new IllegalArgumentException(s"Cannot ${operator} ${leftValue} and ${rightValue}"))
              }
              case operator: BooleanOperator => (leftValue, rightValue, operator) match {
                case (b1: Boolean, b2: Boolean, And) => Success(b1 && b2)
                case (b1: Boolean, b2: Boolean, Or) => Success(b1 || b2)
                case _ => Failure(new IllegalArgumentException(s"Cannot ${operator} ${leftValue} and ${rightValue}"))
              }
            }

        }
      case UnaryOperation(op, operand) => evaluate(operand) match {
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
          case _ => Failure(new IllegalArgumentException(s"Cannot ${op} ${value}"))
        }
      }
      case FunctionEvaluation(name, args) =>
        Try(functions(name).apply(args.map(evaluate).map(_.get)))
    }
  }
}
