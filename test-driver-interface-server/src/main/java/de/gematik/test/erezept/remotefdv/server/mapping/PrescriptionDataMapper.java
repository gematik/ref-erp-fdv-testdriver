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

import de.gematik.erezept.remotefdv.api.model.*;
import de.gematik.test.erezept.fhir.r4.erp.ErxTask;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrescriptionDataMapper {

  public static Prescription from(ErxTask task) {
    val prescription = new Prescription();
    setBaseFields(prescription, task);
    return prescription;
  }

  public static Prescription from(ErxTask task, KbvErpBundle kbvBundle) {
    val prescription = new Prescription();
    setBaseFields(prescription, task);

    val patientDto = new Patient();
    val practitioner = new Practitioner();
    val medication = new Medication();

    patientDto.setName(kbvBundle.getPatient().getFullname());
    patientDto.setKvnr(kbvBundle.getPatient().getKvnr().getValue());

    if (kbvBundle.getPatient().hasGkvKvnr()) {
      patientDto.setInsuranceType(Patient.InsuranceTypeEnum.GKV);
    } else {
      patientDto.setInsuranceType(Patient.InsuranceTypeEnum.PKV);
    }

    practitioner.setName(kbvBundle.getPractitioner().getFullName());
    practitioner.setQualificationType(
        kbvBundle.getPractitioner().getQualificationType().getDisplay());
    val baseANR = kbvBundle.getPractitioner().getANR();
    baseANR.ifPresent(
        anr -> practitioner.setAnrType(Practitioner.AnrTypeEnum.fromValue(anr.getType().name())));
    baseANR.ifPresent(anr -> practitioner.setAnr(anr.getValue()));

    val kbvErpMedication = kbvBundle.getMedication();
    kbvErpMedication
        .getMedicationType()
        .ifPresent(type -> medication.setType(Medication.TypeEnum.fromValue(type.getCode())));
    medication.setCode(kbvBundle.getMedication().getDescription());
    medication.setIsVaccine(kbvBundle.getMedication().isVaccine());

    prescription.setPatient(patientDto);
    prescription.setPractitioner(practitioner);
    prescription.setMedication(medication);
    if (!task.getFlowType().isDirectAssignment()) {
      prescription.setAccessCode(task.getAccessCode().getValue());
    }
    return prescription;
  }

  private static void setBaseFields(Prescription prescription, ErxTask task) {
    prescription.setPrescriptionId(task.getPrescriptionId().getValue());
    prescription.setAuthoredOn(formatToUTC(task.getAuthoredOn()));
    prescription.setAcceptDate(formatToUTC(task.getAcceptDate()));
    prescription.setExpiryDate(formatToUTC(task.getExpiryDate()));
    prescription.setStatus(Prescription.StatusEnum.fromValue(task.getStatus().toCode()));
    prescription.setWorkFlow(WorkFlow.fromValue(task.getFlowType().getCode()));

    val lastMedDispense = task.getLastMedicationDispenseDate();
    lastMedDispense.ifPresent(
        instant -> prescription.setLastMedicationDispense(instant.toString()));
    prescription.setEuRedeemableByProperties(true); // TODO find this parameter from ErxTask
  }

  private static String formatToUTC(Date date) {
    val instant = date.toInstant();
    val odt = instant.atOffset(ZoneOffset.UTC);
    return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
