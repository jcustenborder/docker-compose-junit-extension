![Maven Central](https://img.shields.io/maven-central/v/com.github.jcustenborder/docker-compose-junit-extension.svg)

# Introduction

This project is an extension of the [Docker Compose Rule](https://github.com/palantir/docker-compose-rule) 
written by Palantir with native support for JUnit 5. This also includes a parameter resolver that supports 
parameters annotated with details about the cluster defined in your docker compose.
 
# Simple Example

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

# Cluster health check

This allows you to specify a health check for the cluster. This allows you to check if all of the 
containers are up and available before starting the tests.


```java
import com.github.jcustenborder.docker.junit5.Compose;
@Compose(dockerComposePath = "src/test/resources/docker-compose.yml", clusterHealthCheck = MyClusterHealthCheck.class)
public class SimpleTest {
  
}
```

```java
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
public class MyClusterHealthCheck implements ClusterHealthCheck {
    @Override
    public SuccessOrFailure isClusterHealthy(Cluster cluster) {
      return new SuccessOrFailure() {
        @Override
        protected Optional<String> optionalFailureMessage() {
          return Optional.empty();
        }
      };
    }
}
```

# Rebuild docker cluster for each test.

```java
@Compose(
    dockerComposePath = "src/test/resources/docker-compose.yml",
    cleanupMode = CleanupMode.AfterEach
)
public class AfterEachTest {
  
}
```