import java.nio.file.Paths
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import AsciiWidgets.asciiTable
import com.spotify.docker.client.DockerClient.{ListContainersParam, ListImagesParam, LogsParam, Signal}
import com.spotify.docker.client.auth.FixedRegistryAuthSupplier
import com.spotify.docker.client.messages.swarm.SecretSpec
import com.spotify.docker.client.messages.{Container, ContainerChange, ContainerExit, ContainerInfo, ContainerStats, Image, ProgressMessage, TopResults}
import com.spotify.docker.client.{DefaultDockerClient, DockerCertificates}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import scala.collection.JavaConverters._
import scala.collection.mutable

/** Other things you can do with the Docker client */
trait MiscDemo { this: Demo =>
  // Use the builder / fluent API
   val dockerClient2: Option[DefaultDockerClient] = standardTry(errorsAreFatal=false) {
     DefaultDockerClient
       .builder
       .apiVersion("1.20")
       .connectionPoolSize(10)
       .connectTimeoutMillis(1000)
       .registryAuthSupplier(new FixedRegistryAuthSupplier)
       .dockerCertificates(new DockerCertificates(Paths.get(sys.props("user.home"), ".docker")))
       .header("test", "blah")
       .readTimeoutMillis(1000)
       .uri("http://localhost")
       .useProxy(false)
       .build
   }{}
  dockerClient2.foreach(_.close) // we don't need it any more

  standardTry(errorsAreFatal=false) {
    val runningContainers: mutable.Seq[Container] = dockerClient.listContainers().asScala
    if (runningContainers.isEmpty) println("No running containers")
    else {
      println("Running Containers")
      runningContainers.foreach(container => println(show(container)))
    }
  }{
  }

  standardTry(errorsAreFatal=false) {
    val allContainers: List[Container] = dockerClient.listContainers(ListContainersParam.allContainers).asScala.toList
    val containersShown: List[String] = allContainers.map { container =>
      val containerShown: String = show(container)
      containerShown
    }
    containersShown foreach println
  }{}

  // Inspect a container
  val containerInfo: ContainerInfo = dockerClient.inspectContainer(container.id)
  println(asciiTable("New Container Info", List(show(containerInfo))))

  // List processes running inside a container
  val topResults: TopResults = dockerClient.topContainer(container.id, "ps_args")
  println(asciiTable("New Container Processes", show(topResults)))

  // Get container logs
  standardTry(errorsAreFatal=false) {
    val logs: String =
      dockerClient.logs("containerID", LogsParam.stdout, LogsParam.stderr).readFully
    println(s"Container logs: $logs")
  }{}

  // Inspect changes on a container's filesystem
  val changes: List[ContainerChange] = dockerClient.inspectContainerChanges(container.id).asScala.toList
  println(s"Container file changes: ${ changes.map(x => s"${ x.kind }: ${ x.path }").mkString("\n") }")

  // Export a container
  standardTry(errorsAreFatal=false) {
    val tarStream: TarArchiveInputStream = new TarArchiveInputStream(dockerClient.exportContainer(container.id))
    val files: List[String] = Iterator
      .continually(tarStream.getNextTarEntry)
      .takeWhile(null!=_)
      .map { _.getName }
      .toList
    println(s"Files stored in the tar are: ${ files.mkString("\n") }")
  }{}

  // Get container stats based on resource usage
  val stats: ContainerStats = dockerClient.stats(container.id)
  println(asciiTable("New Container Info", List(show(containerInfo))))

  // Restart a container
  standardTry(errorsAreFatal=false) {
    dockerClient.restartContainer(container.id) // does this return immediately, or only after the container is running?

    // Can specify a delay before restarting the container
    dockerClient.restartContainer(container.id, 10)
  }{}

  // Kill a container
  standardTry(errorsAreFatal=false) {
    dockerClient.killContainer(container.id) // does this return immediately, or only after the container is running?

    // Does this specify a the *nix signal to send in order to kill the container?
    dockerClient.killContainer(container.id, Signal.SIGINT)
  }{}

  // Rename a container
  dockerClient.renameContainer(container.id, "bigBadContainer")

  // Pause a container
  dockerClient.pauseContainer("bigBadContainer")

  // Unpause a container
  dockerClient.unpauseContainer("bigBadContainer")

  // Wait a container ... does this block until the container exits?
  val exit: ContainerExit = dockerClient.waitContainer("bigBadContainer")
  println(s"container returned status ${ exit.statusCode }")

  // Remove a container
  standardTry(errorsAreFatal=false) {
    dockerClient.removeContainer("bigBadContainer")
  }{}

  // Make a gzip tarball of the home directory on this machine and copy it to the /tmp directory in a container
  dockerClient.copyToContainer(Paths.get(sys.props("user.home")), "bigBadContainer", "/tmp")

  // Create secret
  standardTry(errorsAreFatal=false) {
    val secret: SecretSpec = SecretSpec.builder.name("asecret").data(Base64.getEncoder.encode("asdf".getBytes).toString).build
    dockerClient.createSecret(secret)
  }{}


  // Build image from a Dockerfile
  standardTry(errorsAreFatal=false) {
    val imageIdFromMessage: AtomicReference[String] = new AtomicReference()

    val dockerDirectory = sys.props("user.home") + "/.docker"
    val returnedImageId: String = dockerClient.build(
      Paths.get(dockerDirectory), "test", (message: ProgressMessage) =>
        Option(message.buildImageId).foreach { imageId =>
          imageIdFromMessage.set(imageId)
        }
    )
    // todo Discover how to learn when the build is complete
  }{}

  // List images
  standardTry(errorsAreFatal=false) {
    val quxImages: List[Image] = dockerClient.listImages(ListImagesParam.withLabel("foo", "qux")).asScala.toList
  }{}
}
