import java.util.Date
import AsciiWidgets.asciiTable
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.swarm.Config
import com.spotify.docker.client.messages.{Container, ContainerConfig, ContainerInfo, ContainerMount, ContainerStats, HostConfig, Info, NetworkSettings, TopResults}
import scala.collection.JavaConverters._

/** Formatted output for the demo */
object DockerClientShow {
  implicit class RichConfig(config: Config) {
    override def toString: String =
      s"${ config.configSpec } ${ config.createdAt } ${ config.id } ${ config.updatedAt } ${ config.version }"
  }

  implicit class RichContainerConfig(config: ContainerConfig) {
    override def toString: String =
      s"""Container
         |  Cmd:              ${ config.cmd }<br>
         |  Domain name:      ${ config.domainname }<br>
         |  Entry points:     ${ config.entrypoint.asScala.mkString(", ") }<br>
         |  Env:              ${ config.env.asScala.mkString(", ") }<br>
         |  Exposed ports:    ${ config.exposedPorts }<br>
         |  Host config:      ${ RichHostConfig(config.hostConfig) }<br>
         |  Host name:        ${ config.hostname }<br>
         |  Image:            ${ config.image }
         |  Labels:           ${ config.labels }
         |  macAddress:       ${ config.macAddress }
         |  Network disabled: ${ config.networkDisabled }
         |  Network config:   ${ config.networkingConfig }
         |  Port specs:       ${ config.portSpecs.asScala.mkString(", ") }
         |  User:             ${ config.user }
         |  Volume names:     ${ config.volumeNames.asScala.mkString(", ") }
         |  Working dir:      ${ config.workingDir }
         |""".stripMargin
  }

  implicit class RichContainerMount(mount: ContainerMount) {
    override def toString: String =
      s"""type:        ${ mount.`type` }<br>
         |name:        ${ Option(mount.name).getOrElse("") }<br>
         |driver:      ${ mount.driver }<br>
         |mode:        ${ mount.mode }<br>
         |name:        ${ mount.name }<br>
         |propagation: ${ mount.propagation }<br>
         |rw:          ${ mount.rw }<br>
         |source:      ${ mount.source }
         |""".stripMargin
  }

  implicit class RichHostConfig(config: HostConfig) {
    override def toString: String =
      s"""Auto remove:      ${ config.autoRemove }
         |Binds:            ${ Option(config.binds).map(_.asScala.mkString(", ")).getOrElse("") }
         |CPU period:       ${ Option(config.cpuPeriod).getOrElse("") }
         |CPU quota:        ${ Option(config.cpuQuota).getOrElse("") }
         |CPUs:             ${ Option(config.cpusetCpus).getOrElse("") }
         |CPU mems:         ${ Option(config.cpusetMems).getOrElse("") }
         |CPU shares:       ${ Option(config.cpuShares).getOrElse("") }
         |IPC mode:         ${ Option(config.ipcMode).getOrElse("") }
         |Log config:       ${ Option(config.logConfig).getOrElse("") }
         |Memory:           ${ Option(config.memory).getOrElse("") }
         |  Reservation:      ${ Option(config.memoryReservation).getOrElse("") }
         |  Swap:             ${ Option(config.memorySwap).getOrElse("") }
         |Network mode:     ${ Option(config.networkMode).getOrElse("") }
         |OOM kill disable: ${ Option(config.oomKillDisable).getOrElse("") }
         |PortBindings:     ${ Option(config.portBindings).getOrElse("") }
         |ULimits:          ${ Option(config.ulimits).getOrElse("") }
         |""".stripMargin
  }

  implicit class RichInfo(info: Info) {
    override def toString: String =
      s"""Architecture: ${ info.architecture }
         |Containers:   ${ info.containers }
         |  Paused:       ${ info.containersPaused }
         |  Running:      ${ info.containersRunning }
         |  Stopped:      ${ info.containersStopped }
         |CPUs:         ${ info.cpus }
         |Root dir:     ${ info.dockerRootDir }
         |Id:           ${ info.id }
         |Images:       ${ info.images }
         |initPath:     ${ Option(info.initPath).mkString }
         |Memory:       ${ info.kernelMemory }
         |Version:      ${ info.kernelVersion }
         |""".stripMargin
  }

  /*implicit class NetworkingConfig(config: NetworkingConfig) {
    override def toString: String =
      s"""Architecture: ${ config.config.config. }
         |Version:      ${ config.kernelVersion }
         |""".stripMargin
  }*/

