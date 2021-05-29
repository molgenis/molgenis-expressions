package org.molgenis.expression

import org.molgenis.expression.Evaluator.{age, today}

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.{Dictionary, |}
import scala.util.{Failure, Success, Try}
import scala.scalajs.js.JSConverters._

@JSExportTopLevel("Expressions")
object Expressions {
  def ageConvert: List[Any] => Int|Null = (p: List[Any]) => p.head match {
    case l: LocalDate => age(l)
    case s: String => age(LocalDate.parse(s))
    case d: js.Date => age(Instant.parse(d.toISOString).atOffset(ZoneOffset.UTC).toLocalDate)
    case x if js.isUndefined(x) => null
    case null => null
  }

  @JSExport
  def evaluate(expression: String,
               context: js.Dictionary[js.Any]): Any = {
    val functions = mutable.Map("age" -> ageConvert, "today" -> today)
    val parsedExpression = Parser.parseAll(expression)
    val evaluator = new Evaluator.Evaluator(createContext(context), functions)
    val result: Try[Any] = parsedExpression.flatMap(evaluator.evaluate)
    result match {
      case Success(x) => x
      case Failure(exception) => throw exception
    }
  }

  def createContext(context: Dictionary[js.Any]): Map[String, Any] =
    context.toMap.filter({ case (_, value) => !js.isUndefined(value) })

  @JSExport
  def variableNames(expression: String): js.Array[String] = {
    val result = Parser.parseAll(expression).map(Evaluator.getVariables)
    result match {
      case Success(x) => x.toJSArray
      case Failure(exception) => throw exception
    }
  }
}
