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

package de.gematik.test.erezept.remotefdv.server.webservice.mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.utils.PrivateConstructorsUtil;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpBundleFaker;
import de.gematik.test.erezept.fhir.resources.erp.ErxTask;
import de.gematik.test.erezept.fhir.resources.kbv.KbvErpMedication;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.MedicationType;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import de.gematik.test.erezept.remotefdv.server.mapping.PrescriptionDataMapper;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;

class PrescriptionDataMapperTest {
  @Test
  void shouldNotInstantiate() {
    assertTrue(PrivateConstructorsUtil.isUtilityConstructor(PrescriptionDataMapper.class));
  }

  @Test
  void shouldCreatePrescriptionFromErxTask() {
    val erxTask = mock(ErxTask.class);
    val medication = mock(KbvErpMedication.class);

    when(medication.getMedicationType()).thenReturn(Optional.of(MedicationType.INGREDIENT));
    when(medication.getId()).thenReturn(UUID.randomUUID().toString());
    when(medication.getResourceType()).thenReturn(ResourceType.Medication);
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.COMPLETED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(erxTask.getPrescriptionId()).thenReturn(PrescriptionId.random());
    when(erxTask.getAuthoredOn()).thenReturn(new Date());
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getExpiryDate()).thenReturn(new Date());

    val kbvErpBundle = KbvErpBundleFaker.builder().withMedication(medication).fake();
    val prescription = assertDoesNotThrow(() -> PrescriptionDataMapper.from(erxTask, kbvErpBundle));
    assertEquals(
        MedicationType.INGREDIENT.getCode(), prescription.getMedication().getType().toString());
  }
}
