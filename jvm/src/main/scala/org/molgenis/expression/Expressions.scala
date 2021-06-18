package org.molgenis.expression

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import org.molgenis.expression.Parser.parseAll

import java.time.{Instant, LocalDate}
import java.util
import java.util.regex.Pattern
import java.util.regex.Pattern.{CASE_INSENSITIVE, DOTALL, MULTILINE}
import scala.jdk.javaapi.CollectionConverters.{asJava, asScala}
import scala.util.{Failure, Success, Try}

class Expressions(val maxCacheSize: Int = 1000) {
  private val expressionCache: LoadingCache[String, Try[Expression]] =
    Caffeine.newBuilder.maximumSize(maxCacheSize).build(this.load)

  private def load(expression: String): Try[Expression] = parseAll(expression)
  private def today: List[Any] => String = _ => LocalDate.now().toString

  private def regex: List[Any] => Boolean = {
    case List(_, null)              => false
    case List(a: String, b: String) => Pattern.compile(a).matcher(b).matches
    case List(a: String, b: String, flags: String) =>
      val flag: Int = flags.toList.foldLeft(0)((flags, flagChar) =>
        flagChar match {
          case 'i' => flags | CASE_INSENSITIVE
          case 'm' => flags | MULTILINE
          case 's' => flags | DOTALL
          case x =>
            throw new IllegalArgumentException(s"Unknown regex flag: $x")
        }
      )
      Pattern.compile(a, flag).matcher(b).matches
  }

  private def toLocalDate(value: Any): Option[LocalDate] =
    value match {
      case l: LocalDate => Some(l)
      case s: String    => Some(LocalDate.parse(s))
      case _            => None
    }

  private val ageConvert: List[Any] => Any = (p: List[Any]) =>
    p.map(toLocalDate) match {
      case Nil                          => null
      case Some(dob) :: Some(date) :: _ => dob.until(date).getYears
      case Some(dob) :: _               => dob.until(LocalDate.now()).getYears
      case None :: _                    => null
    }

  private val currentYear: List[Any] => Int = (p: List[Any]) =>
    LocalDate.now().getYear

  /**
    * Get all variable names used in expressions. Skips expressions that it cannot parse.
    *
   * @param expressions List of expression Strings
    * @return Set of variable names
    */
  def getAllVariableNames(expressions: util.List[String]): util.Set[String] = {
    val parsed = asScala(expressionCache.getAll(expressions))
    val variables = parsed.values
      .filter(_.isSuccess)
      .map(_.get)
      .toSet
      .flatMap(org.molgenis.expression.Evaluator.getVariables)
    asJava(variables)
  }

  def map2Map(m: util.Map[String, Any]): Map[String, Any] =
    asScala(m).view.mapValues(mapValue).toMap

  def mapValue(v: Any): Any =
    v match {
      case b: java.lang.Byte        => Byte2byte(b)
      case s: java.lang.Short       => Short2short(s)
      case c: java.lang.Character   => Character2char(c)
      case i: java.lang.Integer     => Integer2int(i)
      case l: java.lang.Long        => Long2long(l)
      case f: java.lang.Float       => Float2float(f)
      case d: java.lang.Double      => Double2double(d)
      case b: java.lang.Boolean     => Boolean2boolean(b)
      case l: util.Collection[Any]  => asScala(l).toList.map(mapValue)
      case m: util.Map[String, Any] => map2Map(m)
      case ld: LocalDate            => ld.toString
      case dt: Instant              => dt.toString
      case _                        => v
    }

  /**
    * Try to get all variable names used in an expression.
    *
   * @param expression the expression to get variable names from
    * @return Set of variable names
    * @throws exception if the parsing fails
    */
  def getVariableNames(expression: String): util.Set[String] =
    expressionCache
      .get(expression)
      .map(org.molgenis.expression.Evaluator.getVariables) match {
      case Success(result)    => asJava(result)
      case Failure(exception) => throw exception
    }

  /**
    * Evaluates a list of expressions within a context
    *
   * @param expressions the expressions to parse and evaluate
    * @param context     the context in which to evaluate the expressions
    * @return List<Try> containing the results of the parsing and evaluating
    */
  def parseAndEvaluate(
      expressions: util.List[String],
      context: util.Map[String, Any]
  ): util.List[Try[Any]] = {
    val scalaExpressions: List[String] = asScala(expressions).toList
    val scalaContext: Map[String, Any] = map2Map(context)

    val functions = Map(
      "today" -> today,
      "currentYear" -> currentYear,
      "age" -> ageConvert,
      "regex" -> regex
    )

    val evaluator: Evaluator.Evaluator =
      new Evaluator.Evaluator(scalaContext, functions)
    val evaluated: List[Try[Any]] = scalaExpressions
      .map(expressionCache.get)
      .map(expression => expression.flatMap(evaluator.evaluate))
    asJava(evaluated)
  }
}
