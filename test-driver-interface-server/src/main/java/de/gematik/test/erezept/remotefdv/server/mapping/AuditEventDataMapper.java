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

package de.gematik.test.erezept.remotefdv.server.mapping;

import de.gematik.erezept.remotefdv.api.model.AuditEvent;
import de.gematik.test.erezept.fhir.r4.erp.ErxAuditEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditEventDataMapper {

  public static AuditEvent from(ErxAuditEvent erxAuditEvent) {
    val auditEvent = new AuditEvent();
    auditEvent.setRecorded(DataMapperUtils.formatToUTCString(erxAuditEvent.getRecorded()));
    auditEvent.setText(erxAuditEvent.getFirstText());
    if (erxAuditEvent.getPrescriptionId().isPresent()) {
      auditEvent.setPrescriptionId(erxAuditEvent.getPrescriptionId().get().getValue());
    }
    auditEvent.setAction(AuditEvent.ActionEnum.fromValue(erxAuditEvent.getAction().toCode()));
    auditEvent.setWho(erxAuditEvent.getAgentName());
    return auditEvent;
  }
}
