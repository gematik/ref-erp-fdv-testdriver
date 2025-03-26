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

import de.gematik.test.erezept.fhir.resources.erp.ErxTask;
import de.gematik.test.erezept.fhir.resources.kbv.KbvErpBundle;
import lombok.val;
import org.openapitools.model.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PrescriptionDataMapper {
  private PrescriptionDataMapper() {
    // hide constructor
    throw new IllegalAccessError("Utility class");
  }

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
    patientDto.setInsuranceType(
        Patient.InsuranceTypeEnum.fromValue(kbvBundle.getPatient().getInsuranceKind().getCode()));

    practitioner.setName(kbvBundle.getPractitioner().getFullName());
    practitioner.setQualificationType(
        kbvBundle.getPractitioner().getQualificationType().getDisplay());
    practitioner.setAnrType(
        Practitioner.AnrTypeEnum.fromValue(kbvBundle.getPractitioner().getANRType().name()));
    practitioner.setAnr(kbvBundle.getPractitioner().getANR().getValue());

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
  }
  private static String formatToUTC(Date date){
    val instant = date.toInstant();
    val odt = instant.atOffset(ZoneOffset.UTC);
    return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
