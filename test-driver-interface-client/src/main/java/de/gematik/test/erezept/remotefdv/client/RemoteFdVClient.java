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

package de.gematik.test.erezept.remotefdv.client;

import static java.text.MessageFormat.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.erezept.remotefdv.api.model.Error;
import de.gematik.test.erezept.remotefdv.client.requests.PatientRequests;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class RemoteFdVClient {
  private final HttpRequestInfo requestInfo;

  private RemoteFdVClient(HttpRequestInfo requestInfo) {
    this.requestInfo = requestInfo;
  }

  public static Builder builder() {
    return new Builder();
  }

  public <T> FdVResponse<T> sendRequest(PatientRequests<T> request) {
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
            .header("Authorization", requestInfo.getApiKey())
            .header("Content-Type", "application/json");

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

    HttpClient httpClient = HttpClient.newHttpClient();
    HttpResponse<String> response = null;
    log.info("Sending request to {}", fullUrl);
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

  public <T> List<T> deserialize(String response, PatientRequests<T> request) {
    val objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    if (request.getType().equals(String.class)) {
      val list = List.of(response);
      return (List<T>) list;
    }
    if (!response.isEmpty()) {
      try {
        return objectMapper.readValue(response, request.getTypeReference());
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    } else {
      return Collections.emptyList();
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

    public Builder forRemote(String host) {
      requestBuilder.setHost(host);
      return this;
    }

    public Builder apiKey(String apiKey) {
      requestBuilder.setApiKey(apiKey);
      return this;
    }

    public RemoteFdVClient build() {
      return new RemoteFdVClient(requestBuilder);
    }
  }
}
