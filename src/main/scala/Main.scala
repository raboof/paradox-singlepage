import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def convert(file: Path): Future[ByteString] = {
    FileIO.fromPath(file)
      .runReduce(_ ++ _)
      .map(page => {
        val article = "(?s)<article.*</article>".r.findFirstMatchIn(page.utf8String).get.group(0)

        ByteString(article
            .replaceAll("https://embed.scalafiddle.io/integration.js", "")
        )
      })
      .recover {
        case t: Throwable =>
          throw new IllegalStateException(s"Error processing $file", t)
      }
  }

  try {
    val source = Paths.get(args(0))
    val target = Paths.get(args(1))

    val onePageBody: Source[ByteString, _] = Directory
      .walk(source)
      .filter(_.toFile.isFile)
      .filter(_.toFile.getName.endsWith(".html"))
      .mapAsync(parallelism=20)(file =>
      // TODO proper header/pagebreak
        convert(file).map(ByteString(s"\n\n<!-- include of $file -->\n\n") ++ _)
      )

    val header = Source.single(ByteString(
      """<!DOCTYPE html>
<html class="no-js" lang="en">

<head>
<title>Akka Documentation</title>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<meta name="description" content="akka-docs"/>
<link rel="canonical" href="https://doc.akka.io/docs/akka/current/index.html"/>
<script type="text/javascript" src="lib/jquery/jquery.min.js"></script>
<script type="text/javascript" src="lib/foundation/dist/js/foundation.min.js"></script>
<link rel="stylesheet" type="text/css" href="lib/normalize.css/normalize.css"/>
<link rel="stylesheet" type="text/css" href="lib/foundation/dist/css/foundation.min.css"/>
<link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/docsearch.js/2/docsearch.min.css" />
<link rel="stylesheet" type="text/css" href="css/icons.css"/>
<link rel="stylesheet" type="text/css" href="css/page.css"/>
<link rel="shortcut icon" href="images/favicon.ico" />
<link rel="apple-touch-icon" sizes="180x180" href="images/apple-touch-icon.png"/>
<link rel="icon" type="image/png" sizes="32x32" href="images/favicon-32x32.png"/>
<link rel="icon" type="image/png" sizes="16x16" href="images/favicon-16x16.png"/>
<link rel="manifest" href="images/manifest.json"/>
<meta name="msapplication-TileImage" content="images/mstile-150x150.png"/>
<meta name="msapplication-TileColor" content="#15a9ce"/>
<meta name="theme-color" content="#15a9ce"/>
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent"/>
</head>
<body id="underlay" data-toggler="nav-open">
<main class="fixed-shift-for-large expanded row">
<section class="site-content small-12 column">
      """.stripMargin))
    val footer = Source.single(ByteString(
      """
<footer class="page-footer row clearfix">
<img class="akka-icon float-left show-for-medium" src="images/akka-icon.svg"/>
<section class="copyright">
<div>Akka is Open Source and available under the Apache 2 License.</div>
<p class="legal">
&copy; 2011-2019 <a href="https://www.lightbend.com" target="_blank">Lightbend, Inc.</a> |
<a href="https://www.lightbend.com/legal/licenses" target="_blank">Licenses</a> |
<a href="https://www.lightbend.com/legal/terms" target="_blank">Terms</a> |
<a href="https://www.lightbend.com/legal/privacy" target="_blank">Privacy Policy</a> |
<a href="https://akka.io/cookie/" target="_blank">Cookie Listing</a> |
<a class="optanon-toggle-display">Cookie Settings</a>
</p>
</section>
</footer>

</section>
</main>
<script type="text/javascript" src="js/scrollsneak.js"></script>
<script type="text/javascript">jQuery(document).foundation();</script>
<script type="text/javascript" src="js/groups.js"></script>
<script type="text/javascript" src="js/page.js"></script>
<script type="text/javascript" src="js/magellan.js"></script>

<style type="text/css">@import "lib/prettify/prettify.css";</style>
<script type="text/javascript" src="lib/prettify/prettify.js"></script>
<script type="text/javascript" src="lib/prettify/lang-scala.js"></script>
<script type="text/javascript">//<![CDATA[
jQuery(function(){window.prettyPrint && prettyPrint()});
//]]></script>


</body>
</html>
      """.stripMargin))

    val done =
      header.concat(onePageBody).concat(footer).runWith(FileIO.toPath(target))

    Await.result(done, 130.seconds)
  } finally {
    system.terminate()
  }

}