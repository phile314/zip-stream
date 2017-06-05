# zip-stream

A simple zip streaming library for scalaz-stream. Especially useful for creating zip files on demand,
e.g. large HTTP downloads.

## How to use

The library takes a ``Process[M, ZipEntry[M]]`` as input and produces a ``Process[M, ByteVector]``.

For a small example, see ``src/test/scala/ch/fhnw/scala/zip/Main.scala``.
