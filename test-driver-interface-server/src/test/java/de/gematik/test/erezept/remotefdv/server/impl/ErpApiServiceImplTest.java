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

package de.gematik.test.erezept.remotefdv.server.impl;

import static de.gematik.test.erezept.fhir.builder.GemFaker.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.exceptions.FhirValidationException;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.erezept.remotefdv.api.model.*;
import de.gematik.idp.client.IdpTokenResult;
import de.gematik.idp.token.JsonWebToken;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.*;
import de.gematik.test.erezept.client.usecases.eu.*;
import de.gematik.test.erezept.config.dto.erpclient.BackendRouteConfiguration;
import de.gematik.test.erezept.config.dto.erpclient.EnvironmentConfiguration;
import de.gematik.test.erezept.fhir.builder.erp.ErxComDispReqBuilder;
import de.gematik.test.erezept.fhir.builder.erp.ErxCommunicationBuilder;
import de.gematik.test.erezept.fhir.builder.kbv.KbvErpBundleFaker;
import de.gematik.test.erezept.fhir.builder.kbv.KbvPatientFaker;
import de.gematik.test.erezept.fhir.parser.FhirParser;
import de.gematik.test.erezept.fhir.r4.erp.*;
import de.gematik.test.erezept.fhir.r4.eu.EuAccessPermission;
import de.gematik.test.erezept.fhir.r4.eu.EuConsent;
import de.gematik.test.erezept.fhir.r4.eu.EuConsentBundle;
import de.gematik.test.erezept.fhir.r4.kbv.KbvErpBundle;
import de.gematik.test.erezept.fhir.values.*;
import de.gematik.test.erezept.fhir.values.json.CommunicationDisReqMessage;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCodeNCPeH;
import de.gematik.test.erezept.fhir.valuesets.PrescriptionFlowType;
import de.gematik.test.erezept.remotefdv.server.actors.Patient;
import de.gematik.test.erezept.remotefdv.server.config.MyConfigurationFactory;
import de.gematik.test.erezept.remotefdv.server.config.TestFdVFactory;
import de.gematik.test.erezept.remotefdv.server.exceptions.NoSuchEnvironmentException;
import java.time.Instant;
import java.util.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import lombok.SneakyThrows;
import lombok.val;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ErpApiServiceImplTest {
  private static ErpClient mockClient;
  private static Egk mockEgk;
  private static Patient patient;
  private static ErpApiServiceImpl service;
  private static SecurityContext mockApiKey;
  private static FhirBResponse mockResponse;
  private static MyConfigurationFactory mockConfigFactory;

  @BeforeEach
  void setUp() {
    // basic setup
    mockEgk = mock(Egk.class);
    mockClient = mock(ErpClient.class);
    mockConfigFactory = mock(MyConfigurationFactory.class);
    patient = spy(Patient.class);
    service = new ErpApiServiceImpl();
    mockApiKey = mock(SecurityContext.class);
    service.setErpClient(mockClient);
    service.setPatient(patient);
    service.setEgk(mockEgk);
    service.setMcf(mockConfigFactory);
    patient.setClient(mockClient);

    // setup to test handling OperationOutcome
    val oo = mock(OperationOutcome.class);
    val issueComponent = new OperationOutcome.OperationOutcomeIssueComponent();
    issueComponent.setCode(OperationOutcome.IssueType.VALUE);
    issueComponent.setDetails(new CodeableConcept().setText("some details"));
    issueComponent.setDiagnostics("some diagnostics");
    when(oo.getIssueFirstRep()).thenReturn(issueComponent);
    mockResponse = mock(FhirBResponse.class);
    when(mockResponse.getAsOperationOutcome()).thenReturn(oo);
  }

  private static ValidationResult createEmptyValidationResult() {
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
        FhirBResponse.forPayload(ErxAuditEventBundle.class, bundle)
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
        FhirBResponse.forPayload(ErxCommunicationBundle.class, body)
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
        FhirBResponse.forPayload(EmptyResource.class, null)
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

  @Test
  @SneakyThrows
  void shouldLogin() {
    val kvnr = KVNR.random().getValue();

    val mockEgk = mock(Egk.class);
    when(mockEgk.getKvnr()).thenReturn(kvnr);
    val mockErpClient = mock(ErpClient.class);

    val backendRouteConfig = mockBackendRouteConfig();
    val envConfig = spy(EnvironmentConfiguration.class);
    envConfig.setName("RU");
    envConfig.setInternet(backendRouteConfig);

    try (val mockFdVFactory = mockStatic(TestFdVFactory.class)) {
      mockFdVFactory.when(() -> TestFdVFactory.loadConfig(any())).thenReturn(mockConfigFactory);

      when(mockConfigFactory.getEgkByKvnr(any())).thenReturn(mockEgk);
      when(mockConfigFactory.createErpClientForPatient(kvnr, patient)).thenReturn(mockErpClient);
      when(mockConfigFactory.getActiveEnvConfig()).thenReturn(envConfig);

      val mockIdpToken = mock(IdpTokenResult.class);
      when(mockIdpToken.getAccessToken()).thenReturn(new JsonWebToken("accessToken"));

      when(mockErpClient.getAuthentication()).thenReturn(() -> mockIdpToken);
      assertEquals(200, service.erpTestdriverApiV1LoginPut(kvnr, mockApiKey).getStatus());
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowNoSuchEnvironmentExceptionOnLogin() {
    val kvnr = KVNR.random().getValue();

    try (val mockFdVFactory = mockStatic(TestFdVFactory.class)) {
      val mockEgk = mock(Egk.class);

      mockFdVFactory.when(() -> TestFdVFactory.loadConfig(any())).thenReturn(mockConfigFactory);
      when(mockConfigFactory.getEgkByKvnr(any())).thenReturn(mockEgk);
      when(mockConfigFactory.createErpClientForPatient(kvnr, patient))
          .thenThrow(new NoSuchEnvironmentException("No such environment"));
      assertEquals(400, service.erpTestdriverApiV1LoginPut(kvnr, mockApiKey).getStatus());
    }
  }

  @Test
  @SneakyThrows
  void shouldThrowOnLogin() {
    val kvnr = KVNR.random().getValue();
    try (val mockFdVFactory = mockStatic(TestFdVFactory.class)) {
      val mockEgk = mock(Egk.class);

      mockFdVFactory.when(() -> TestFdVFactory.loadConfig(any())).thenReturn(mockConfigFactory);
      when(mockConfigFactory.getEgkByKvnr(any())).thenReturn(mockEgk);
      when(mockConfigFactory.createErpClientForPatient(kvnr, patient))
          .thenThrow(new IllegalArgumentException("Invalid argument"));
      assertEquals(500, service.erpTestdriverApiV1LoginPut(kvnr, mockApiKey).getStatus());
    }
  }

  @Test
  void shouldGetInfo() {
    val envConfig = spy(EnvironmentConfiguration.class);
    envConfig.setName("RU");

    val backendRouteConfig = mockBackendRouteConfig();
    when(mockConfigFactory.getActiveEnvConfig()).thenReturn(envConfig);
    when(envConfig.getInternet()).thenReturn(backendRouteConfig);
    Assertions.assertDoesNotThrow(() -> service.erpTestdriverApiV1InfoGet(mockApiKey));
  }

  @Test
  @SneakyThrows
  void shouldNotGetInfoWithoutPatient() {
    service.setPatient(null);
    assertEquals(400, service.erpTestdriverApiV1InfoGet(mockApiKey).getStatus());
  }

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
        FhirBResponse.forPayload(ErxMedicationDispenseBundle.class, bundle)
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

  @Test
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
    val kbvErpBundle = mock(KbvErpBundle.class);
    val erxTask = mock(ErxTask.class);
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.ACCEPTED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(prescriptionBundle.getTask()).thenReturn(erxTask);
    when(prescriptionBundle.getKbvBundle()).thenReturn(Optional.of(kbvErpBundle));
    val erpResponse =
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1Pharmacy2dCodePost(body, mockApiKey));
    assertEquals(200, r.getStatus());
  }

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
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
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
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
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

  @Test
  void shouldAssignToPharmacy() {
    val prescriptionId = PrescriptionId.random();
    val telematikId = fakerTelematikId();
    val accessCode = AccessCode.random();

    Map<String, String> map = new HashMap<>();
    map.put("prescriptionId", prescriptionId.getValue());
    map.put("telematikId", telematikId);

    val prescriptionBundle = mock(ErxPrescriptionBundle.class);
    val erxTask = mock(ErxTask.class);

    when(erxTask.getAccessCode()).thenReturn(accessCode);
    when(prescriptionBundle.getTask()).thenReturn(erxTask);

    val erpResponse =
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);

    val mockComBuilder = mockStatic(ErxCommunicationBuilder.class);
    val mockDispReqBuilder = mock(ErxComDispReqBuilder.class);
    mockComBuilder
        .when(
            () -> ErxCommunicationBuilder.forDispenseRequest(any(CommunicationDisReqMessage.class)))
        .thenReturn(mockDispReqBuilder);

    when(mockDispReqBuilder.basedOn(prescriptionId.getValue(), accessCode.getValue()))
        .thenReturn(mockDispReqBuilder);
    when(mockDispReqBuilder.receiver(telematikId)).thenReturn(mockDispReqBuilder);
    when(mockDispReqBuilder.flowType(any())).thenReturn(mockDispReqBuilder);
    when(mockDispReqBuilder.version(any())).thenReturn(mockDispReqBuilder);

    val com = mock(ErxCommunication.class);
    when(com.getUnqualifiedId()).thenReturn(UUID.randomUUID().toString());
    when(com.getBasedOnReferenceId()).thenReturn(TaskId.from(prescriptionId));
    when(com.getSenderId()).thenReturn(KVNR.random().getValue());
    when(com.getRecipientId()).thenReturn(fakerTelematikId());
    when(com.getSent()).thenReturn(new Date());
    when(com.getMessage()).thenReturn(SupplyOptionsType.ON_PREMISE.toString());

    when(mockDispReqBuilder.build()).thenReturn(com);

    val erpResponse2 =
        FhirBResponse.forPayload(ErxCommunication.class, com)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(CommunicationPostCommand.class))).thenReturn(erpResponse2);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1PharmacyAssignmentPost(map, mockApiKey));
    assertEquals(200, r.getStatus());
  }

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
        FhirBResponse.forPayload(ErxTaskBundle.class, bundle)
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
        FhirBResponse.forPayload(EmptyResource.class, null)
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

  @Test
  void shouldReturnCancelledPrescriptionWhenTaskWasAlreadyDeleted() {
    val prescriptionId = PrescriptionId.random();
    val searchResponse = taskSearchResponseFor(cancelledTask(prescriptionId));
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockResponse.getStatusCode()).thenReturn(410);
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(mockResponse);
    when(mockClient.request(any(TaskGetCommand.class))).thenReturn(searchResponse);

    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdGet(prescriptionId.getValue(), mockApiKey));

    assertEquals(200, r.getStatus());
    assertEquals(Prescription.StatusEnum.CANCELLED, ((Prescription) r.getEntity()).getStatus());
  }

  @Test
  void shouldReturnOperationOutcomeWhenDeletedTaskIsNotInTaskSearch() {
    val searchResponse = taskSearchResponseFor(cancelledTask(PrescriptionId.random()));
    when(mockResponse.isOperationOutcome()).thenReturn(true);
    when(mockResponse.getStatusCode()).thenReturn(410);
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(mockResponse);
    when(mockClient.request(any(TaskGetCommand.class))).thenReturn(searchResponse);

    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdGet(
                    PrescriptionId.random().getValue(), mockApiKey));

    assertEquals(410, r.getStatus());
  }

  @Test
  void shouldGetPrescriptionById() {
    val prescriptionBundle = mock(ErxPrescriptionBundle.class);

    val fhirParser = new FhirParser(); // the next line fails without this
    val kbvErpBundle = KbvErpBundleFaker.builder().fake();

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
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erpResponse);
    val r =
        Assertions.assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdGet(prescriptionId.getValue(), mockApiKey));
    assertEquals(200, r.getStatus());
  }

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

  @Test
  void shouldNotPostConsentWithoutPatient() {
    service.setPatient(null);
    val r =
        Assertions.assertDoesNotThrow(
            () -> service.erpTestdriverApiV1ConsentPost(ConsentCategory.EUDISPCONS, mockApiKey));
    assertEquals(400, r.getStatus());
  }

  @Test
  void shouldPostEuConsent() {
    val response = postConsent(new EuConsent(), ConsentCategory.EUDISPCONS);
    assertEquals(201, response.getStatus());
  }

  @Test
  void shouldPostErxConsent() {
    val response = postConsent(new ErxConsent(), ConsentCategory.CHARGCONS);
    assertEquals(201, response.getStatus());
  }

  @Test
  void shouldNotDeleteConsentWithoutPatient() {
    service.setPatient(null);
    val response = deleteConsent(ConsentCategory.EUDISPCONS);
    assertEquals(400, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldFailDeletingConsent() {
    val oo = (FhirBResponse<EmptyResource>) buildOperationOutcome();
    when(mockClient.request(any(EuConsentDeleteCommand.class))).thenReturn(oo);
    when(mockClient.request(any(ConsentDeleteCommand.class))).thenReturn(oo);

    val euResponse =
        service.erpTestdriverApiV1ConsentDelete(ConsentCategory.EUDISPCONS, mockApiKey);
    val erxResponse =
        service.erpTestdriverApiV1ConsentDelete(ConsentCategory.CHARGCONS, mockApiKey);
    assertEquals(400, euResponse.getStatus());
    assertEquals(400, erxResponse.getStatus());
  }

  @Test
  void shouldDeleteEuConsent() {
    val response = deleteConsent(ConsentCategory.EUDISPCONS);
    assertEquals(204, response.getStatus());
  }

  @Test
  void shouldDeleteErxConsent() {
    val response = deleteConsent(ConsentCategory.CHARGCONS);
    assertEquals(204, response.getStatus());
  }

  @Test
  void shouldNotFindAnyConsent() {
    val emptyEuConsentBundle = mock(EuConsentBundle.class);
    val emptyErxConsentBundle = mock(ErxConsentBundle.class);
    when(emptyEuConsentBundle.getConsent()).thenReturn(Optional.empty());
    when(emptyErxConsentBundle.getConsent()).thenReturn(Optional.empty());

    val euResponse = getConsent(emptyEuConsentBundle, ConsentCategory.EUDISPCONS);
    val erxResponse = getConsent(emptyErxConsentBundle, ConsentCategory.CHARGCONS);
    assertEquals(404, euResponse.getStatus());
    assertEquals(404, erxResponse.getStatus());
  }

  @Test
  void shouldGetEuConsent() {
    val mockEuConsent = mock(EuConsent.class);
    val mockConsentBundle = mock(EuConsentBundle.class);
    when(mockConsentBundle.getConsent()).thenReturn(Optional.of(mockEuConsent));

    val cc =
        new CodeableConcept()
            .setCoding(List.of(new Coding().setCode(ConsentCategory.EUDISPCONS.getValue())));
    val fhirParser = new FhirParser(); // the next line fails without this
    val kbvPatient = KbvPatientFaker.builder().fake();

    when(mockEuConsent.getPatient()).thenReturn(kbvPatient.asReference());
    when(mockEuConsent.getCategoryFirstRep()).thenReturn(cc);
    when(mockEuConsent.getDateTime()).thenReturn(new Date());

    val response = getConsent(mockConsentBundle, ConsentCategory.EUDISPCONS);
    assertEquals(200, response.getStatus());
  }

  @Test
  void shouldGetErxConsent() {
    val mockErxConsent = mock(ErxConsent.class);
    val mockConsentBundle = mock(ErxConsentBundle.class);
    when(mockConsentBundle.getConsent()).thenReturn(Optional.of(mockErxConsent));

    val cc =
        new CodeableConcept()
            .setCoding(List.of(new Coding().setCode(ConsentCategory.CHARGCONS.getValue())));
    val fhirParser = new FhirParser(); // the next line fails without this
    val kbvPatient = KbvPatientFaker.builder().fake();

    when(mockErxConsent.getPatient()).thenReturn(kbvPatient.asReference());
    when(mockErxConsent.getCategoryFirstRep()).thenReturn(cc);
    when(mockErxConsent.getDateTime()).thenReturn(new Date());

    val response = getConsent(mockConsentBundle, ConsentCategory.CHARGCONS);
    assertEquals(200, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldNotDeleteEuAccessAuthorizationWithoutPatient() {
    service.setPatient(null);
    val response = service.erpTestdriverApiV1EuAccessAuthorizationDelete(mockApiKey);
    assertEquals(400, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldFailDeletingEuAccessAuthorization() {
    val oo = (FhirBResponse<EmptyResource>) buildOperationOutcome();
    when(mockClient.request(any(EuGrantAccessDeleteCommand.class))).thenReturn(oo);

    val response = service.erpTestdriverApiV1EuAccessAuthorizationDelete(mockApiKey);
    assertEquals(400, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldDeleteEuAccessAuthorization() {
    val erpResponse =
        FhirBResponse.forPayload(EmptyResource.class, new EmptyResource())
            .withStatusCode(204)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());
    when(mockClient.request(any(EuGrantAccessDeleteCommand.class))).thenReturn(erpResponse);
    val response = service.erpTestdriverApiV1EuAccessAuthorizationDelete(mockApiKey);
    assertEquals(204, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldNotGetEuAccessAuthorizationWithoutPatient() {
    service.setPatient(null);
    val response = service.erpTestdriverApiV1EuAccessAuthorizationGet(mockApiKey);
    assertEquals(400, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldFailGettingEuAccessAuthorization() {
    val oo = (FhirBResponse<EuAccessPermission>) buildOperationOutcome();
    when(mockClient.request(any(EuGrantAccessGetCommand.class))).thenReturn(oo);

    val response = service.erpTestdriverApiV1EuAccessAuthorizationGet(mockApiKey);
    assertEquals(400, response.getStatus());
  }

  @Test
  @SneakyThrows
  void shouldGetEuAccessAuthorization() {
    val accessCode = EuAccessCode.random();
    val euAccessPermission = mock(EuAccessPermission.class);
    when(euAccessPermission.getIsoCountryCode()).thenReturn(IsoCountryCodeNCPeH.LT);
    when(euAccessPermission.getAccessCode()).thenReturn(accessCode);
    when(euAccessPermission.getCreateAt()).thenReturn(Optional.of(Instant.now()));
    when(euAccessPermission.getValidUntil()).thenReturn(Optional.of(Instant.now()));

    val erpResponse =
        FhirBResponse.forPayload(EuAccessPermission.class, euAccessPermission)
            .withStatusCode(201)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(EuGrantAccessGetCommand.class))).thenReturn(erpResponse);
    val euAccessAuthorization =
        readEntity(
                service.erpTestdriverApiV1EuAccessAuthorizationGet(mockApiKey),
                EUAccessAuthorization.class)
            .orElseThrow();
    assertEquals(accessCode, EuAccessCode.from(euAccessAuthorization.getAccessCode()));
  }

  @SneakyThrows
  @Test
  void shouldPostEuAccessAuthorization() {
    val accessCode = EuAccessCode.random();
    val euAccessPermission = mock(EuAccessPermission.class);
    when(euAccessPermission.getIsoCountryCode()).thenReturn(IsoCountryCodeNCPeH.LT);
    when(euAccessPermission.getAccessCode()).thenReturn(accessCode);
    when(euAccessPermission.getCreateAt()).thenReturn(Optional.of(Instant.now()));
    when(euAccessPermission.getValidUntil()).thenReturn(Optional.of(Instant.now()));

    val erpResponse =
        FhirBResponse.forPayload(EuAccessPermission.class, euAccessPermission)
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

  @Test
  void shouldPatchPrescriptionAndReturnItWithKbvBundleInformation() {
    val prescriptionId = PrescriptionId.random();
    val prescriptionBundle = mock(ErxPrescriptionBundle.class);

    val fhirParser = new FhirParser(); // the next line fails without this
    val kbvErpBundle = KbvErpBundleFaker.builder().fake();
    val erxTask = mock(ErxTask.class);

    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.COMPLETED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(erxTask.getPrescriptionId()).thenReturn(prescriptionId);
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getExpiryDate()).thenReturn(new Date());
    when(erxTask.getAuthoredOn()).thenReturn(new Date());

    when(prescriptionBundle.getTask()).thenReturn(erxTask);
    when(prescriptionBundle.getKbvBundle()).thenReturn(Optional.of(kbvErpBundle));

    val patchRequest = new ErpTestdriverApiV1PrescriptionIdPatchRequest();
    patchRequest.setEuRedeemableByPatient(true);

    val erxTaskResponse =
        FhirBResponse.forPayload(ErxTask.class, erxTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    val erxPresBundleResponse =
        FhirBResponse.forPayload(ErxPrescriptionBundle.class, prescriptionBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(TaskPatchCommand.class))).thenReturn(erxTaskResponse);
    when(mockClient.request(any(TaskGetByIdCommand.class))).thenReturn(erxPresBundleResponse);

    val response =
        assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdPatch(
                    prescriptionId.getValue(), patchRequest, mockApiKey));
    assertEquals(200, response.getStatus());
  }

  @Test
  void shouldPatchPrescriptionAndReturnItWithoutKbvBundleInformation() {
    val prescriptionId = PrescriptionId.random();
    val erxTask = mock(ErxTask.class);

    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.COMPLETED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAccessCode()).thenReturn(AccessCode.random());
    when(erxTask.getPrescriptionId()).thenReturn(prescriptionId);
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getExpiryDate()).thenReturn(new Date());
    when(erxTask.getAuthoredOn()).thenReturn(new Date());

    val patchRequest = new ErpTestdriverApiV1PrescriptionIdPatchRequest();
    patchRequest.setEuRedeemableByPatient(true);

    val erxTaskResponse =
        FhirBResponse.forPayload(ErxTask.class, erxTask)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(TaskPatchCommand.class))).thenReturn(erxTaskResponse);
    when(mockClient.request(any(TaskGetByIdCommand.class)))
        .thenThrow(new FhirValidationException("Could not read the KbvBundle of Prescription"));

    val response =
        assertDoesNotThrow(
            () ->
                service.erpTestdriverApiV1PrescriptionIdPatch(
                    prescriptionId.getValue(), patchRequest, mockApiKey));
    assertEquals(200, response.getStatus());
  }

  private static ErxTask cancelledTask(PrescriptionId prescriptionId) {
    val erxTask = mock(ErxTask.class);
    when(erxTask.getPrescriptionId()).thenReturn(prescriptionId);
    when(erxTask.getStatus()).thenReturn(Task.TaskStatus.CANCELLED);
    when(erxTask.getFlowType()).thenReturn(PrescriptionFlowType.FLOW_TYPE_160);
    when(erxTask.getAuthoredOn()).thenReturn(new Date());
    when(erxTask.getAcceptDate()).thenReturn(new Date());
    when(erxTask.getExpiryDate()).thenReturn(new Date());
    return erxTask;
  }

  private static FhirBResponse<ErxTaskBundle> taskSearchResponseFor(ErxTask task) {
    val bundle = mock(ErxTaskBundle.class);
    when(bundle.getTasks()).thenReturn(List.of(task));
    return FhirBResponse.forPayload(ErxTaskBundle.class, bundle)
        .withStatusCode(200)
        .withHeaders(Map.of())
        .andValidationResult(createEmptyValidationResult());
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

  private static BackendRouteConfiguration mockBackendRouteConfig() {
    val backendRouteConfig = new BackendRouteConfiguration();
    backendRouteConfig.setFdBaseUrl("https://erp-dev.app.ti-dienste.de");
    return backendRouteConfig;
  }

  @SneakyThrows
  private static Response postConsent(Consent consent, ConsentCategory category) {
    when(mockEgk.getKvnr()).thenReturn(KVNR.random().getValue());

    val fhirParser = new FhirParser(); // the next line fails without this
    val kbvPatient = KbvPatientFaker.builder().fake();
    val cc = new CodeableConcept().setCoding(List.of(new Coding().setCode(category.getValue())));
    val mockEuConsent = mock(consent.getClass());
    when(mockEuConsent.getPatient()).thenReturn(kbvPatient.asReference());
    when(mockEuConsent.getCategoryFirstRep()).thenReturn(cc);
    when(mockEuConsent.getDateTime()).thenReturn(new Date());

    val erpResponse =
        FhirBResponse.forPayload(consent.getClass(), mockEuConsent)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(EuConsentPostCommand.class)))
        .thenReturn((FhirBResponse<EuConsent>) erpResponse);
    when(mockClient.request(any(ConsentPostCommand.class)))
        .thenReturn((FhirBResponse<ErxConsent>) erpResponse);
    return service.erpTestdriverApiV1ConsentPost(category, mockApiKey);
  }

  @SneakyThrows
  private static Response getConsent(Bundle consentBundle, ConsentCategory category) {
    val erpResponse =
        FhirBResponse.forPayload(consentBundle.getClass(), consentBundle)
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(EuConsentGetCommand.class)))
        .thenReturn((FhirBResponse<EuConsentBundle>) erpResponse);
    when(mockClient.request(any(ConsentGetCommand.class)))
        .thenReturn((FhirBResponse<ErxConsentBundle>) erpResponse);
    return service.erpTestdriverApiV1ConsentGet(category, mockApiKey);
  }

  @SneakyThrows
  private static Response deleteConsent(ConsentCategory category) {
    val erpResponse =
        FhirBResponse.forPayload(EmptyResource.class, new EmptyResource())
            .withStatusCode(200)
            .withHeaders(Map.of())
            .andValidationResult(createEmptyValidationResult());

    when(mockClient.request(any(EuConsentDeleteCommand.class))).thenReturn(erpResponse);
    when(mockClient.request(any(ConsentDeleteCommand.class))).thenReturn(erpResponse);
    return service.erpTestdriverApiV1ConsentDelete(category, mockApiKey);
  }

  private static FhirBResponse<? extends Resource> buildOperationOutcome() {
    val oo = new OperationOutcome();
    oo.addIssue()
        .setCode(OperationOutcome.IssueType.INVALID)
        .setDetails(new CodeableConcept().setText("Unknown Error"));

    val mockFhirBResponse = mock(FhirBResponse.class);
    when(mockFhirBResponse.isOperationOutcome()).thenReturn(true);
    when(mockFhirBResponse.getAsOperationOutcome()).thenReturn(oo);
    when(mockFhirBResponse.getStatusCode()).thenReturn(400);
    return mockFhirBResponse;
  }
}
