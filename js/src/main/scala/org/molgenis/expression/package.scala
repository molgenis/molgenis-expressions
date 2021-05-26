package org.molgenis

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.{Failure, Success, Try}

package object expression {
  def age: LocalDate => Int = (d: LocalDate) => d.until(LocalDate.now(ZoneOffset.UTC).plusDays(1)).getYears
  def ageConvert: List[Any] => Int = (p: List[Any]) => p.head match {
    case l: LocalDate => age(l)
    case s: String => age(LocalDate.parse(s))
    case d: js.Date => age(Instant.parse(d.toISOString).atOffset(ZoneOffset.UTC).toLocalDate)
  }
  def today: List[Any] => LocalDate = _ => LocalDate.now()

  @JSExportTopLevel("evaluate")
  def evaluate(expression: String,
               context: js.Dictionary[js.Any]): Any = {
    val functions = mutable.Map("age" -> ageConvert, "today" -> today)
    val parsedExpression = Parser.parseAll(expression)
    val scalaContext = context.toMap
    val evaluator = new Evaluator.Evaluator(scalaContext, functions)
    val result: Try[Any] = parsedExpression.flatMap(evaluator.evaluate)
    result match {
      case Success(x) => x
      case Failure(exception) => throw exception
    }
  }
}