  implicit class RichNetworkSettings(settings: NetworkSettings) {
    override def toString: String =
      s"""IP address:  ${ settings.ipAddress }<br>
         |Ports:       ${ settings.ports.asScala.mkString(", ") }<br>
         |Sandbox id:  ${ settings.sandboxId }<br>
         |Sandbox key: ${ settings.sandboxKey }
         |""".stripMargin
  }
}

trait DockerClientShow {
  import DockerClientShow._

  protected def last6charsOf(string: String): String = string.substring(string.length-6)

  protected def show(container: Container): String =
    asciiTable(
      s"Container #${ last6charsOf(container.id) }, created ${ new Date(container.created) } from image '${ container.image }'",
      List(
        List("Command",               container.command),
        List("Image id",              container.imageId),
        List("Labels",                container.labels.asScala.mkString(", ")),
        List("Mounts",                container.mounts.asScala.map(RichContainerMount).mkString(", ")),
        List("Names",                 container.names.asScala.mkString(", ")),
        List("Network settings",      RichNetworkSettings(container.networkSettings).toString),
        List("Ports",                 container.ports.asScala.mkString(", ")),
        List("Ports as strings",      container.portsAsString),
        List("Root file system size", Option(container.sizeRootFs).getOrElse(-1L).toString),
        List("Size RW",               Option(container.sizeRw).getOrElse(-1L).toString),
        List("State",                 container.state),
        List("Status",                container.status)
      ): _*
    )

  def show(containerInfo: ContainerInfo): String =
    s"""Container:
       |  appArmorProfile = ${ containerInfo.appArmorProfile }
       |  args            = ${ containerInfo.args.asScala.mkString(", ") }
       |  config          = ${ RichContainerConfig(containerInfo.config) }
       |  created         = ${ containerInfo.created }
       |  driver          = ${ containerInfo.driver }
       |  execDriver      = ${ containerInfo.execDriver }
       |  execIds         = ${ Option(containerInfo.execIds).map(_.asScala.mkString(", ")).getOrElse("") }
       |  hostConfig      = ${ RichHostConfig(containerInfo.hostConfig) }
       |  hostnamePath    = ${ containerInfo.hostnamePath }
       |  id              = ${ containerInfo.id }
       |  image           = ${ containerInfo.image }
       |  logPath         = ${ containerInfo.logPath }
       |  mountLabel      = ${ containerInfo.mountLabel }
       |  mounts          = ${ containerInfo.mounts.asScala.mkString(", ") }
       |  name            = ${ containerInfo.name }
       |  networkSettings = ${ RichNetworkSettings(containerInfo.networkSettings) }
       |  node            = ${ containerInfo.node }
       |  path            = ${ containerInfo.path }
       |  processLabel    = ${ containerInfo.processLabel }
       |  resolvConfPath  = ${ containerInfo.resolvConfPath }
       |  restartCount    = ${ containerInfo.restartCount }
       |  state           = ${ containerInfo.state }
       |""".stripMargin

  def show(containerStats: ContainerStats): String =
    s"""ContainerStats:
       |  blockIoStats = ${ containerStats.blockIoStats }
       |  cpuStats     = ${ containerStats.cpuStats }
       |  memoryStats  = ${ containerStats.memoryStats }
       |  network      = ${ containerStats.network }
       |  precpuStats  = ${ containerStats.precpuStats }
       |  read         = ${ containerStats.read }
       |""".stripMargin

  protected def show(dockerClient: DockerClient, errorsAreFatal: Boolean = true): String = {
    val result: Option[String] = standardTry(errorsAreFatal) {
      val line1 = s"DockerClient for ${ dockerClient.getHost }"
      val info = RichInfo(dockerClient.info)
      val configs: List[Config] = try {
        dockerClient.listConfigs.asScala.toList
      } catch {
        case _: Exception =>
          if (errorsAreFatal) System.exit(0).asInstanceOf[Nothing] else Nil
      }
      if (configs.isEmpty) line1 + info + "Docker client has no configuration information"
      else asciiTable(line1 + info, configs.map(_.toString))
    } {
      System.err.println(if (errorsAreFatal) "Is docker running?" else "Error ignored.")
    }
    result.getOrElse("")
  }

  def show(topResults: TopResults): List[String] =
    (topResults.titles.asScala.toList zip topResults.processes.asScala.toList).map {
      case (title, processes) => s"$title: ${ processes.asScala.mkString(", ") }"
    }


  protected def standardTry[T](errorsAreFatal: Boolean = true)
                              (block: => T)
                              (lastThing: => Any): Option[T] =
    try {
      Some(block)
    } catch {
      case e: Exception =>
        System.err.println(s"Error: ${ e.getMessage }")
        lastThing
        if (errorsAreFatal) System.exit(-1)
        None
    }
}
