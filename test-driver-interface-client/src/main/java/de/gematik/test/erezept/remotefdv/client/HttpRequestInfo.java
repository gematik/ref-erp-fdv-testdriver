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
 */

package de.gematik.test.erezept.remotefdv.client;

import java.util.HashMap;
import lombok.Getter;

@Getter
public class HttpRequestInfo {
  private String host;
  private String apiKey;
  private String method;
  private String resource;
  private String body;
  private String resourceId;
  private HashMap<String, String> query = new HashMap<>();

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setMethod(String method) {
    this.method = method.toUpperCase();
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public void setQuery(String key, String value) {
    query.put(key, value);
  }

  public void setBody(String body) {
    this.body = body;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }
}
