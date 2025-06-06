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

import de.gematik.test.erezept.fhir.resources.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.resources.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.resources.kbv.KbvErpMedication;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.openapitools.model.Medication;
import org.openapitools.model.MedicationDispense;
import org.openapitools.model.Pharmacist;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MedicationDispenseDataMapper {

  public static MedicationDispense fromGemErpMedication(
      Pair<ErxMedicationDispense, GemErpMedication> pair) {
    val erxMedDispense = pair.getKey();
    val gemErpMedication = pair.getValue();
    val medication = new Medication();
    if (gemErpMedication.getPzn().isPresent()) {
      medication.setType(Medication.TypeEnum.PZN);
    }
    medication.setIsVaccine(gemErpMedication.isVaccine());
    medication.setCode(gemErpMedication.getCode().getText());
    return createMedDispense(erxMedDispense, medication);
  }

  public static MedicationDispense fromKbvErpMedication(
      Pair<ErxMedicationDispense, KbvErpMedication> pair) {
    val erxMedDispense = pair.getKey();
    val kbvErpMedication = pair.getValue();
    val medication = new Medication();
    if (kbvErpMedication.getMedicationType().isPresent()) {
      medication.setType(
          Medication.TypeEnum.fromValue(kbvErpMedication.getMedicationType().get().getCode()));
    }
    medication.setIsVaccine(kbvErpMedication.isVaccine());
    medication.setCode(kbvErpMedication.getCode().getText());
    return createMedDispense(erxMedDispense, medication);
  }

  private static MedicationDispense createMedDispense(
      ErxMedicationDispense erxMedDispense, Medication medication) {
    val medDispense = new MedicationDispense();
    val instant = erxMedDispense.getWhenHandedOver().toInstant();
    val odt = instant.atOffset(ZoneOffset.UTC);
    val whenHandedOverUTC = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    val pharmacist = new Pharmacist();
    pharmacist.setName(erxMedDispense.getPerformerFirstRep().toString());
    pharmacist.setIdentifier(erxMedDispense.getPerformerIdFirstRep());
    medDispense.setWhenhandedover(whenHandedOverUTC);
    medDispense.setPrescriptionId(erxMedDispense.getPrescriptionId().getValue());
    medDispense.setPharmacist(pharmacist);
    medDispense.setMedication(medication);
    return medDispense;
  }
}
