package acleague

import java.io.{FileOutputStream, InputStream, FileInputStream}

import acleague.mserver.{MultipleServerParser, MultipleServerParserFoundGame}

import scala.io.Codec


object ProcessJournalApp extends App {

  def parseSource(inputStream: InputStream): Iterator[MultipleServerParserFoundGame] = {
    scala.io.Source.fromInputStream(inputStream)(Codec.UTF8)
      .getLines()
      .scanLeft(MultipleServerParser.empty)(_.process(_))
      .collect { case m: MultipleServerParserFoundGame => m }
  }

  args.toList match {
    case List(a, b) =>
      val input = if ( a == "-" ) System.in else new FileInputStream(a)
      val output = if ( b == "-") System.out else new FileOutputStream(b, false)
      try parseSource(input)
        .map(g => s"${g.detailString}\n".getBytes("UTF-8"))
        .foreach(b => output.write(b))
      finally { input.close(); output.close() }
  }

}
