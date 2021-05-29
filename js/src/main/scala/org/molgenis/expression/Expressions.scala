package org.molgenis.expression

import org.molgenis.expression.Evaluator.regex

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.Dictionary
import scala.util.{Failure, Success, Try}

@JSExportTopLevel("Expressions")
object Expressions {
  // https://stackoverflow.com/a/7091965/1973271
  def age(birthDate: js.Date): Int = {
    val today = new js.Date()
    val result = today.getFullYear.toInt - birthDate.getFullYear.toInt
    val m = today.getMonth.toInt - birthDate.getMonth.toInt
    if (m < 0 || (m == 0 && today.getDate() < birthDate.getDate()))
      result - 1
    else result
  }
  def today(params: List[Any]) = new js.Date()
  def ageConvert(p: List[Any]): Any = p.head match {
    case s: String => age(new js.Date(s))
    case d: js.Date => age(d)
    case x if js.isUndefined(x) => null
    case null => null
  }
  @JSExport
  val functions: mutable.Map[String, List[Any] => Any] = mutable.Map(
    "today" -> today,
    "age" -> ageConvert,
    "regex" -> regex
  )

  @JSExport
  def evaluate(expression: String,
               context: js.Dictionary[js.Any]): Any = {
    val parsedExpression = Parser.parseAll(expression)
    val evaluator = new Evaluator.Evaluator(createContext(context), functions)
    val result: Try[Any] = parsedExpression.flatMap(evaluator.evaluate)
    result match {
      case Success(x) => x
      case Failure(exception) => throw exception
    }
  }

  def createContext(context: Dictionary[js.Any]): Map[String, Any] =
    context.toMap
      .filter({ case (_, value) => !js.isUndefined(value) })
      .filter({ case (_, value) => value != null })

  @JSExport
  def variableNames(expression: String): js.Array[String] = {
    val result = Parser.parseAll(expression).map(Evaluator.getVariables)
    result match {
      case Success(x) => x.toJSArray
      case Failure(exception) => throw exception
    }
  }
}
