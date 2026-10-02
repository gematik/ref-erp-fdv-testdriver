/*
 * Copyright 2026 gematik GmbH
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

package de.gematik.test.erezept.remotefdv.client;

import static java.text.MessageFormat.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.bbriccs.utils.ResourceLoader;
import de.gematik.erezept.remotefdv.api.model.Error;
import de.gematik.test.erezept.remotefdv.client.requests.FdVRequests;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.yaml.snakeyaml.error.MissingEnvironmentVariableException;

@Slf4j
public class RemoteFdVClient {
  private final HttpRequestInfo requestInfo;
  private final HttpClient httpClient;

  private RemoteFdVClient(HttpRequestInfo requestInfo, HttpClient httpClient) {
    this.requestInfo = requestInfo;
    this.httpClient = httpClient;
  }

  public static Builder builder() {
    return new Builder();
  }

  public <T> FdVResponse<T> sendRequest(FdVRequests<T> request) {
    request.finalizeRequest(requestInfo);
    val fullUrl =
        new StringBuilder(
            format(
                "{0}/erp/testdriver/api/v1/{1}", requestInfo.getHost(), requestInfo.getResource()));
    if (requestInfo.getResourceId() != null) {
      fullUrl.append("/").append(requestInfo.getResourceId());
    }
    val query = new StringBuilder();
    if (!requestInfo.getQuery().isEmpty()) {
      for (Map.Entry<String, String> entry : requestInfo.getQuery().entrySet()) {
        if (!query.isEmpty()) query.append("&");
        query.append(URLEncoder.encode(entry.getKey()));
        query.append("=");
        query.append(URLEncoder.encode(entry.getValue()));
      }
      fullUrl.append("?").append(query);
    }

    val requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(fullUrl.toString()))
            .header("Content-Type", "application/json");

    if (requestInfo.getApiKey() != null) {
      requestBuilder.header("Authorization", requestInfo.getApiKey());
    }

    val body =
        requestInfo.getBody() != null
            ? HttpRequest.BodyPublishers.ofString(requestInfo.getBody())
            : HttpRequest.BodyPublishers.noBody();
    switch (requestInfo.getMethod()) {
      case "GET":
        requestBuilder.GET();
        break;
      case "PUT":
        requestBuilder.PUT(body);
        break;
      case "POST":
        requestBuilder.POST(body);
        break;
      case "DELETE":
        requestBuilder.DELETE();
        break;
      case "PATCH":
        requestBuilder.method("PATCH", body);
        break;
      default:
        val error = new Error();
        error.setStatusCode(BigDecimal.valueOf(405));
        error.setDetails("Unsupported request method: " + requestInfo.getMethod());
        val errorResponse = new FdVResponse<Error>();
        errorResponse.setResourcesList(Collections.emptyList());
        errorResponse.setOperationOutcome(error);
        return (FdVResponse<T>) errorResponse;
    }

    HttpResponse<String> response;
    log.info("Sending {} request to {}", requestInfo.getMethod(), fullUrl);
    try {
      response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      throw new RemoteFdVErrorException(e.getMessage());
    }
    assert response != null;
    if (response.statusCode() >= 400) {
      log.error("Error response from server: {}", response.body());
      val errorResponse = new FdVResponse<Error>();
      errorResponse.setResourcesList(Collections.emptyList());
      errorResponse.setOperationOutcome(buildOperationOutcome(response));
      return (FdVResponse<T>) errorResponse;
    }
    log.info("Response from server: {}", response.body());
    val resource = deserialize(response.body(), request);
    log.info("Deserialized response: {}", resource);
    val fdvResponse = new FdVResponse<T>();
    fdvResponse.setResourcesList(resource);
    return fdvResponse;
  }

  private <T> List<T> deserialize(String response, FdVRequests<T> request) {
    val objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    if (request.getType().equals(String.class)) {
      return (List<T>) List.of(response);
    }
    if (response.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(response, request.getTypeReference());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public Error buildOperationOutcome(HttpResponse<String> response) {
    val dto = new Error();
    dto.setStatusCode(BigDecimal.valueOf(response.statusCode()));
    dto.setDetails(response.body());
    return dto;
  }

  public static class Builder {
    private final HttpRequestInfo requestBuilder = new HttpRequestInfo();
    private HttpClient httpClient = HttpClient.newHttpClient();
    private boolean mTlsEnabled = false;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStorePath;
    private String trustStorePassword;

    public Builder forRemote(String host) {
      requestBuilder.setHost(host);
      return this;
    }

    public Builder apiKey(String apiKey) {
      requestBuilder.setApiKey(apiKey);
      return this;
    }

    public Builder withKeystore(String path, String password) {
      mTlsEnabled = true;
      this.keyStorePath = path;
      this.keyStorePassword = password;
      return this;
    }

    public Builder withTruststore(String path, String password) {
      mTlsEnabled = true;
      this.trustStorePath = path;
      this.trustStorePassword = password;
      return this;
    }

    Builder withHttpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    private KeyStore loadKeyStore() throws Exception {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (InputStream is = ResourceLoader.getFileFromResourceAsStream(keyStorePath)) {
        keyStore.load(is, keyStorePassword.toCharArray());
      }

      return keyStore;
    }

    private KeyStore loadTrustStore() throws Exception {
      val is = ResourceLoader.getFileFromResourceAsStream(trustStorePath);
      if (is == null) {
        log.warn(
            "No truststore found at {}. Falling back to default JVM trust material.",
            trustStorePath);
        return null;
      }
      KeyStore trustStore = KeyStore.getInstance("PKCS12");
      try (InputStream closable = is) {
        trustStore.load(closable, trustStorePassword.toCharArray());
      }
      return trustStore;
    }

    private SSLContext createSslContextFromClasspathResources() {
      try {
        KeyManagerFactory kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), keyStorePassword.toCharArray());

        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(loadTrustStore());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
      } catch (NullPointerException e) {
        throw new MissingEnvironmentVariableException(
            "Environment variable KEYSTORE_PASSWORD is not set.");
      } catch (Exception e) {
        throw new RemoteFdVErrorException(
            "Failed to initialize mTLS SSL context: " + e.getMessage(), e);
      }
    }

    private HttpClient createSslContextAwareHttpClient() {
      SSLContext sslContext = createSslContextFromClasspathResources();
      return HttpClient.newBuilder()
          .sslContext(sslContext)
          .connectTimeout(Duration.ofSeconds(30))
          .build();
    }

    public RemoteFdVClient build() {
      if (mTlsEnabled) {
        return new RemoteFdVClient(requestBuilder, createSslContextAwareHttpClient());
      } else {
        return new RemoteFdVClient(requestBuilder, httpClient);
      }
    }
  }
}
