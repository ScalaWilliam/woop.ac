package acleague

import java.io.{InputStream, FileInputStream}

import acleague.mserver.{MultipleServerParser, MultipleServerParserFoundGame}

import scala.io.Codec


object ProcessJournalApp extends App {

  val inputSource = args.toList match {
    case List("-") => parseSource(System.in)
    case Nil => throw new IllegalArgumentException("Must specify '-' or a file.")
    case files =>
      files.par.foreach { filename =>
        val fis = new FileInputStream(filename)
        try parseSource(fis).map(_.detailString).foreach(println)
        finally fis.close()
      }
  }

  def parseSource(inputStream: InputStream): Iterator[MultipleServerParserFoundGame] = {
    scala.io.Source.fromInputStream(inputStream)(Codec.UTF8)
      .getLines()
      .scanLeft(MultipleServerParser.empty)(_.process(_))
      .collect { case m: MultipleServerParserFoundGame => m }
  }



}
