import java.nio.file.{Files, Path, Paths}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.alpakka.xml.{EndElement, StartElement}
import akka.stream.alpakka.xml.scaladsl.{XmlParsing, XmlWriting}
import akka.stream.scaladsl.{FileIO, Sink}

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def convert(file: Path): Future[String] = {
    val target = Paths.get("/tmp/out", file.getFileName.toString)
    FileIO.fromPath(file)
//      .map(x => {println("bs"); x})
      .via(XmlParsing.parser)
      .statefulMapConcat(() => {
        var inArticle = false
        var result = List()
        _ match {
          case s: StartElement if s.localName == "article" =>
            inArticle = true
            immutable.Seq(s)
          case e: EndElement if e.localName == "article" =>
            inArticle = false
            immutable.Seq(e)
          case element if inArticle =>
            immutable.Seq(element)
          case _ =>
            immutable.Seq.empty
        }
      })
//      .map(e => { println(s"e ${e}."); e })
      .via(XmlWriting.writer)
      .recover {
        case t: Throwable =>
          throw new IllegalStateException(s"Error processing $file", t)
      }
//      .map(bs => { println(s"bs ${bs.length}."); bs })
      .runWith(FileIO.toPath(target))
      .map(ioResult => {
        if (ioResult.count == 0)
          throw new IllegalStateException(s"Error processing $file: empty result")
        s"Wrote $file to $target: $ioResult"
      })
  }

  try {
    val done = Directory
      .walk(Paths.get(args(0)))
      .filter(_.toFile.isFile)
      .filter(_.toFile.getName.endsWith(".html"))
      .mapAsync(10)(convert)
      .runForeach(println)
    //    .runWith(Sink.ignore)
//    val done =
//      convert(Paths.get("/home/aengelen/dev/akka/akka-docs/target/paradox/site/main/stream/operators/Source/zipN.html"))
//    done.onComplete(println)

    Await.result(done, 130.seconds)
  } finally {
    system.terminate()
  }

}