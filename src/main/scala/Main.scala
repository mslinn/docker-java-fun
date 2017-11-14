import java.nio.file.Paths
import java.util
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import com.spotify.docker.client.DockerClient.{AttachParameter, ListContainersParam, ListImagesParam, LogsParam, Signal}
import com.spotify.docker.client.messages.swarm.SecretSpec
import com.spotify.docker.client.messages.{Container, ContainerChange, ContainerConfig, ContainerCreation, ContainerExit, ContainerInfo, ContainerStats, Image, ProgressMessage, TopResults}
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import scala.collection.JavaConverters._
import scala.collection.mutable

/** Typing along with https://github.com/spotify/docker-dockerClient/blob/master/docs/user_manual.md */
object Main extends App {
  // Create a dockerClient based on DOCKER_HOST and DOCKER_CERT_PATH env vars
  import com.spotify.docker.client.DefaultDockerClient
  val dockerClient = DefaultDockerClient.fromEnv.build
  AsciiWidgets.asciiTable("Docker Client", List(show(dockerClient)))

  protected def show(dockerClient: DockerClient): String =
    standardTry {
      s"""DockerClient for ${ dockerClient.getHost }, info: ${ dockerClient.info }
         |${ dockerClient.listConfigs.asScala.mkString(", ") }
         |""".stripMargin
      } {
        println("Is docker running?")
      }

    // ...or use the builder
    // import com.spotify.docker.dockerClient.DockerClient
    // val docker = DefaultDockerClient.builder.build // Set various options

    val runningContainers: mutable.Seq[Container] = dockerClient.listContainers().asScala
    AsciiWidgets.asciiTable("Running Containers", runningContainers.toList.map(show))

    val allContainers: List[Container] = dockerClient.listContainers(ListContainersParam.allContainers).asScala.toList
    AsciiWidgets.asciiTable("All Containers", allContainers.map(show))

    protected def show(container: Container): String =
    s"""Container #${ container.id }, created ${ container.created } from image ${ container.image }
       |  command: ${ container.command }
       |  imageId: ${ container.imageId }
       |  labels:  ${ container.labels.asScala.mkString(", ") }
       |  mounts:  ${ container.mounts.asScala.mkString(", ") }
       |  names: ${ container.names }
       |  networkSettings: ${ container.networkSettings}
       |  ports: ${ container.ports }
       |  portsAsString: ${ container.portsAsString }
       |  sizeRootFs: ${ container.sizeRootFs }
       |  sizeRw: ${ container.sizeRw }
       |  state: ${ container.state }
       |  status: ${ container.status }
       |""".stripMargin

  val container: ContainerCreation = dockerClient.createContainer(ContainerConfig.builder.build)

  // Inspect a container
  val containerInfo: ContainerInfo = dockerClient.inspectContainer(container.id)
  AsciiWidgets.asciiTable("New Container Info", List(show(containerInfo)))

  def show(containerInfo: ContainerInfo) =
    s"""Container:
       |  appArmorProfile = ${ containerInfo.appArmorProfile }
       |  args            = ${ containerInfo.args.asScala.mkString(", ") }
       |  config          = ${ containerInfo.config }
       |  created         = ${ containerInfo.created }
       |  driver          = ${ containerInfo.driver }
       |  execDriver      = ${ containerInfo.execDriver }
       |  execIds         = ${ containerInfo.execIds.asScala.mkString(", ") }
       |  hostConfig      = ${ containerInfo.hostConfig }
       |  hostnamePath    = ${ containerInfo.hostnamePath }
       |  id              = ${ containerInfo.id }
       |  image           = ${ containerInfo.image }
       |  logPath         = ${ containerInfo.logPath }
       |  mountLabel      = ${ containerInfo.mountLabel }
       |  mounts          = ${ containerInfo.mounts.asScala.mkString(", ") }
       |  name            = ${ containerInfo.name }
       |  networkSettings = ${ containerInfo.networkSettings }
       |  node            = ${ containerInfo.node }
       |  path            = ${ containerInfo.path }
       |  processLabel    = ${ containerInfo.processLabel }
       |  resolvConfPath  = ${ containerInfo.resolvConfPath }
       |  restartCount    = ${ containerInfo.restartCount }
       |  state           = ${ containerInfo.state }
       |""".stripMargin

