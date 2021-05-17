package org.molgenis

import java.time.LocalDate
import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.{Failure, Success, Try}

package object expression {
  @JSExportTopLevel("evaluate")
  def evaluate(expression: String,
               context: js.Dictionary[js.Any]): Any = {
    // TODO: interop with js dates?
    def today: List[Any] => LocalDate = _ => LocalDate.now()
    def age: List[Any] => Int = (p: List[Any]) => p.head.asInstanceOf[LocalDate].until(LocalDate.now().plusDays(1)).getYears
    val functions = mutable.Map("age" -> age, "today" -> today)

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
