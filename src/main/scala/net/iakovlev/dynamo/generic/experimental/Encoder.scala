package net.iakovlev.dynamo.generic.experimental

import shapeless.labelled.FieldType
import shapeless.{::, HList, HNil, LabelledGeneric, Typeable, Witness}

import scala.language.higherKinds
import cats.implicits._

trait SingleFieldEncoder[A] {
  def encode(a: A): Option[AttributeValue]
}

object SingleFieldEncoder {
  implicit val intEncoder = new SingleFieldEncoder[Int] {
    override def encode(a: Int): Option[AttributeValue] =
      Some(AttributeValue(a))
  }
  implicit val stringEncoder = new SingleFieldEncoder[String] {
    override def encode(a: String): Option[AttributeValue] =
      Some(AttributeValue(a))
  }

  implicit def mapAsMapEncoder[A](implicit e: SingleFieldEncoder[A]) =
    new SingleFieldEncoder[Map[String, A]] {
      override def encode(a: Map[String, A]): Option[AttributeValue] = {
        val res1: Option[Map[String, AttributeValue]] =
          a.mapValues(e.encode).sequence
        res1.map(AttributeValueMap.apply)
      }
    }

  implicit def mapAsClassEncoder[A](implicit e: MapEncoder[A]) =
    new SingleFieldEncoder[A] {
      override def encode(a: A): Option[AttributeValue] = {
        e.encode(a)
      }
    }

  implicit def listEncoder[A](implicit e: SingleFieldEncoder[A]) =
    new SingleFieldEncoder[List[A]] {
      override def encode(a: List[A]): Option[AttributeValue] = {
        a.traverse(e.encode).map(AttributeValueList.apply)
      }
    }

  implicit def optionEncoder[A](implicit e: SingleFieldEncoder[A]) =
    new SingleFieldEncoder[Option[A]] {
      override def encode(a: Option[A]): Option[AttributeValue] =
        a.flatMap(e.encode)
    }
}

trait Encoder[A] {
  def encode(a: A): Map[String, Option[AttributeValue]]
}

object Encoder {
  implicit def hNilEncode[H <: HNil] = new Encoder[HNil] {
    override def encode(a: HNil): Map[String, Option[AttributeValue]] =
      Map.empty
  }

  implicit def hConsEncode[K <: Symbol, H, T <: HList](
      implicit eh: SingleFieldEncoder[H],
      et: Encoder[T],
      k: Witness.Aux[K]) =
    new Encoder[FieldType[K, H] :: T] {
      override def encode(
          a: FieldType[K, H] :: T): Map[String, Option[AttributeValue]] = {
        println("encode hCons")
        et.encode(a.tail) + (k.value.name -> eh.encode(a.head))
      }
    }

  implicit def caseClassEncoder[A, R](implicit lg: LabelledGeneric.Aux[A, R],
                                      e: Encoder[R],
                                      t: Typeable[A]) = new Encoder[A] {
    override def encode(a: A): Map[String, Option[AttributeValue]] = {
      println("case class encode " + t.describe)
      e.encode(lg.to(a))
    }
  }

  private def flattenMapValues[A, B](m: Map[A, Option[B]]): Map[A, B] =
    for ((k, mv) <- m; v <- mv) yield k -> v

  def apply[A](a: A)(implicit e: Encoder[A]): Map[String, AttributeValue] =
    flattenMapValues(e.encode(a))
}

trait MapEncoder[A] {
  def encode(a: A): Option[AttributeValueMap]
}

object MapEncoder {
  implicit def hNilEncode[H <: HNil] = new MapEncoder[HNil] {
    override def encode(a: HNil): Option[AttributeValueMap] =
      Some(AttributeValueMap(Map.empty))
  }

  implicit def hConsEncode[K <: Symbol, H, T <: HList](
      implicit eh: SingleFieldEncoder[H],
      et: Encoder[T],
      k: Witness.Aux[K]) =
    new MapEncoder[FieldType[K, H] :: T] {
      override def encode(a: FieldType[K, H] :: T): Option[AttributeValueMap] = {
        println("encode map hCons")
        val res: Map[String, Option[AttributeValue]] =
          et.encode(a.tail) + (k.value.name -> eh.encode(a.head))
        val res1: Option[Map[String, AttributeValue]] = res.sequence
        res1.map(AttributeValueMap.apply)
      }
    }

  implicit def caseClassEncoder[A, R](implicit lg: LabelledGeneric.Aux[A, R],
                                      e: Encoder[R],
                                      t: Typeable[A]) = new MapEncoder[A] {
    override def encode(a: A): Option[AttributeValueMap] = {
      println("case class map encode " + t.describe)
      val res: Option[Map[String, AttributeValue]] =
        e.encode(lg.to(a)).sequence
      res.map(AttributeValueMap.apply)
    }
  }

}
