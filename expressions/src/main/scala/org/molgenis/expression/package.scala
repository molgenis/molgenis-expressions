package org.molgenis

import java.time.LocalDate
import scala.collection.mutable
import scala.scalajs.js.annotation.{JSExportTopLevel}
import scala.util.Try

package object expression {
  @JSExportTopLevel("evaluate")
  def evaluate(expression: String,
               context: scala.collection.Map[String, Any]): Try[Any] = {
    def today: List[Any] => LocalDate = _ => LocalDate.now()
    def age: List[Any] => Int = (p: List[Any]) => p.head.asInstanceOf[LocalDate].until(LocalDate.now().plusDays(1)).getYears
    val functions = mutable.Map("age" -> age, "today" -> today)

    val parsedExpression = Parser.parseAll(expression)
    val evaluator = new Evaluator.Evaluator(context, functions)
    parsedExpression.flatMap(evaluator.evaluate)
  }
}