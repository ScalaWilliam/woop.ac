package io.enet.akka

import akka.util.ByteString
import io.enet.akka.ENetService.PacketFromPeer
import shapeless.ops.hlist.LeftFolder

object Shapper extends App {
  import shapeless._
  import shapeless.poly._
  import shapeless.HList._
  object byteCompressor extends Poly1 {
    // all of these return ByteString
    implicit def caseInt = at[Int](Compressor.intToByteString)
    implicit def caseByte = at[Byte](ByteString(_))
    implicit def caseString = at[String](Compressor.stringToByteString)
  }
  object addByteStrings extends Poly2 {
    implicit def default[T](implicit st: byteCompressor.Case.Aux[T, ByteString]) =
      at[ByteString, T]{ (acc, t) => acc++byteCompressor(t) }
  }
  def compressBytes[P <: Product, F, L <: HList, R](p: P)(
    implicit gen: Generic.Aux[P, L],
    folder: LeftFolder[L, ByteString, addByteStrings.type]) = {
    gen.to(p).foldLeft(ByteString.empty)(addByteStrings)
  }
  implicit class packetFromPeerExtra(input: PacketFromPeer) {
    def reply(i: Int) = {
      input.replyWith(Compressor.intToByteString(i))
    }
    def reply[P <: Product, F, L <: HList, R](p: P)(
      implicit gen: Generic.Aux[P, L],
      folder: LeftFolder[L, ByteString, addByteStrings.type])  = {
      val a = gen.to(p).foldLeft(ByteString.empty)(addByteStrings)
      // don't kill me. I don't get how it works.
      // this is fine, but inside this function the same thing won't work:
      //  val rsly = compressBytes(23, "wat", "dis", 23.toByte)
      //  println(rsly: ByteString)
      val byteString = a.asInstanceOf[ByteString]
      input.replyWith(byteString)
    }
  }


//  implicit def sizeString = size.λ[String](s => s.length)
//  implicit def sizeTuple[T, U](implicit st : size.λ[T], su : size.λ[U]) =
//    size.λ[(T, U)](t => 1+size(t._1)+size(t._2))

//  val l = 23 :: true :: "foo" :: ("bar", "wibble") :: HNil
//  val ls = l map size
}