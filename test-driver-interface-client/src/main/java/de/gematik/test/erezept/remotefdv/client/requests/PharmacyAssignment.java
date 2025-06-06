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

package de.gematik.test.erezept.remotefdv.client.requests;

import com.fasterxml.jackson.core.type.TypeReference;
import de.gematik.test.erezept.remotefdv.client.HttpRequestInfo;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.openapitools.model.Communication;

@RequiredArgsConstructor
public class PharmacyAssignment implements PatientRequests<Communication> {
  private final String prescriptionId;
  private final String telematikId;
  private final String supplyOptionsType;
  private final @Getter Class<Communication> type = Communication.class;
  private final @Getter TypeReference<List<Communication>> typeReference = new TypeReference<>() {};

  @Override
  public void finalizeRequest(HttpRequestInfo rb) {
    rb.setResource("pharmacy/assignment");
    rb.setMethod("POST");
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("prescriptionId", prescriptionId);
    jsonObject.put("telematikId", telematikId);
    jsonObject.put("supplyOptionsType", supplyOptionsType);
    rb.setBody(jsonObject.toString());
  }
}
