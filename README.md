Experiment to see if we could post-process paradox docs (e.g. from Akka) and
generate one big HTML file out of them, which could be used for PDF and EPUB.

This is not complete, see https://github.com/raboof/paradox-singlepage/issues
for the tasks that are still to be done.

Depends on:
* https://github.com/akka/alpakka/pull/1652
* https://github.com/akka/akka/pull/26759

To run, create a `/tmp/out`

  $ cp -r cp -r /home/aengelen/dev/akka/akka-docs/target/paradox/site/main /tmp/onepage
  $ sbt
  sbt:paradox-singlepage> ~run "/home/aengelen/dev/akka/akka-docs/target/paradox/site/main" "/tmp/onepage/index.html"


