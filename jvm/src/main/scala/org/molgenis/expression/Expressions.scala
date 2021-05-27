package org.molgenis.expression

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import org.molgenis.expression.Evaluator.age
import org.molgenis.expression.Parser.parseAll

import java.time.{LocalDate, ZoneOffset}
import java.util
import scala.jdk.javaapi.CollectionConverters.{asJava, asScala}
import scala.util.Try

class Expressions(val maxCacheSize: Int = 1000) {
  private val expressionCache: LoadingCache[String, Try[Expression]] =
    Caffeine.newBuilder.maximumSize(maxCacheSize).build(this.load)

  private def load(expression: String): Try[Expression] = parseAll(expression)

  /**
   * Get all variable names used in expressions. Skips expressions that it cannot parse.
   *
   * @param expressions List of expression Strings
   * @return Set of variable names
   */
  def getVariableNames(expressions: util.List[String]): util.Set[String] = {
    val parsed = asScala(expressionCache.getAll(expressions))
    val variables = parsed.values.filter(_.isSuccess).map(_.get).toSet
      .flatMap(org.molgenis.expression.Evaluator.getVariables)
    asJava(variables)
  }

  def parseAndEvaluate(expressions: util.List[String],
                       context: util.Map[String, Any]): util.List[Try[Any]] = {
    val scalaExpressions: List[String] = asScala(expressions).toList
    val scalaContext: Map[String, Any] = asScala(context).toMap

    def ageConvert: List[Any] => Any = (p: List[Any]) => p.head match {
      case l: LocalDate => age(l)
      case s: String => age(LocalDate.parse(s))
      case null => null
    }

    def today: List[Any] => LocalDate = _ => LocalDate.now(ZoneOffset.UTC)

    def regex: List[Any] => Boolean = {
      case List(_, null) => false
      case List(a: String, b: String) => a.r.matches(b)
    }

    val functions = Map(
      "age" -> ageConvert,
      "today" -> today,
      "regex" -> regex
    )

    val evaluator: Evaluator.Evaluator = new Evaluator.Evaluator(scalaContext, functions)
    val evaluated: List[Try[Any]] = scalaExpressions
      .map(expressionCache.get)
      .map(expression => expression.flatMap(evaluator.evaluate))
    asJava(evaluated)
  }
}
