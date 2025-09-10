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
import de.gematik.erezept.remotefdv.api.model.Prescription;
import de.gematik.test.erezept.remotefdv.client.HttpRequestInfo;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

@AllArgsConstructor
public class PatchPrescriptionById implements PatientRequests<Prescription> {
  private final String prescriptionId;
  private final boolean euRedeemable;
  private final @Getter Class<Prescription> type = Prescription.class;
  private final @Getter TypeReference<List<Prescription>> typeReference = new TypeReference<>() {};

  @Override
  public void finalizeRequest(HttpRequestInfo rb) {
    rb.setMethod("PATCH");
    rb.setResource("prescription");
    rb.setResourceId(prescriptionId);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("euRedeemableByPatient", euRedeemable);
    rb.setBody(jsonObject.toString());
  }
}
