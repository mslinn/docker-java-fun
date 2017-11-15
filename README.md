# docker-java-fun

<img src='https://i1.wp.com/blog.docker.com/wp-content/uploads/2013/06/Docker-logo-011.png?ssl=1' align='right' width='20%'>
<img src='https://seeklogo.com/images/S/spotify-2015-logo-560E071CB7-seeklogo.com.png' align='right' width='15%' style="margin-right: 0.5em;">

Fooling around with Spotify's [docker-client](https://github.com/spotify/docker-client) library.

[![Build Status](https://travis-ci.org/mslinn/docker-java-fun.svg?branch=master)](https://travis-ci.org/mslinn/docker-java-fun)
[![GitHub version](https://badge.fury.io/gh/mslinn%2Fdocker-java-fun.svg)](https://badge.fury.io/gh/mslinn%2Fdocker-java-fun)

Built with Scala 2.12, which requires Java 8+.

## Scaladoc
[Here](http://mslinn.github.io/docker-java-fun/latest/api/index.html).

## Running the Program
Before running the program, control the machine that docker client connects to by setting `DOCKER_HOST`.
It defaults to `localhost`, so there is no need to type this:

    export DOCKER_HOST=localhost

### Running from SBT
Right now this is the only way:

    $ sbt run

### Run Script
B0rked.
Maybe I'll figure out the problem with `sbt-assemply` at some point.

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
