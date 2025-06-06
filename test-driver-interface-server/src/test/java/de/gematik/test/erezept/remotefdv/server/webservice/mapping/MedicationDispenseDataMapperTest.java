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

import static de.gematik.test.erezept.fhir.builder.GemFaker.fakerPrescriptionId;
import static de.gematik.test.erezept.fhir.builder.GemFaker.fakerTelematikId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.test.erezept.fhir.resources.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.resources.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.values.PZN;
import de.gematik.test.erezept.remotefdv.server.mapping.MedicationDispenseDataMapper;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.junit.jupiter.api.Test;

class MedicationDispenseDataMapperTest {

  @Test
  void shouldCreateMedicationDispense() {
    val erxMedDispense = mock(ErxMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);
    when(gemErpMedication.getPzn()).thenReturn(Optional.of(PZN.random()));
    when(gemErpMedication.getId()).thenReturn(UUID.randomUUID().toString());

    when(erxMedDispense.getPrescriptionId()).thenReturn(fakerPrescriptionId());
    when(erxMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(erxMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(erxMedDispense.getPerformerFirstRep())
        .thenReturn(new MedicationDispense.MedicationDispensePerformerComponent());
    val cc = mock(CodeableConcept.class);
    when(cc.getText()).thenReturn("Nerisona 30g, Asche Basis 60g");
    when(gemErpMedication.getCode()).thenReturn(cc);
    Pair<ErxMedicationDispense, GemErpMedication> pair = Pair.of(erxMedDispense, gemErpMedication);
    assertDoesNotThrow(() -> MedicationDispenseDataMapper.fromGemErpMedication(pair));
  }
}
