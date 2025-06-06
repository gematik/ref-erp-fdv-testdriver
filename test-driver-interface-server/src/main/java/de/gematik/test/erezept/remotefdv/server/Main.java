/*
 * Copyright 2025 gematik GmbH
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.test.erezept.remotefdv.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.webapp.WebAppContext;

@Slf4j
public class Main {

  @SuppressWarnings("java:S4823")
  public static void main(String[] args) throws Exception {
    Server server = new Server();

    // HTTP Configuration
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecureScheme("http");
    httpConfig.setSecurePort(8080);
    httpConfig.setOutputBufferSize(32768);

    // HTTP Connector
    try (ServerConnector httpConnector =
        new ServerConnector(server, new HttpConnectionFactory(httpConfig))) {
      httpConnector.setPort(8080);

      // Add the HTTP connector to the server
      server.setConnectors(new Connector[] {httpConnector});
    }

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setContextPath("/");
    webAppContext.setWar(
        System.getenv().getOrDefault("WEBAPP", "test-driver-interface-server/src/main/webapp"));

    server.setHandler(webAppContext);
    server.start();
    server.join();
  }
}
