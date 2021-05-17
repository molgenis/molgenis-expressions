package org.molgenis.expression

import org.molgenis.expression
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.flatspec.AnyFlatSpec

import java.time.LocalDate
import scala.scalajs.js
import scala.util.Try

class packageTest extends AnyFlatSpec {
  "expression.evaluate" should "compute age" in {
    val context = js.Dictionary("dob" -> LocalDate.now().minusYears(3).asInstanceOf[js.Any])
    assert(Try(expression.evaluate("age({dob})", context)).success.value == 3)
  }
}
