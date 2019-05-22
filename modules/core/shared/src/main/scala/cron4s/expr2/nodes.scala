/*
 * Copyright 2017 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cron4s
package expr2

import cron4s.base._
import cron4s.datetime.IsDateTime

sealed trait CronNode[F <: CronField]
object CronNode {
  implicit def cronNodeSteppable[F <: CronField, DT](
      implicit
      R: Steppable[CronRange[F], Int],
      DT: IsDateTime[DT]
  ): Steppable[CronNode[F], DT] = new Steppable[CronNode[F], DT] {
    def step(node: CronNode[F], from: DT, step: Step): Either[ExprError, (DT, Int)] =
      node match {
        case range: RangeNode[F]   => Steppable[RangeNode[F], DT].step(range, from, step)
        case picker: PickerNode[F] => Steppable[PickerNode[F], DT].step(picker, from, step)
      }
  }
}

final case class RangeNode[F <: CronField](range: CronRange[F]) extends CronNode[F]
object RangeNode {

  implicit def rangeNodeSteppable[F <: CronField, DT](
      implicit
      S: Steppable[CronRange[F], Int],
      DT: IsDateTime[DT]
  ): Steppable[RangeNode[F], DT] = new Steppable[RangeNode[F], DT] {
    def step(node: RangeNode[F], from: DT, step: Step): Either[ExprError, (DT, Int)] =
      for {
        currValue             <- DT.get(from, node.range.unit.field)
        (newValue, carryOver) <- S.step(node.range, currValue, step)
        newResult             <- DT.set(from, node.range.unit.field, newValue)
      } yield (newResult, carryOver)
  }

}

final case class PickerNode[F <: CronField](picker: CronPicker[F]) extends CronNode[F]
object PickerNode {
  implicit def pickerNodeSteppable[F <: CronField, DT](
      implicit DT: IsDateTime[DT]
  ): Steppable[PickerNode[F], DT] = new Steppable[PickerNode[F], DT] {
    def step(node: PickerNode[F], from: DT, step: Step): Either[ExprError, (DT, Int)] =
      node.picker.pickFrom(from).map(_ -> 0)
  }
}
