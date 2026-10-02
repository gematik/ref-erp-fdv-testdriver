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

import de.gematik.erezept.remotefdv.api.model.*;
import de.gematik.test.erezept.fhir.r4.erp.ErxMedicationDispense;
import de.gematik.test.erezept.fhir.r4.erp.GemErpMedication;
import de.gematik.test.erezept.fhir.r4.eu.EuMedicationDispense;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

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
    medication.setCode(gemErpMedication.getCode().getCodingFirstRep().getDisplay());
    return createMedDispense(erxMedDispense, medication);
  }

  private static MedicationDispense createMedDispense(
      ErxMedicationDispense erxMedDispense, Medication medication) {
    val medDispense = new DeMedicationDispense();
    val pharmacist = new Pharmacist();
    val pharmacy = new Pharmacy();
    pharmacist.setName(erxMedDispense.getPerformerFirstRep().toString());
    pharmacist.setIdentifier(erxMedDispense.getPerformerIdFirstRep());
    pharmacy.setName(erxMedDispense.getPerformerFirstRep().toString());
    pharmacy.setAddress(new Address());
    pharmacy.setPharmacist(List.of(pharmacist));
    medDispense.setWhenhandedover(
        DataMapperUtils.formatToUTCString(erxMedDispense.getWhenHandedOver()));
    medDispense.setPrescriptionId(erxMedDispense.getPrescriptionId().getValue());
    medDispense.setPharmacy(pharmacy);
    medDispense.setMedication(medication);
    medDispense.setDispenseType(MedicationDispense.DispenseTypeEnum.DE);
    return medDispense;
  }

  public static EUMedicationDispense fromEuErpMedication(
      Pair<EuMedicationDispense, GemErpMedication> pair) {
    val euMedicationDispense = pair.getKey();
    val euErpMedication = pair.getValue();

    val dto = new EUMedicationDispense();
    val pharmacist = new Pharmacist();
    pharmacist.setName(euMedicationDispense.getPerformerFirstRep().toString());
    pharmacist.setIdentifier(euMedicationDispense.getPerformerIdFirstRep());
    val medication = new Medication();
    medication.setCode(euErpMedication.getCode().getCodingFirstRep().getDisplay());
    medication.setIsVaccine(euErpMedication.isVaccine());
    medication.setType(Medication.TypeEnum.PZN);
    dto.setDispenseType(MedicationDispense.DispenseTypeEnum.EU);
    dto.setPrescriptionId(euMedicationDispense.getPrescriptionId().getValue());
    dto.setWhenhandedover(
        DataMapperUtils.formatToUTCString(euMedicationDispense.getWhenHandedOver()));
    dto.setMedication(medication);
    dto.setPharmacist(pharmacist);
    return dto;
  }
}
