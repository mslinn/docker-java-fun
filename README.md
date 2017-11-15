# docker-java-fun

Fooling around with Spotify's [docker-client]() library.

[![Build Status](https://travis-ci.org/mslinn/docker-java-fun.svg?branch=master)](https://travis-ci.org/mslinn/docker-java-fun)
[![GitHub version](https://badge.fury.io/gh/mslinn%2Fdocker-java-fun.svg)](https://badge.fury.io/gh/mslinn%2Fdocker-java-fun)

Built with Scala 2.12, which requires Java 8+.

## Scaladoc
[Here](http://mslinn.github.io/docker-java-fun/latest/api/index.html).

## Running the Program
The `bin/run` Bash script assembles this project into a fat jar and runs it.
Sample usage, which runs the `Main` entry point in `src/main/scala/Main.scala:

```
$ bin/run Main
```

The `-j` option forces a rebuild of the fat jar. 
Use it after modifying the source code.

```
$ bin/run -j Main
```
