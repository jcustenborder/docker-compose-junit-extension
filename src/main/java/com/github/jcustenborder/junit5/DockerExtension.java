/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.junit5;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.configuration.ShutdownStrategy;
import com.palantir.docker.compose.connection.Cluster;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.ContainerCache;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.DockerPort;
import com.palantir.docker.compose.connection.ImmutableCluster;
import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import com.palantir.docker.compose.connection.waiting.ClusterWait;
import com.palantir.docker.compose.execution.DefaultDockerCompose;
import com.palantir.docker.compose.execution.Docker;
import com.palantir.docker.compose.execution.DockerCompose;
import com.palantir.docker.compose.execution.DockerComposeExecutable;
import com.palantir.docker.compose.execution.DockerExecutable;
import com.palantir.docker.compose.execution.RetryingDockerCompose;
import com.palantir.docker.compose.logging.FileLogCollector;
import com.palantir.docker.compose.logging.LogCollector;
import org.joda.time.Duration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class DockerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
  private static final Logger log = LoggerFactory.getLogger(DockerExtension.class);

  private static Compose findDockerComposeAnnotation(ExtensionContext extensionContext) {
    Class<?> testClass = extensionContext.getTestClass().get();

    log.trace("Looking for Compose extension on {}", testClass.getName());

    Compose compose = testClass.getAnnotation(Compose.class);
    Preconditions.checkNotNull(compose, "Compose annotation not found on %s", testClass.getName());
    Preconditions.checkState(!Strings.isNullOrEmpty(compose.dockerComposePath()), "compose.dockerComposePath() cannot be null or empty.");
    Preconditions.checkState(!Strings.isNullOrEmpty(compose.logRootPath()), "compose.logRootPath() cannot be null or empty.");
    return compose;
  }

  private static ExtensionContext.Namespace namespace(ExtensionContext extensionContext) {
    Class<?> testClass = extensionContext.getTestClass().get();
    ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(testClass.getName(), "docker", "compose");
    log.trace("Created namespace {}", namespace);
    return namespace;
  }

  static <T> T getOrBuild(Class<T> cls, ExtensionContext.Store store, Compose compose, Supplier<T> supplier) {
    final String key = cls.getSimpleName();
    T result = store.get(key, cls);

    if (null == result) {
      result = supplier.get();
      store.put(key, result);
    }

    return result;
  }

  private DockerMachine dockerMachine(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(DockerMachine.class, store, compose, () -> DockerMachine.localMachine().build());
  }

  private DockerComposeExecutable dockerComposeExecutable(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(DockerComposeExecutable.class, store, compose, () -> {
      DockerMachine dockerMachine = dockerMachine(store, compose);
      return DockerComposeExecutable.builder()
          .dockerConfiguration(dockerMachine)
          .dockerComposeFiles(DockerComposeFiles.from(compose.dockerComposePath()))
          .build();
    });
  }


  private DockerCompose dockerCompose(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(DockerCompose.class, store, compose, () -> {
      DockerMachine dockerMachine = dockerMachine(store, compose);
      DockerComposeExecutable dockerComposeExecutable = dockerComposeExecutable(store, compose);
      DockerCompose dockerCompose = new DefaultDockerCompose(dockerComposeExecutable, dockerMachine);
      return new RetryingDockerCompose(compose.retryAttempts(), dockerCompose);
    });
  }

  private LogCollector logCollector(Optional<Class<?>> testClass, ExtensionContext.Store store, Compose compose) {
    return getOrBuild(LogCollector.class, store, compose, () -> {
      File logPathRoot = new File(compose.logRootPath());
      File testClassLogPath = new File(logPathRoot, testClass.get().getName());
      return FileLogCollector.fromPath(testClassLogPath.getAbsolutePath());
    });
  }


  private Cluster cluster(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(Cluster.class, store, compose, () -> {
      DockerMachine machine = dockerMachine(store, compose);
      Docker docker = docker(store, compose);
      DockerCompose dockerCompose = dockerCompose(store, compose);
      return ImmutableCluster.builder()
          .ip(machine.getIp())
          .containerCache(new ContainerCache(docker, dockerCompose))
          .build();
    });
  }

  private DockerExecutable dockerExecutable(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(DockerExecutable.class, store, compose, () -> {
      DockerMachine machine = dockerMachine(store, compose);
      return DockerExecutable.builder()
          .dockerConfiguration(machine)
          .build();
    });
  }

  private Docker docker(ExtensionContext.Store store, Compose compose) {
    return getOrBuild(Docker.class, store, compose, () -> {
      DockerExecutable dockerExecutable = dockerExecutable(store, compose);
      return new Docker(dockerExecutable);
    });
  }

  ClusterWait healthCheck(Compose compose) throws IllegalAccessException, InstantiationException {
    ClusterHealthCheck result;
    try {
      result = compose.clusterHealthCheck().newInstance();
    } catch (Exception ex) {
      throw new IllegalStateException(
          String.format("Could not create instance of health check '%s'", compose.clusterHealthCheck().getName()),
          ex
      );
    }

    return new ClusterWait(result, Duration.standardSeconds(compose.clusterHealthCheckTimeout()));
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    final Optional<Class<?>> testClass = extensionContext.getTestClass();
    final ExtensionContext.Namespace namespace = namespace(extensionContext);
    final Compose compose = findDockerComposeAnnotation(extensionContext);
    ExtensionContext.Store store = extensionContext.getStore(namespace);

    DockerCompose dockerCompose = dockerCompose(store, compose);

    LogCollector logCollector = logCollector(testClass, store, compose);
    dockerCompose.build();
    dockerCompose.up();

    logCollector.startCollecting(dockerCompose);

    Cluster cluster = cluster(store, compose);
    log.debug("Waiting for services");


    final List<ClusterWait> clusterWaits = new ArrayList<ClusterWait>();
    clusterWaits.add(
        new ClusterWait(ClusterHealthCheck.nativeHealthChecks(), Duration.standardSeconds(compose.clusterHealthCheckTimeout()))
    );
    clusterWaits.add(healthCheck(compose));
    clusterWaits.forEach(clusterWait -> clusterWait.waitUntilReady(cluster));
    log.debug("docker-compose cluster started");
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    final Optional<Class<?>> testClass = extensionContext.getTestClass();
    ExtensionContext.Namespace namespace = namespace(extensionContext);
    ExtensionContext.Store store = extensionContext.getStore(namespace);
    Compose compose = findDockerComposeAnnotation(extensionContext);

    DockerCompose dockerCompose = dockerCompose(store, compose);
    Docker docker = docker(store, compose);
    LogCollector collector = logCollector(testClass, store, compose);
    collector.stopCollecting();
    try {
      ShutdownStrategy.KILL_DOWN.shutdown(dockerCompose, docker);

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Error cleaning up docker compose cluster", e);
    }
  }


  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return
        parameterContext.isAnnotated(FormatString.class) ||
            parameterContext.isAnnotated(DockerContainer.class) ||
            parameterContext.isAnnotated(Port.class) ||
            parameterContext.isAnnotated(Host.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    ExtensionContext.Namespace namespace = namespace(extensionContext);
    ExtensionContext.Store store = extensionContext.getStore(namespace);
    final Compose compose = findDockerComposeAnnotation(extensionContext);
    final Class<?> parameterType = parameterContext.getParameter().getType();

    DockerCompose dockerCompose = dockerCompose(store, compose);
    DockerMachine dockerMachine = dockerMachine(store, compose);
    Cluster cluster = cluster(store, compose);

    final Host host = parameterContext.getParameter().getAnnotation(Host.class);

    if (null != host) {
      return dockerHost(parameterType, dockerMachine, host);
    }

    final Port port = parameterContext.getParameter().getAnnotation(Port.class);

    if (null != port) {
      return dockerPort(parameterType, cluster, port);
    }

    final DockerContainer container = parameterContext.getParameter().getAnnotation(DockerContainer.class);

    if (null != container) {
      return dockerContainer(parameterType, cluster, container);
    }

    final FormatString formatString = parameterContext.getParameter().getAnnotation(FormatString.class);

    if (null != formatString) {
      return dockerFormatString(parameterType, cluster, formatString);
    }

    return null;
  }

  private Object dockerHost(Class<?> parameterType, DockerMachine dockerMachine, Host host) {
    if (String.class == parameterType) {
      return dockerMachine.getIp();
    } else if (InetAddress.class.isAssignableFrom(parameterType)) {
      try {
        return InetAddress.getByName(dockerMachine.getIp());
      } catch (UnknownHostException e) {
        throw new ParameterResolutionException("Could not resolve ip", e);
      }
    } else {
      return null;
    }
  }

  private Object dockerPort(Class<?> parameterContext, Cluster cluster, Port port) {
    Container container = cluster.container(port.container());
    Preconditions.checkNotNull(container, "Could not find container '%s'", port.container());
    DockerPort dockerPort = container.port(port.port());
    Preconditions.checkNotNull(container, "Could not find port '%s' for container '%s'", port.port(), port.container());

    if (InetSocketAddress.class.isAssignableFrom(parameterContext)) {
      return InetSocketAddress.createUnresolved(
          cluster.ip(),
          dockerPort.getExternalPort()
      );
    } else if (Integer.class.equals(parameterContext)) {
      return dockerPort.getExternalPort();
    } else if (int.class.equals(parameterContext)) {
      return dockerPort.getExternalPort();
    } else {
      return null;
    }
  }

  private Object dockerContainer(Class<?> parameterContext, Cluster cluster, DockerContainer dockerContainer) {
    Container container = cluster.container(dockerContainer.container());
    Preconditions.checkNotNull(container, "Could not find container '%s'", dockerContainer.container());
    return container;
  }

  private Object dockerFormatString(Class<?> parameterContext, Cluster cluster, FormatString formatString) {
    Container container = cluster.container(formatString.container());
    Preconditions.checkNotNull(container, "Could not find container '%s'", formatString.container());
    DockerPort port = container.port(formatString.port());
    Preconditions.checkNotNull(container, "Could not find port '%s' for container '%s'", formatString.port(), formatString.container());
    return port.inFormat(formatString.format());
  }
}
