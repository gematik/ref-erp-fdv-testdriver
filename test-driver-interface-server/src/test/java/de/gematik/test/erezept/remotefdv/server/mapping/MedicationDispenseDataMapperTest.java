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

package de.gematik.test.erezept.remotefdv.server.mapping;

import static de.gematik.test.erezept.fhir.builder.GemFaker.fakerPrescriptionId;
import static de.gematik.test.erezept.fhir.builder.GemFaker.fakerTelematikId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.fhir.de.value.PZN;
import de.gematik.erezept.remotefdv.api.model.DeMedicationDispense;
import de.gematik.erezept.remotefdv.api.model.MedicationDispense;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.r4.eu.EuMedicationDispense;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;

class MedicationDispenseDataMapperTest {

  @Test
  void shouldMapGemErpMedicationWithPzn() {
    val erxMedDispense = mock(ErxMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);

    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);
    when(coding.getDisplay()).thenReturn("Ibuprofen 400mg");
    when(cc.getCodingFirstRep()).thenReturn(coding);

    when(gemErpMedication.getPzn()).thenReturn(Optional.of(PZN.random()));
    when(gemErpMedication.getId()).thenReturn(UUID.randomUUID().toString());
    when(gemErpMedication.isVaccine()).thenReturn(false);
    when(gemErpMedication.getCode()).thenReturn(cc);

    when(erxMedDispense.getPrescriptionId()).thenReturn(PrescriptionId.from(fakerPrescriptionId()));
    when(erxMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(erxMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(erxMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val pair = Pair.of(erxMedDispense, gemErpMedication);
    val result = assertDoesNotThrow(() -> MedicationDispenseDataMapper.fromGemErpMedication(pair));

    assertNotNull(result);
    assertInstanceOf(DeMedicationDispense.class, result);
    assertEquals(MedicationDispense.DispenseTypeEnum.DE, result.getDispenseType());
    assertNotNull(result.getMedication());
    assertEquals(
        de.gematik.erezept.remotefdv.api.model.Medication.TypeEnum.PZN,
        result.getMedication().getType());
    assertNotNull(((DeMedicationDispense) result).getPharmacy());
  }

  @Test
  void shouldMapGemErpMedicationWithoutPzn() {
    val erxMedDispense = mock(ErxMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);

    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);
    when(coding.getDisplay()).thenReturn("Wirkstoff ohne PZN");
    when(cc.getCodingFirstRep()).thenReturn(coding);

    when(gemErpMedication.getPzn()).thenReturn(Optional.empty());
    when(gemErpMedication.getId()).thenReturn(UUID.randomUUID().toString());
    when(gemErpMedication.isVaccine()).thenReturn(true);
    when(gemErpMedication.getCode()).thenReturn(cc);

    when(erxMedDispense.getPrescriptionId()).thenReturn(PrescriptionId.from(fakerPrescriptionId()));
    when(erxMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(erxMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(erxMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val pair = Pair.of(erxMedDispense, gemErpMedication);
    val result = assertDoesNotThrow(() -> MedicationDispenseDataMapper.fromGemErpMedication(pair));

    assertNotNull(result);
    assertInstanceOf(DeMedicationDispense.class, result);
    assertEquals(MedicationDispense.DispenseTypeEnum.DE, result.getDispenseType());

    assertNull(result.getMedication().getType());
    assertTrue(result.getMedication().getIsVaccine());
  }

  @Test
  void shouldSetPrescriptionIdFromGemErpMedication() {
    val prescriptionId = PrescriptionId.from(fakerPrescriptionId());
    val erxMedDispense = mock(ErxMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);

    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);
    when(coding.getDisplay()).thenReturn("Test");
    when(cc.getCodingFirstRep()).thenReturn(coding);
    when(gemErpMedication.getPzn()).thenReturn(Optional.empty());
    when(gemErpMedication.getId()).thenReturn(UUID.randomUUID().toString());
    when(gemErpMedication.isVaccine()).thenReturn(false);
    when(gemErpMedication.getCode()).thenReturn(cc);

    when(erxMedDispense.getPrescriptionId()).thenReturn(prescriptionId);
    when(erxMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(erxMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(erxMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val result =
        MedicationDispenseDataMapper.fromGemErpMedication(
            Pair.of(erxMedDispense, gemErpMedication));

    assertEquals(prescriptionId.getValue(), result.getPrescriptionId());
  }

  @Test
  void shouldMapEuMedicationDispense() {
    val euMedDispense = mock(EuMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);
    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);

    when(coding.getDisplay()).thenReturn("EU-Medikament");
    when(cc.getCodingFirstRep()).thenReturn(coding);
    when(gemErpMedication.getCode()).thenReturn(cc);
    when(gemErpMedication.isVaccine()).thenReturn(false);

    when(euMedDispense.getPrescriptionId()).thenReturn(PrescriptionId.from(fakerPrescriptionId()));
    when(euMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(euMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(euMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val result =
        assertDoesNotThrow(
            () ->
                MedicationDispenseDataMapper.fromEuErpMedication(
                    Pair.of(euMedDispense, gemErpMedication)));

    assertNotNull(result);
    assertEquals(MedicationDispense.DispenseTypeEnum.EU, result.getDispenseType());
    assertNotNull(result.getMedication());
    assertEquals("EU-Medikament", result.getMedication().getCode());
    assertNotNull(result.getPharmacist());
  }

  @Test
  void shouldSetPrescriptionIdFromEuMedicationDispense() {
    val prescriptionId = PrescriptionId.from(fakerPrescriptionId());
    val euMedDispense = mock(EuMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);
    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);

    when(coding.getDisplay()).thenReturn("Test");
    when(cc.getCodingFirstRep()).thenReturn(coding);
    when(gemErpMedication.getCode()).thenReturn(cc);
    when(gemErpMedication.isVaccine()).thenReturn(false);

    when(euMedDispense.getPrescriptionId()).thenReturn(prescriptionId);
    when(euMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(euMedDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());
    when(euMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val result =
        MedicationDispenseDataMapper.fromEuErpMedication(Pair.of(euMedDispense, gemErpMedication));

    assertEquals(prescriptionId.getValue(), result.getPrescriptionId());
  }

  @Test
  void shouldSetPharmacistFromEuMedicationDispense() {
    val telematikId = fakerTelematikId();
    val euMedDispense = mock(EuMedicationDispense.class);
    val gemErpMedication = mock(GemErpMedication.class);
    val cc = mock(CodeableConcept.class);
    val coding = mock(Coding.class);

    when(coding.getDisplay()).thenReturn("Dummy");
    when(cc.getCodingFirstRep()).thenReturn(coding);
    when(gemErpMedication.getCode()).thenReturn(cc);
    when(gemErpMedication.isVaccine()).thenReturn(false);

    when(euMedDispense.getPrescriptionId()).thenReturn(PrescriptionId.from(fakerPrescriptionId()));
    when(euMedDispense.getWhenHandedOver()).thenReturn(new Date());
    when(euMedDispense.getPerformerIdFirstRep()).thenReturn(telematikId);
    when(euMedDispense.getPerformerFirstRep())
        .thenReturn(
            new org.hl7.fhir.r4.model.MedicationDispense.MedicationDispensePerformerComponent());

    val result =
        MedicationDispenseDataMapper.fromEuErpMedication(Pair.of(euMedDispense, gemErpMedication));

    assertEquals(telematikId, result.getPharmacist().getIdentifier());
  }
}
