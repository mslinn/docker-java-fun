import java.util.{List => JList, Map => JMap, Set => JSet}
import com.spotify.docker.client.DockerClient.AttachParameter
import com.spotify.docker.client.messages.{ContainerConfig, ContainerCreation, ContainerInfo, ExecCreation, HostConfig, PortBinding, ProgressMessage}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream, ProgressHandler}
import scala.collection.JavaConverters._

class Demo extends DockerClientShow {
  // Create a dockerClient based on DOCKER_CERT_PATH env vars, or if not defined, configuration settings in ~/.docker
  // DOCKER_HOST does not seem to be referenced, contrary to the tutorial
  val dockerClient: DefaultDockerClient = standardTry() {
    DefaultDockerClient.fromEnv.build
  }{}.map { dockerClient =>
    try { println(show(dockerClient, errorsAreFatal=false)) } catch { case _: Exception => }
    dockerClient
  }.get

  // Pull an image if it has not already been pulled
  val imageName = "ubuntu"
  if (dockerClient.searchImages(imageName).asScala.exists(_.name==imageName))
    println(s"Docker image '$imageName' has already been pulled, no need to pull it again.")
  else dockerClient.pull(imageName, (_: ProgressMessage) => ()) // suppress lots of output

  val container: ContainerCreation = standardTry() {
    // Host ports and container ports are the same for the http and ssh services
    val hostPorts: List[(String, List[PortBinding])] =
      List(2080 -> 80, 2022 -> 22)
        .map { case (hostPort, clientPort) => (clientPort.toString, List(PortBinding.of("0.0.0.0", hostPort))) }

    // Bind container port 443 to an automatically allocated available host port.
    val sslTuple: (String, List[PortBinding]) = ("443", List(PortBinding.randomPort("0.0.0.0")))

    val portBindings: JMap[String, JList[PortBinding]] =
      (sslTuple :: hostPorts)
        .toMap
        .map { case (key, value) => (key, value.asJava) }
      .asJava

    val hostConfig = HostConfig.builder.portBindings(portBindings).build
    val exposedPorts: JSet[String] = hostPorts.map(_._1).toSet.asJava

    val containerConfig = ContainerConfig
      .builder
      .hostConfig(hostConfig)
      .exposedPorts(exposedPorts)
      .image(imageName/* + ":latest"*/)
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
  // todo send to stdin and receive from stdout and stderr
  standardTry(errorsAreFatal=false) {
    val stream: String = dockerClient.attachContainer(
      container.id,
      AttachParameter.LOGS,
      AttachParameter.STDOUT,
      AttachParameter.STDERR,
      AttachParameter.STREAM
    ).readFully
    println(stream)
  }{}

  // Execute command inside running container with attached STDIN, STDOUT and STDERR
  val execCreation: ExecCreation = dockerClient.execCreate(
    container.id,
    Array("bash", "-c", "ls"),
    DockerClient.ExecCreateParam.attachStdin,
    DockerClient.ExecCreateParam.attachStdout,
    DockerClient.ExecCreateParam.attachStderr
  )
  val output: LogStream = dockerClient.execStart(execCreation.id)
  val execOutput: String = output.readFully
  println(s"Output of command: $execOutput")


  // Resize a container's tty
  dockerClient.resizeTty(container.id, 30, 50) // todo how do we see the change?

  // Stop a container
  standardTry(errorsAreFatal=false) {
    dockerClient.stopContainer(container.id, 10); // kill if not stopped after 10 seconds
  }{}

  // Close the docker client
  dockerClient.close()
}
