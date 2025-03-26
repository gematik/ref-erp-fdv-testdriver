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

package de.gematik.test.erezept.remotefdv.server.mapping;

import de.gematik.test.erezept.fhir.resources.erp.ErxAuditEvent;
import lombok.val;
import org.openapitools.model.AuditEvent;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class AuditEventDataMapper {
  private AuditEventDataMapper() {
    // hide constructor
    throw new IllegalAccessError("Utility class");
  }

  public static AuditEvent from(ErxAuditEvent erxAuditEvent) {
    val auditEvent = new AuditEvent();
    val instant = erxAuditEvent.getRecorded().toInstant();
    val odt = instant.atOffset(ZoneOffset.UTC);
    val recordedUTC =odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    auditEvent.setRecorded(recordedUTC);
    auditEvent.setText(erxAuditEvent.getFirstText());
    if (erxAuditEvent.getPrescriptionId().isPresent()) {
      auditEvent.setPrescriptionId(erxAuditEvent.getPrescriptionId().get().getValue());
    }
    auditEvent.setAction(AuditEvent.ActionEnum.fromValue(erxAuditEvent.getAction().toCode()));
    auditEvent.setWho(erxAuditEvent.getAgentName());
    return auditEvent;
  }
}
