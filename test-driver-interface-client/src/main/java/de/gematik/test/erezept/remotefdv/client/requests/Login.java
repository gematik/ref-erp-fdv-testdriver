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

package de.gematik.test.erezept.remotefdv.client.requests;

import com.fasterxml.jackson.core.type.TypeReference;
import de.gematik.test.erezept.remotefdv.client.HttpRequestInfo;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openapitools.model.LoginSuccess;

@RequiredArgsConstructor
public class Login implements PatientRequests<LoginSuccess> {
  private final String kvnr;
  private final @Getter Class<LoginSuccess> type = LoginSuccess.class;
  private final @Getter TypeReference<List<LoginSuccess>> typeReference = new TypeReference<>() {};

  @Override
  public void finalizeRequest(HttpRequestInfo rb) {
    rb.setMethod("PUT");
    rb.setResource("login");
    rb.setBody(kvnr);
  }
}
