package cron4s.expr

import cron4s.matcher
import cron4s.types.Sequential
import org.scalacheck._

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 01/08/2016.
  */
object SeveralExprSpec extends Properties("SeveralExpr") with ExprGenerators {
  import Prop._
  import Arbitrary.arbitrary

  property("min should be the min value of the head") = forAll(severalExpressions) {
    expr => classify(expr.values.size > 5, "large", "small") {
      expr.min == expr.values.head.min
    }
  }

  property("max should be the max value of the last") = forAll(severalExpressions) {
    expr => classify(expr.values.size > 5, "large", "small") {
      expr.max == expr.values.last.max
    }
  }

  property("range must be the distinct sum of the ranges of its elements") = forAll(severalExpressions) {
    expr => classify(expr.values.size > 5, "large", "small") {
      expr.range == expr.values.flatMap(_.range).distinct.sorted
    }
  }

  val valuesOutsideRange = for {
    expr <- severalExpressions
    value <- arbitrary[Int] if !matcher.anyOf(expr.values.map(_.matches)).apply(value)
  } yield (expr, value)

  val valuesInsideRange = for {
    expr <- severalExpressions
    value <- Gen.choose(expr.min, expr.max) if matcher.anyOf(expr.values.map(_.matches)).apply(value)
  } yield (expr, value)

  property("should not match values outside the range of its elements") = forAll(valuesOutsideRange) {
    case (expr, value) => !expr.matches(value)
  }

  property("should match values inside the range of its elements") = forAll(valuesInsideRange) {
    case (expr, value) => expr.matches(value)
  }

  val stepsFromOutsideUnitRange = for {
    expr      <- severalExpressions
    fromValue <- arbitrary[Int] if fromValue < expr.unit.min || fromValue > expr.unit.max
    stepSize  <- arbitrary[Int]
  } yield (expr, fromValue, stepSize)

  property("stepping from outside the unit's range returns none") = forAll(stepsFromOutsideUnitRange) {
    case (expr, fromValue, stepSize) =>
      expr.step(fromValue, stepSize).isEmpty
  }

  val stepsFromInsideRange = for {
    expr      <- severalExpressions
    fromValue <- Gen.oneOf(expr.range)
    stepSize  <- arbitrary[Int]
  } yield (expr, fromValue, stepSize)

  property("stepping with a zero size step does nothing") = forAll(stepsFromInsideRange) {
    case (expr, fromValue, _) => expr.step(fromValue, 0).contains((fromValue, 0))
  }

  property("stepping with a non-zero size is the same as stepping inside the internal expression") = forAll(stepsFromInsideRange) {
    case (expr, fromValue, stepSize) =>
      val internalRange = Sequential.sequential(expr.range)
      expr.step(fromValue, stepSize) == internalRange.step(fromValue, stepSize)
  }

}