  // List processes running inside a container
  val topResults: TopResults = dockerClient.topContainer(container.id, "ps_args")
  AsciiWidgets.asciiTable("New Container Processes", show(topResults))

  def show(topResults: TopResults): List[String] = {
    (topResults.titles.asScala.toList zip topResults.processes.asScala.toList).map {
      case (title, processes) => s"$title: ${ processes.asScala.mkString(", ") }"
    }
  }

  // Get container logs
  val logs: String =
    standardTry {
      dockerClient.logs("containerID", LogsParam.stdout, LogsParam.stderr).readFully
    }{}
  println(s"Container logs: $logs")

  // Inspect changes on a container's filesystem
  val changes: List[ContainerChange] = dockerClient.inspectContainerChanges(container.id).asScala.toList
  println(s"Container file changes: ${ changes.map(x => s"${ x.kind }: ${ x.path }").mkString("\n") }")

  // Export a container
  standardTry {
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
  AsciiWidgets.asciiTable("New Container Info", List(show(containerInfo)))

  def show(containerStats: ContainerStats) =
    s"""ContainerStats:
       |  blockIoStats = ${ containerStats.blockIoStats }
       |  cpuStats     = ${ containerStats.cpuStats }
       |  memoryStats  = ${ containerStats.memoryStats }
       |  network      = ${ containerStats.network }
       |  precpuStats  = ${ containerStats.precpuStats }
       |  read         = ${ containerStats.read }
       |""".stripMargin

  // Resize a container's tty
  dockerClient.resizeTty(container.id, 30, 50) // todo how do we see the change?

  // Start a container
  standardTry {
    dockerClient.startContainer(container.id)
  }{}

  // Stop a container
  standardTry {
    dockerClient.stopContainer(container.id, 10); // kill if not stopped after 10 seconds
  }{}

  // Restart a container
  standardTry {
    dockerClient.restartContainer(container.id) // does this return immediately, or only after the container is running?

    // Can specify a delay before restarting the container
    dockerClient.restartContainer(container.id, 10)
  }{}

  // Kill a container
  standardTry {
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

  // Attach to a container
  // todo send to stdin and receive from stdout and stderr
  val stream: String = standardTry {
    dockerClient.attachContainer(
      "bigBadContainer",
      AttachParameter.LOGS,
      AttachParameter.STDOUT,
      AttachParameter.STDERR,
      AttachParameter.STREAM
    ).readFully
  }{}

  // Wait a container ... does this block until the container exits?
  val exit: ContainerExit = dockerClient.waitContainer("bigBadContainer")
  println(s"container returned status ${ exit.statusCode }")

  // Remove a container
  standardTry {
    dockerClient.removeContainer("bigBadContainer")
  }{}

  // Make a gzip tarball of the home directory on this machine and copy it to the /tmp directory in a container
  dockerClient.copyToContainer(Paths.get(sys.props("user.home")), "bigBadContainer", "/tmp")

  // Create secret
  standardTry {
    val secret: SecretSpec = SecretSpec.builder.name("asecret").data(Base64.getEncoder.encode("asdf".getBytes).toString).build
    dockerClient.createSecret(secret)
  }{}

  // List images
  standardTry {
    val quxImages: List[Image] = dockerClient.listImages(ListImagesParam.withLabel("foo", "qux")).asScala.toList
  }{}

  // Build image from a Dockerfile
  standardTry {
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


  protected def standardTry[T](block: => T)(lastThing: => Any): T =
    try {
      block
    } catch {
      case e: Exception =>
        System.err.println(s"Error: ${ e.getMessage }")
        lastThing
        System.exit(-1).asInstanceOf[Nothing]
    }
}
