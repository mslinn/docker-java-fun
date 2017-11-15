import com.spotify.docker.client.messages.{ContainerConfig, ContainerCreation, ContainerInfo, ExecCreation, HostConfig, PortBinding, ProgressMessage}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
import scala.collection.JavaConverters._

class Demo(imageName: String) extends DockerClientShow {
  // Create a dockerClient based on DOCKER_CERT_PATH env vars, or if not defined, configuration settings in ~/.docker
  // DOCKER_HOST does not seem to be referenced, contrary to the tutorial
  val dockerClient: DefaultDockerClient = standardTry() {
    DefaultDockerClient.fromEnv.build
  }{}.map { dockerClient =>
    try { println(show(dockerClient, errorsAreFatal=false)) } catch { case _: Exception => }
    dockerClient
  }.get

  // Pull the requested image if it has not already been pulled
  standardTry() {
    if (dockerClient.searchImages(imageName).asScala.exists(_.name==imageName))
      println(s"Docker image '$imageName' has already been pulled, no need to pull it again.")
    else dockerClient.pull(imageName, (_: ProgressMessage) => ()) // Mute the ProgressMessage handler
  }{}.get

  val container: ContainerCreation = standardTry() {
    val portMonster = PortMonster(HostClientPorts(2000, 80, 22))

    val containerConfig = ContainerConfig
      .builder
      .hostConfig(portMonster.hostConfig)
      .exposedPorts(portMonster.exposedPorts)
      .image(imageName /*+ ":latest"*/)
      .cmd("sh", "-c", """while :; do echo "Hello, world"; sleep 1; done""")
      .build

    dockerClient.createContainer(containerConfig)
  }{}.get

  val cc: ContainerInfo = dockerClient.inspectContainer(container.id)
  println(show(cc))

  sys.ShutdownHookThread { // tidy up if Control-C
    dockerClient.stopContainer(container.id, 0)
  }

  // Start container
  dockerClient.startContainer(container.id)

  // Attach to a container
  /*standardTry(errorsAreFatal=false) {
    val stdoutPipe = new PipedOutputStream(new PipedInputStream)
    val stderrPipe = new PipedOutputStream(new PipedInputStream)
    dockerClient.attachContainer(
      container.id,
      AttachParameter.LOGS,
      AttachParameter.STDOUT,
      AttachParameter.STDERR,
      AttachParameter.STREAM
    ).attach(stdoutPipe, stderrPipe, true)
  }{}*/

  // Execute command inside running container with attached STDIN, STDOUT and STDERR
  // fixme This is crap
  Iterator
    .continually(Console.in.readLine)
    .takeWhile(_ => io.StdIn.readLine("\nEnter to end: ").isEmpty)
    .foreach { command =>
      println(command)
      val execCreation: ExecCreation = dockerClient.execCreate(
        container.id,
        command.split(" "),
        DockerClient.ExecCreateParam.attachStdin,
        DockerClient.ExecCreateParam.attachStdout,
        DockerClient.ExecCreateParam.attachStderr
      )
      val output: LogStream = dockerClient.execStart(execCreation.id)
      val execOutput: String = output.readFully
      println(s"Output of $command: $execOutput")
    }


  // Resize a container's tty
  dockerClient.resizeTty(container.id, 30, 50) // todo how do we see the change?

  // Stop a container
  standardTry(errorsAreFatal=false) {
    dockerClient.stopContainer(container.id, 10); // kill if not stopped after 10 seconds
  }{}

  // Close the docker client
  dockerClient.close()
}
