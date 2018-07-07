/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.docker.junit5;

import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;



@Compose(dockerComposePath = "src/test/resources/docker-compose.yml")
public class SimpleTest {
  private static final Logger log = LoggerFactory.getLogger(SimpleTest.class);

  @Test
  public void host(@Host String host) {
    log.info("host(String) - host = {}", host);
    assertNotNull(host, "host should not be null.");
  }

  @Test
  public void host(@Host InetAddress address) {
    log.info("host(InetAddress) - host = {}", address);
    assertNotNull(address, "address should not be null.");
  }

  @Test
  public void port(@Port(container = "nginx", internalPort = 80) int port) {
    log.info("internalPort(int) - internalPort = {}", port);
    assertFalse(0 == port);
  }

  @Test
  public void port(@Port(container = "nginx", internalPort = 80) Integer port) {
    log.info("internalPort(Integer) - internalPort = {}", port);
    assertFalse(0 == port);
  }

  @Test
  public void port(@Port(container = "nginx", internalPort = 80) InetSocketAddress address) {
    log.info("internalPort(InetSocketAddress) - address = {}", address);
    assertNotNull(address);
  }

  @Test
  public void container(@DockerContainer(container = "nginx") Container container) {
    log.info("container(Container) - container = {}", container.getContainerName());
    assertNotNull(container);
  }

  @Test
  public void formatString(@FormatString(container = "nginx", internalPort = 80, format = "https://$HOST:$EXTERNAL_PORT") String uri) {
    log.info("formatString(String) - uri = {}", uri);
    assertNotNull(uri);
  }

  @Test
  public void formatString(@FormatString(container = "nginx", internalPort = 80, format = "https://$HOST:$EXTERNAL_PORT") URI uri) {
    log.info("formatString(URI) - uri = {}", uri);
    assertNotNull(uri);
  }

  @Test
  public void formatString(@FormatString(container = "nginx", internalPort = 80, format = "https://$HOST:$EXTERNAL_PORT") URL url) {
    log.info("formatString(URL) - url = {}", url);
    assertNotNull(url);
  }

  @Test
  public void cluster(@DockerCluster Cluster cluster) {
    log.info("cluster(Cluster) - cluster = {}", cluster);
    assertNotNull(cluster);
  }
}
