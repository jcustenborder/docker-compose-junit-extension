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

import com.palantir.docker.compose.connection.waiting.ClusterHealthCheck;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation is used to mark a test class with tests using a docker compose file.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerExtension.class)
public @interface Compose {
  /**
   * Relative path to the docker-compose.yml file to use.
   *
   * @return Relative path to the docker-compose.yml file to use.
   */
  String dockerComposePath();

  /**
   * Relative path to write the logs to.
   *
   * @return Relative path to write the logs to.
   */
  String logRootPath() default "target/docker-compose";

  /**
   * Health check class used to check the health of the cluster.
   *
   * @return Health check class used to check the health of the cluster.
   */
  Class<? extends ClusterHealthCheck> clusterHealthCheck() default NullClusterHealthCheck.class;

  /**
   * The timeout it seconds to wait until the cluster is healthy.
   *
   * @return The timeout it seconds to wait until the cluster is healthy.
   */
  int clusterHealthCheckTimeout() default 120;

  /**
   * The number of tries before failing the test and cleaning up.
   *
   * @return The number of tries before failing the test and cleaning up.
   */
  int retryAttempts() default 2;
}
