package org.molgenis.expression

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.{Dictionary, RegExp}
import scala.util.{Failure, Success, Try}

object Expressions {
  // https://stackoverflow.com/a/7091965/1973271
  def age(birthDate: js.Date, d: js.Date): Int = {
    val result = d.getFullYear.toInt - birthDate.getFullYear.toInt
    val m = d.getMonth.toInt - birthDate.getMonth.toInt
    if (m < 0 || (m == 0 && d.getDate() < birthDate.getDate()))
      result - 1
    else result
  }

  def today(params: List[Any]) = new js.Date().toISOString().substring(0, 10)

  def regex(params: List[Any]): Boolean =
    params match {
      case List(_, null)              => false
      case List(_, null, _)           => false
      case List(a: String, b: String) => RegExp(a).test(b)
      case List(a: String, b: String, flags: String) =>
        val invalidFlag = "([^ims])".r
        flags match {
          case invalidFlag(x) =>
            throw new IllegalArgumentException(s"Unknown regex flag: $x")
          case _ =>
        }
        RegExp(a, flags).test(b)
    }

  def convertDate(p: Any): Option[js.Date] =
    p match {
      case s: String              => Some(new js.Date(s))
      case d: js.Date             => Some(d)
      case x if js.isUndefined(x) => None
      case null                   => None
    }

  val ageConvert: List[Any] => Any = (p: List[Any]) =>
    p.map(convertDate) match {
      case Nil                          => null
      case Some(dob) :: Some(date) :: _ => age(dob, date)
      case Some(dob) :: _               => age(dob, new js.Date())
      case None :: _                    => null
    }

  val currentYear: List[Any] => Int = (p: List[Any]) =>
    new js.Date().getFullYear().toInt

  @JSExportTopLevel("functions")
  val functions: mutable.Map[String, List[Any] => Any] = mutable.Map(
    "today" -> today,
    "currentYear" -> currentYear,
    "age" -> ageConvert,
    "regex" -> regex
  )

  @JSExportTopLevel("evaluate")
  def evaluate(expression: String, context: js.Dictionary[js.Any]): Any = {
    val parsedExpression = Parser.parseAll(expression)
    val evaluator = new Evaluator.Evaluator(createContext(context), functions)
    val result: Try[Any] = parsedExpression.flatMap(evaluator.evaluate)
    result match {
      case Success(x)         => x
      case Failure(exception) => throw exception
    }
  }

  def mapValue(v: js.Any): Any =
    v match {
      case a: js.Array[_]         => a.toList
      case x if js.isUndefined(x) => null
      case d: js.Date             => d.toISOString()
      case _                      => v
    }

  def createContext(context: Dictionary[js.Any]): Map[String, Any] =
    context.view
      .mapValues(mapValue)
      .filter({ case (_, value) => value != null })
      .toMap

  @JSExportTopLevel("variableNames")
  def variableNames(expression: String): js.Array[String] = {
    val result = Parser.parseAll(expression).map(Evaluator.getVariables)
    result match {
      case Success(x)         => x.toJSArray
      case Failure(exception) => throw exception
    }
  }
}
