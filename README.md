

```java
@Compose(dockerComposePath = "src/test/resources/docker-compose.yml")
public class SimpleTest {
  private static final Logger log = LoggerFactory.getLogger(SimpleTest.class);

  @Test
  public void host(@Host String host) {
    log.info("host = {}", host);
    assertNotNull(host, "host should not be null.");
  }

  @Test
  public void host(@Host InetAddress address) {
    log.info("host = {}", address);
    assertNotNull(address, "address should not be null.");
  }

  @Test
  public void port(@Port(container = "nginx", port = 80) int port) {
    log.info(internalPort, port);
    assertFalse(0 == port);
  }

  @Test
  public void port(@Port(container = "nginx", port = 80) InetSocketAddress address) {
    log.info("address = {}", address);
    assertNotNull(address);
  }

  @Test
  public void container(@DockerContainer(container = "nginx") Container container) {
    log.info("container = {}", container.getContainerName());
    assertNotNull(container);
  }

  @Test
  public void formatString(@FormatString(container = "nginx", port = 80, format = "https://$HOST:$EXTERNAL_PORT") String uri) {
    log.info("uri = {}", uri);
    assertNotNull(uri);
  }
}
```