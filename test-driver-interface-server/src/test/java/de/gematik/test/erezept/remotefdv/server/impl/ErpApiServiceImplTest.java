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

package de.gematik.test.erezept.remotefdv.server.impl;

import static de.gematik.test.erezept.fhir.builder.GemFaker.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erezept.remotefdv.api.model.EUAccessAuthorization;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.rest.ErpResponse;
import de.gematik.test.erezept.client.usecases.*;
import de.gematik.test.erezept.client.usecases.eu.EuGrantAccessPostCommand;
import de.gematik.test.erezept.fhir.r4.erp.*;
import de.gematik.test.erezept.fhir.r4.eu.EuAccessPermission;
import de.gematik.test.erezept.fhir.values.*;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCode;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import de.gematik.test.erezept.remotefdv.server.actors.Patient;
import java.time.Instant;
import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import lombok.SneakyThrows;
import lombok.val;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ErpApiServiceImplTest {
  private static ErpClient mockClient;
  private static Patient patient;
  private static ErpApiServiceImpl service;
  private static SecurityContext mockApiKey;
  private static ErpResponse mockResponse;

  @BeforeEach
  void setUp() {
    // basic setup
    mockClient = mock(ErpClient.class);
    patient = spy(Patient.class);
    service = new ErpApiServiceImpl();
    mockApiKey = mock(SecurityContext.class);
    service.setErpClient(mockClient);
    service.setPatient(patient);
    patient.setClient(mockClient);

    // setup to test handling OperationOutcome
    val oo = mock(OperationOutcome.class);
    val issueComponent = new OperationOutcome.OperationOutcomeIssueComponent();
    issueComponent.setCode(OperationOutcome.IssueType.VALUE);
    issueComponent.setDetails(new CodeableConcept().setText("some details"));
    issueComponent.setDiagnostics("some diagnostics");
    when(oo.getIssueFirstRep()).thenReturn(issueComponent);
    mockResponse = mock(ErpResponse.class);
    when(mockResponse.getAsOperationOutcome()).thenReturn(oo);
  }

  public static ValidationResult createEmptyValidationResult() {
    ValidationResult vr = Mockito.mock(ValidationResult.class);
    Mockito.when(vr.isSuccessful()).thenReturn(true);
    Mockito.when(vr.getMessages()).thenReturn(List.of());
    return vr;
  }

  @Test
  void shouldReturnOperationOutcomeByAuditEventsGet() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(AuditEventGetCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1AuditEventsGet(mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1AuditEventsGet(mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldGetAuditEvents() {
    val bundle = mock(ErxAuditEventBundle.class);
    val erxAuditEvent = mock(ErxAuditEvent.class);
    when(erxAuditEvent.getRecorded()).thenReturn(new Date());
    when(erxAuditEvent.getFirstText()).thenReturn("test");
    when(erxAuditEvent.getPrescriptionId()).thenReturn(Optional.of(PrescriptionId.random()));
    when(erxAuditEvent.getAction()).thenReturn(AuditEvent.AuditEventAction.C);
    when(bundle.getAuditEvents()).thenReturn(List.of(erxAuditEvent));
    val erpResponse =
        ErpResponse.forPayload(bundle, ErxAuditEventBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(AuditEventGetCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1AuditEventsGet(mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldNotGetAuditEventBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1AuditEventsGet(mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeByCommunicationsGet() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(CommunicationGetCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1CommunicationGet(mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1CommunicationGet(mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldGetCommunications() {
    val body = new ErxCommunicationBundle();

    val erpResponse =
        ErpResponse.forPayload(body, ErxCommunicationBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(CommunicationGetCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1CommunicationGet(mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldNotGetCommunicationsBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1CommunicationGet(mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeByCommunicationsDelete() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(CommunicationDeleteCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1CommunicationIdDelete("id", mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1CommunicationIdDelete("id", mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldDeleteCommunicationById() {
    val erpResponse =
        ErpResponse.forPayload(null, EmptyResource.class)
            .withStatusCode(204)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(CommunicationDeleteCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1CommunicationIdDelete("ComId", mockApiKey));
    assertEquals(204, r.getStatus());
  }

  @Test
  void shouldNotDeleteCommunicationWithoutId() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1CommunicationIdDelete(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotDeleteCommunicationBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1CommunicationIdDelete("ComId", mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldNotLoginWithoutPatient() {
    service.setPatient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1LoginPut(KVNR.random().getValue(), mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotLoginWithoutKvnr() {
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1LoginPut(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  /*@Test
  void shouldGetInfo() {
    Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1InfoGet(mockApiKey));
  }*/

  @Test
  void shouldReturnOperationOutcomeByMedicationDispenseGet() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(MedicationDispenseGetCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1MedicationdispenseGet("date", mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1MedicationdispenseGet("date", mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldGetMedicationDispense() {
    val bundle = mock(ErxMedicationDispenseBundle.class);
    val erxMedicationDispense = mock(ErxMedicationDispense.class);

    when(erxMedicationDispense.getPrescriptionId()).thenReturn(PrescriptionId.random());
    when(erxMedicationDispense.getWhenHandedOver()).thenReturn(new Date());
    when(erxMedicationDispense.getPerformerIdFirstRep()).thenReturn(fakerTelematikId());

    when(bundle.getMedicationDispenses()).thenReturn(List.of(erxMedicationDispense));
    val erpResponse =
        ErpResponse.forPayload(bundle, ErxMedicationDispenseBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(MedicationDispenseGetCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1MedicationdispenseGet("date", mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldNotGetMedicationDispenseBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1MedicationdispenseGet("date", mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldNotGetMedicationDispenseWithoutWhenHandedOver() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1MedicationdispenseGet(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotPostDataMatrixCodeWithoutTaskId() {
    Map<String, String> map = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    String body;
    try {
      body = objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1Pharmacy2dCodePost(body, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  /*@Test //TODO fix this failing test
  void shouldPostDataMatrixCode() {
    val taskId = PrescriptionId.random();
    val accessCode = AccessCode.random();

    Map<String, String> map = new HashMap<>();
    map.put("taskId", taskId.getValue());
    map.put("accessCode", accessCode.getValue());
    ObjectMapper objectMapper = new ObjectMapper();
    String body;
    try {
      body = objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    val kbvErpBundle = KbvErpBundleFaker.builder().fake();
    val erxTask = mock(ErxTask.class);
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.ACCEPTED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);
    when(prescriptionBundle.getKbvBundle()).thenReturn(Optional.of(kbvErpBundle));
    val erpResponse =
        ErpResponse.forPayload(prescriptionBundle, ErxPrescriptionBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1Pharmacy2dCodePost(body, mockApiKey));
    assertEquals(200, r.getStatus());
  }*/

  @Test
  void shouldNotPostDataMatrixCodeBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1Pharmacy2dCodePost("body", mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldNotPostDataMatrixCodeWithoutBody() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1Pharmacy2dCodePost(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotAssignToPharmacyWithoutTaskId() {
    Map<String, String> map = new HashMap<>();
    map.put("telematikId", fakerTelematikId());
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotAssignToPharmacyWithoutAccessCode() {
    val taskId = PrescriptionId.random();
    Map<String, String> map = new HashMap<>();
    map.put("taskId", taskId.getValue());

    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotAssignToPharmacyWithoutTelematikId() {
    Map<String, String> map = new HashMap<>();
    map.put("taskId", PrescriptionId.random().getValue());

    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    val erxTask = mock(ErxTask.class);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);

    val erpResponse =
        ErpResponse.forPayload(prescriptionBundle, ErxPrescriptionBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeByPharmacyAssignment() {
    val taskId = PrescriptionId.random();
    val telematikId = fakerTelematikId();

    Map<String, String> map = new HashMap<>();
    map.put("taskId", taskId.getValue());
    map.put("telematikId", telematikId);

    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    val erxTask = mock(ErxTask.class);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);

    val erpResponse =
        ErpResponse.forPayload(prescriptionBundle, ErxPrescriptionBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);

    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(CommunicationPostCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  /*@Test
  void shouldAssignToPharmacy() {
    val prescriptionId = PrescriptionId.random();
    val telematikId = fakerTelematikId();

    Map<String, String> map = new HashMap<>();
    map.put("prescriptionId", prescriptionId.getValue());
    map.put("telematikId", telematikId);

    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    val erxTask = mock(ErxTask.class);
    val flowType = randomElement(PrescriptionFlowType.values());
    when(erxTask.getFlowType()).thenReturn(flowType);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);

    val erpResponse =
        ErpResponse.forPayload(prescriptionBundle, ErxPrescriptionBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    val com = mock(ErxCommunication.class);
    when(com.getUnqualifiedId()).thenReturn(UUID.randomUUID().toString());
    when(com.getBasedOnReferenceId()).thenReturn(TaskId.from(prescriptionId));
    when(com.getSenderId()).thenReturn(KVNR.random().getValue());
    when(com.getRecipientId()).thenReturn(fakerTelematikId());
    when(com.getSent()).thenReturn(new Date());
    when(com.getMessage()).thenReturn(SupplyOptionsType.ON_PREMISE.toString());
    when(com.getBasedOnReferenceId().getFlowType()).thenReturn(flowType); //TODO fix this

    val erpResponse2 =
        ErpResponse.forPayload(com, ErxCommunication.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    when(mockClient.request(any(CommunicationPostCommand.class))).thenReturn(erpResponse2);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(200, r.getStatus());*/
  // }

  @Test
  void shouldNotPostPharmacyAssignmentBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost("body", mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldNotPostPharmacyAssignmentWithoutBody() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldSearchForPharmacy() {
    Assertions.assertDoesNotThrow(
        () ->
            service.erpTestdriverApiV1PharmacySearchGet(
                fakerZipCode(), fakerCity(), fakerName(), mockApiKey));
  }

  @Test
  void shouldReturnOperationOutcomeByPrescriptionsGet() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(TaskGetCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1PrescriptionGet(mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1PrescriptionGet(mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldGetPrescriptions() {
    val bundle = mock(ErxTaskBundle.class);
    val erxTask = mock(ErxTask.class);
    when(erxTask.getPrescriptionId()).thenReturn(PrescriptionId.random());
    when(erxTask.getAuthoredOn()).thenReturn(new Date());
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.COMPLETED);
    when(erxTask.getExpiryDate()).thenReturn(new Date());
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());

    when(bundle.getTasks()).thenReturn(List.of(erxTask));
    val erpResponse =
        ErpResponse.forPayload(bundle, ErxTaskBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(TaskGetCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1PrescriptionGet(mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldNotGetPrescriptionsBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1PrescriptionGet(mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeByPrescriptionDelete() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(TaskAbortCommand.class))).thenReturn(mockResponse);
    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1PrescriptionIdDelete(fakerPrescriptionId(), mockApiKey));
    when(mockResponse.isOperationOutcome()).thenReturn(false);
    when(mockResponse.getStatusCode()).thenReturn(400);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdDelete(fakerPrescriptionId(), mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldDeletePrescriptionById() {
    val erpResponse =
        ErpResponse.forPayload(null, EmptyResource.class)
            .withStatusCode(204)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskAbortCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdDelete(
                    PrescriptionId.random().getValue(), mockApiKey));
    assertEquals(204, r.getStatus());
  }

  @Test
  void shouldNotDeletePrescriptionWithoutId() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PrescriptionIdDelete(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotDeletePrescriptionBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdDelete(
                    PrescriptionId.random().getValue(), mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeByPrescriptionGetById() {
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(mockResponse);

    Assertions.assertDoesNotThrow(
        () -> service.erpTestdriverApiV1PrescriptionIdGet(fakerPrescriptionId(), mockApiKey));
  }

  /*@Test //TODO fix this failing test
  void shouldGetPrescriptionById() {
    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    // val kbvErpBundle = KbvErpBundleFaker.builder().fake();
    val kbvErpBundle =
        KbvErpBundleFaker.builder()
            .withVersion(KbvItaErpVersion.V1_2_0, KbvItaForVersion.V1_2_0)
            .fake();
    val erxTask = mock(ErxTask.class);
    val prescriptionId = PrescriptionId.random();
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.COMPLETED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(erxTask.getPrescriptionId()).thenReturn(prescriptionId);
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getExpiryDate()).thenReturn(new Date());
    when(erxTask.getAuthoredOn()).thenReturn(new Date());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);
    when(prescriptionBundle.getKbvBundle()).thenReturn(Optional.of(kbvErpBundle));

    val erpResponse =
        ErpResponse.forPayload(prescriptionBundle, ErxPrescriptionBundle.class)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdGet(
                    PrescriptionId.random().getValue(), mockApiKey));
    assertEquals(200, r.getStatus());
  }*/

  @Test
  void shouldNotGetPrescriptionWithoutId() {
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PrescriptionIdGet(null, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldNotGetPrescriptionBeforeAuthentication() {
    service.setErpClient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdGet(
                    PrescriptionId.random().getValue(), mockApiKey));
    assertEquals(403, r.getStatus());
  }

  @Test
  void shouldStartTestFdV() {
    val service = new ErpApiServiceImpl();
    val r = Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1StartPut(mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldStopTestFdV() {
    val r = Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1StopPut(mockApiKey));
    assertEquals(200, r.getStatus());
  }

  @Test
  void shouldCheckRequiredFields() {
    service.setPatient(null);
    val r = Assertions.assertDoesNotThrow(() -> service.checkRequiredFields());
    assertEquals(400, r.build().getStatus());
  }

  @SneakyThrows
  @Test
  void shouldSetEuAccessAuthorization() {
    val accessCode = EuAccessCode.random();
    val euAccessPermission = mock(EuAccessPermission.class);
    when(euAccessPermission.getIsoCountryCode()).thenReturn(IsoCountryCode.LT);
    when(euAccessPermission.getAccessCode()).thenReturn(accessCode);
    when(euAccessPermission.getCreateAt()).thenReturn(Optional.of(Instant.now()));
    when(euAccessPermission.getValidUntil()).thenReturn(Optional.of(Instant.now()));

    val erpResponse =
        ErpResponse.forPayload(euAccessPermission, EuAccessPermission.class)
            .withStatusCode(201)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(EuGrantAccessPostCommand.class))).thenReturn(erpResponse);

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> service.erpTestdriverApiV1EuAccessAuthorizationPost("", "", mockApiKey));
    var euAccessAuthorization =
        readEntity(
                service.erpTestdriverApiV1EuAccessAuthorizationPost("LI", "", mockApiKey),
                EUAccessAuthorization.class)
            .orElseThrow();
    Assertions.assertEquals(accessCode, EuAccessCode.from(euAccessAuthorization.getAccessCode()));
    euAccessAuthorization =
        readEntity(
                service.erpTestdriverApiV1EuAccessAuthorizationPost(
                    "LI", accessCode.getValue(), mockApiKey),
                EUAccessAuthorization.class)
            .orElseThrow();
    Assertions.assertEquals(accessCode, EuAccessCode.from(euAccessAuthorization.getAccessCode()));
  }

  private <T> Optional<T> readEntity(Response response, Class<T> clazz) {
    if (!response.hasEntity()) {
      return Optional.empty();
    }
    if (clazz.isInstance(response.getEntity())) {
      return Optional.of(clazz.cast(response.getEntity()));
    }
    throw new IllegalArgumentException("Response entity is not of type " + clazz.getName());
  }
}
