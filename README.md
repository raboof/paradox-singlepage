Experiment to see if we could post-process paradox docs (e.g. from Akka) and
generate one big HTML file out of them, which could be used for PDF and EPUB.

This is not complete, see https://github.com/raboof/paradox-singlepage/issues
for the tasks that are still to be done.

Depends on https://github.com/akka/akka/pull/26759

To run:

  $ git clone git@github.com:akka/akka
  $ cd akka
  $ git checkout cleanerParadoxOutput
  $ sbt paradox
  $ cd ..
  $ git clone git@github.com:raboof/paradox-singlepage
  $ cd paradox-singlepage
  $ cp -r cp -r /home/aengelen/dev/akka/akka-docs/target/paradox/site/main /tmp/onepage
  $ sbt
  sbt:paradox-singlepage> ~run "/home/aengelen/dev/akka/akka-docs/target/paradox/site/main" "/tmp/onepage/index.html"


