import java.util.{List => JList, Map => JMap, Set => JSet}
import com.spotify.docker.client.DockerClient.AttachParameter
import com.spotify.docker.client.messages.{ContainerConfig, ContainerCreation, ContainerInfo, ExecCreation, HostConfig, PortBinding}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
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
  val imageName = "busybox"
  if (dockerClient.searchImages(imageName).asScala.exists(_.name==imageName))
    println(s"Docker image '$imageName' has already been pulled, no need to pull it again.")
  else dockerClient.pull(imageName)

  val container: ContainerCreation = standardTry() {
    // Host ports and container ports are the same for the http and ssh services
    val hostPorts: List[(String, List[PortBinding])] =
      List(80, 22) map { port => (port.toString, List(PortBinding.of("0.0.0.0", port))) }

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
      .image(imageName)
      .cmd("sh", "-c", """while :; do echo "Hello, world"; sleep 1; done""")
      .build

    dockerClient.createContainer(containerConfig)
  }{}.get

  val cc: ContainerInfo = dockerClient.inspectContainer(container.id)
  println(show(cc))

  // Start container
  dockerClient.startContainer(container.id)

  // Execute command inside running container with attached STDOUT and STDERR
  val execCreation: ExecCreation = dockerClient.execCreate(
    container.id,
    Array("sh", "-c", "ls"),
    DockerClient.ExecCreateParam.attachStdout,
    DockerClient.ExecCreateParam.attachStderr
  )
  val output: LogStream = dockerClient.execStart(execCreation.id)
  val execOutput: String = output.readFully
  println(s"Output of command: $execOutput")


  // Resize a container's tty
  dockerClient.resizeTty(container.id, 30, 50) // todo how do we see the change?

  // Attach to a container
  // todo send to stdin and receive from stdout and stderr
  standardTry(errorsAreFatal=false) {
    val stream: String = dockerClient.attachContainer(
      "bigBadContainer",
      AttachParameter.LOGS,
      AttachParameter.STDOUT,
      AttachParameter.STDERR,
      AttachParameter.STREAM
    ).readFully
  }{}

  // Stop a container
  standardTry(errorsAreFatal=false) {
    dockerClient.stopContainer(container.id, 10); // kill if not stopped after 10 seconds
  }{}

  // Close the docker client
  dockerClient.close()
}
