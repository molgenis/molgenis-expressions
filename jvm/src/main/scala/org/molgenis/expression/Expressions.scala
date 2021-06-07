package org.molgenis.expression

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import org.molgenis.expression.Evaluator.regex
import org.molgenis.expression.Parser.parseAll

import java.time.{LocalDate, ZoneOffset}
import java.util
import scala.jdk.javaapi.CollectionConverters.{asJava, asScala}
import scala.util.{Failure, Success, Try}

class Expressions(val maxCacheSize: Int = 1000) {
  private val expressionCache: LoadingCache[String, Try[Expression]] =
    Caffeine.newBuilder.maximumSize(maxCacheSize).build(this.load)

  private def load(expression: String): Try[Expression] = parseAll(expression)

  def age: LocalDate => Int = (d: LocalDate) => d.until(LocalDate.now(ZoneOffset.UTC)).getYears
  def today: List[Any] => LocalDate = _ => LocalDate.now()

  /**
   * Get all variable names used in expressions. Skips expressions that it cannot parse.
   *
   * @param expressions List of expression Strings
   * @return Set of variable names
   */
  def getAllVariableNames(expressions: util.List[String]): util.Set[String] = {
    val parsed = asScala(expressionCache.getAll(expressions))
    val variables = parsed.values.filter(_.isSuccess).map(_.get).toSet
      .flatMap(org.molgenis.expression.Evaluator.getVariables)
    asJava(variables)
  }

  def map2Map(m:util.Map[String, Any]): Map[String, Any] = asScala(m).view.mapValues(mapValue).toMap

  def mapValue(v: Any): Any = v match {
    case b: java.lang.Byte => Byte2byte(b)
    case s: java.lang.Short => Short2short(s)
    case c: java.lang.Character => Character2char(c)
    case i: java.lang.Integer => Integer2int(i)
    case l: java.lang.Long => Long2long(l)
    case f: java.lang.Float => Float2float(f)
    case d: java.lang.Double => Double2double(d)
    case b: java.lang.Boolean => Boolean2boolean(b)
    case l: util.Collection[Any] => asScala(l).toList.map(mapValue)
    case m: util.Map[String, Any] => map2Map(m)
  }

  /**
   * Try to get all variable names used in an expression.
   * @param expression the expression to get variable names from
   * @return Set of variable names
   * @throws exception if the parsing fails
   */
  def getVariableNames(expression: String): util.Set[String] =
    expressionCache.get(expression)
      .map(org.molgenis.expression.Evaluator.getVariables) match {
      case Success(result) => asJava(result)
      case Failure(exception) => throw exception
    }

  /**
   * Evaluates a list of expressions within a context
   * @param expressions the expressions to parse and evaluate
   * @param context the context in which to evaluate the expressions
   * @return List<Try> containing the results of the parsing and evaluating
   */
  def parseAndEvaluate(expressions: util.List[String],
                       context: util.Map[String, Any]): util.List[Try[Any]] = {
    val scalaExpressions: List[String] = asScala(expressions).toList
    val scalaContext: Map[String, Any] = map2Map(context)

    def ageConvert: List[Any] => Any = (p: List[Any]) => p.head match {
      case l: LocalDate => age(l)
      case s: String => age(LocalDate.parse(s))
      case null => null
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
