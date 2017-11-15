import com.spotify.docker.client.messages.{HostConfig, PortBinding}
import java.util.{List => JList, Map => JMap, Set => JSet}
import scala.collection.JavaConverters._

object HostClientPorts {
  /** Generates mapping between host and client ports
    * @param hostPorts Varargs list of ports on host to connect
    * @param offset constant offset between host ports and client ports */
  def apply(offset: Int, hostPorts: Int*): List[HostClientPorts] =
    hostPorts.toList.map(hostPort => HostClientPorts(hostPort, hostPort + offset))
}

case class HostClientPorts(hostPort: Int, clientPort: Int)


case class PortMonster(hostClientPorts: List[HostClientPorts]) {
  // Host port numbers and container port numbers for similar services differ by 2000
  protected val portMappingTuples: List[(String, List[PortBinding])] =
    HostClientPorts(2000, 80, 22)
      .map {
        case HostClientPorts(hostPort, clientPort) =>
          (clientPort.toString, List(PortBinding.of("0.0.0.0", hostPort)))
      }

  // Bind container port 443 to an automatically allocated available host port.
  protected val sslTuple: (String, List[PortBinding]) = ("443", List(PortBinding.randomPort("0.0.0.0")))

  protected val portBindings: JMap[String, JList[PortBinding]] =
    (sslTuple :: portMappingTuples)
      .toMap
      .map { case (key, value) => (key, value.asJava) }
    .asJava

  val hostConfig: HostConfig = HostConfig.builder.portBindings(portBindings).build
  val exposedPorts: JSet[String] = portMappingTuples.map(_._1).toSet.asJava
}
