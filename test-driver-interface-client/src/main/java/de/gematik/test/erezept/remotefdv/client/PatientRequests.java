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

package de.gematik.test.erezept.remotefdv.client;

import de.gematik.test.erezept.remotefdv.client.requests.*;

public class PatientRequests {
  public static GetPrescription getPrescriptions() {
    return new GetPrescription();
  }

  public static Login loginWithKvnr(String kvnr) {
    return new Login(kvnr);
  }

  public static Start startFdV() {
    return new Start();
  }

  public static Stop stopFdV() {
    return new Stop();
  }

  public static GetPrescriptionById getPrescriptionById(String prescriptionId) {
    return new GetPrescriptionById(prescriptionId);
  }

  public static DeletePrescriptionById deletePrescriptionById(String prescriptionById) {
    return new DeletePrescriptionById(prescriptionById);
  }

  public static GetMedicationDispense getMedicationDispense(String whenHandedOver) {
    return new GetMedicationDispense(whenHandedOver);
  }

  public static GetAuditEvents getAuditEvents() {
    return new GetAuditEvents();
  }

  public static String getPharmacy() {
    // TODO not implemented yet
    return null;
  }

  public static PharmacyAssignment assignToPharmacy(
      String prescriptionId, String telematikId, String supplyOptionsType) {
    return new PharmacyAssignment(prescriptionId, telematikId, supplyOptionsType);
  }

  public static GenerateDataMatrixCode generateDataMatrixCode(String prescriptionId) {
    return new GenerateDataMatrixCode(prescriptionId);
  }

  public static GetCommunication getCommunication() {
    return new GetCommunication();
  }

  public static DeleteCommunicationById deleteCommunicationById(String prescriptionId) {
    return new DeleteCommunicationById(prescriptionId);
  }
}
