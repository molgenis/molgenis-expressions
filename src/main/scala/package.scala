package org.molgenis

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Try

package object expression {
  @JSExportTopLevel("evaluate")
  def evaluate(expression: String,
               context: scala.collection.Map[String, Any],
               functions: scala.collection.Map[String, List[Any] => Any]): Try[Any] = {
    val parsedExpression = Parser.parseAll(expression)
    val evaluator = new Evaluator.Evaluator(context, functions)
    parsedExpression.map(evaluator.evaluate)
  }
}
