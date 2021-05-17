package org.molgenis.expression

import org.molgenis.expression
import org.scalatest.flatspec.AnyFlatSpec

import java.time.LocalDate
import org.scalatest.TryValues._

class packageTest extends AnyFlatSpec {
  "expression.evaluate" should "compute age" in {
    val context = Map("dob" -> LocalDate.now().minusYears(3))
    assert(expression.evaluate("age({dob})", context).success.value == 3)
  }

}
