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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation is used to format a parameter with components from a docker internalPort. Valid
 * types are String, URI, and URL.
 *
 * @see com.palantir.docker.compose.connection.DockerPort
 * @see String
 * @see java.net.URI
 * @see java.net.URL
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FormatString {
  /**
   * The name of the docker container to retrieve the port for.
   * @return The name of the docker container to retrieve the port for.
   */
  String container();

  /**
   * The internal port to search for.
   * @return The internal port to search for.
   */
  int internalPort();

  /**
   * The format for the docker port.
   * @return The format for the docker port.
   */
  String format();
}
