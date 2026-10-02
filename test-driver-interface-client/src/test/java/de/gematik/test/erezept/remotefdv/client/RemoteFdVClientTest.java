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

package de.gematik.test.erezept.remotefdv.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.bbriccs.fhir.de.value.TelematikID;
import de.gematik.erezept.remotefdv.api.model.*;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.valuesets.IsoCountryCodeNCPeH;
import de.gematik.test.erezept.remotefdv.client.requests.FdVRequests;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteFdVClientTest {
  private static HttpClient mockHttpClient;
  private static RemoteFdVClient remoteFdVClient;

  @BeforeEach
  void setUp() {
    mockHttpClient = mock(HttpClient.class);
    remoteFdVClient =
        RemoteFdVClient.builder()
            .forRemote("http://localhost:8080")
            .apiKey("test_key")
            .withHttpClient(mockHttpClient)
            .build();
  }

  @Test
  @SneakyThrows
  void shouldThrowRemoteFdVErrorException() {
    when(mockHttpClient.send(any(), any()))
        .thenThrow(new InterruptedException("Simulated network error"));
    assertThrows(
        RemoteFdVErrorException.class,
        () -> remoteFdVClient.sendRequest(PatientRequests.startFdV()));
  }

  @Test
  @SneakyThrows
  void shouldReturnOperationOutcome() {
    val mockResponse = mock(HttpResponse.class);
    when(mockResponse.body()).thenReturn("Unknown Error");
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.startFdV()));
    // no resource is returned for error responses
    assertThrows(RemoteFdVErrorException.class, response::getExpectedResource);
    assertEquals(Optional.empty(), response.getResourceOptional());

    assertEquals(400, response.getOperationOutcome().getStatusCode().intValue());
  }

  @Test
  void shouldThrowOnInvalidRequestMethod() {
    val errorResponse = remoteFdVClient.sendRequest(new InvalidFdVRequest());
    // no expected resource for error responses
    assertThrows(RemoteFdVErrorException.class, errorResponse::getExpectedResourcesList);
    assertEquals(405, errorResponse.getOperationOutcome().getStatusCode().intValue());
  }

  @Test
  @SneakyThrows
  void shouldStartFdV() {
    val startMessage = "FdV started";
    val mockResponse = mock(HttpResponse.class);
    when(mockResponse.body()).thenReturn(startMessage);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.startFdV()));
    assertEquals(startMessage, response.getExpectedResource());
    assertEquals(startMessage, response.getExpectedResourcesList().getFirst());
  }

  @Test
  @SneakyThrows
  void shouldStopFdV() {
    val startMessage = "FdV stopped";
    val mockResponse = mock(HttpResponse.class);
    when(mockResponse.body()).thenReturn(startMessage);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response = assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.stopFdV()));
    assertEquals(startMessage, response.getExpectedResource());
  }

  @Test
  @SneakyThrows
  void shouldGetEuConsent() {
    val mockResponse = mock(HttpResponse.class);

    val objectMapper = new ObjectMapper();
    val consent =
        objectMapper.writeValueAsString(new Consent().category(ConsentCategory.EUDISPCONS));
    when(mockResponse.body()).thenReturn(consent);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.getConsent(ConsentCategory.EUDISPCONS)));
    assertEquals(ConsentCategory.EUDISPCONS, response.getExpectedResource().getCategory());
  }

  @Test
  @SneakyThrows
  void shouldPostEuConsent() {
    val mockResponse = mock(HttpResponse.class);

    val objectMapper = new ObjectMapper();
    val consent =
        objectMapper.writeValueAsString(new Consent().category(ConsentCategory.EUDISPCONS));
    when(mockResponse.body()).thenReturn(consent);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.postConsent(ConsentCategory.EUDISPCONS)));
    assertEquals(ConsentCategory.EUDISPCONS, response.getExpectedResource().getCategory());
  }

  @Test
  @SneakyThrows
  void shouldDeleteEuConsent() {
    val mockResponse = mock(HttpResponse.class);

    when(mockResponse.body()).thenReturn("");
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.deleteConsent(ConsentCategory.EUDISPCONS)));
    assertNull(response.getOperationOutcome());
  }

  @Test
  @SneakyThrows
  void shouldDeleteCommunicationById() {
    val mockResponse = mock(HttpResponse.class);

    when(mockResponse.body()).thenReturn("");
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);
    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.deleteCommunicationById(PrescriptionId.random().getValue())));
    assertNull(response.getOperationOutcome());
  }

  @Test
  @SneakyThrows
  void shouldDeleteEuAccessAuthorization() {
    val mockResponse = mock(HttpResponse.class);

    when(mockResponse.body()).thenReturn("");
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);
    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.deleteEuAccessAuth()));
    assertNull(response.getOperationOutcome());
  }

  @Test
  @SneakyThrows
  void shouldDeletePrescriptionById() {
    val mockResponse = mock(HttpResponse.class);

    when(mockResponse.body()).thenReturn("");
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);
    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.deletePrescriptionById(PrescriptionId.random().getValue())));
    assertNull(response.getOperationOutcome());
  }

  @Test
  @SneakyThrows
  void shouldGetAuditEvents() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val auditEvent = new AuditEvent();
    auditEvent.setPrescriptionId(prescriptionId.getValue());

    val objectMapper = new ObjectMapper();
    val auditEventJson = objectMapper.writeValueAsString(List.of(auditEvent));

    when(mockResponse.body()).thenReturn(auditEventJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.getAuditEvents()));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getPrescriptionId());
  }

  @Test
  @SneakyThrows
  void shouldGetCommunications() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val communication = new Communication();
    communication.setReference(prescriptionId.getValue());

    val objectMapper = new ObjectMapper();
    val communicationJson = objectMapper.writeValueAsString(List.of(communication));

    when(mockResponse.body()).thenReturn(communicationJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.getCommunication()));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getReference());
  }

  @Test
  @SneakyThrows
  void shouldGetEuAccessAuthorization() {
    val mockResponse = mock(HttpResponse.class);

    val euAccessAuth = new EUAccessAuthorization();
    euAccessAuth.setCountry(IsoCountryCodeNCPeH.DE.getCode());

    val objectMapper = new ObjectMapper();
    val euAccessAuthJson = objectMapper.writeValueAsString(List.of(euAccessAuth));

    when(mockResponse.body()).thenReturn(euAccessAuthJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.getEuAccessAuth()));
    assertEquals("DE", response.getExpectedResource().getCountry());
  }

  @Test
  @SneakyThrows
  void shouldGetEuMedicationDispense() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val euMedDispense = new EUMedicationDispense();
    euMedDispense.dispenseType(MedicationDispense.DispenseTypeEnum.EU);
    euMedDispense.setPrescriptionId(prescriptionId.getValue());

    val objectMapper = new ObjectMapper();
    val euMedDispenseJson = objectMapper.writeValueAsString(List.of(euMedDispense));

    when(mockResponse.body()).thenReturn(euMedDispenseJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(PatientRequests.getEuMedicationDispense("2026-09-23")));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getPrescriptionId());
    assertEquals(
        MedicationDispense.DispenseTypeEnum.EU, response.getExpectedResource().getDispenseType());
  }

  @Test
  @SneakyThrows
  void shouldGetMedicationDispense() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val medDispense = new DeMedicationDispense();
    medDispense.dispenseType(MedicationDispense.DispenseTypeEnum.DE);
    medDispense.setPrescriptionId(prescriptionId.getValue());

    val objectMapper = new ObjectMapper();
    val medDispenseJson = objectMapper.writeValueAsString(List.of(medDispense));

    when(mockResponse.body()).thenReturn(medDispenseJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () -> remoteFdVClient.sendRequest(PatientRequests.getMedicationDispense("2026-09-23")));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getPrescriptionId());
    assertEquals(
        MedicationDispense.DispenseTypeEnum.DE, response.getExpectedResource().getDispenseType());
  }

  @Test
  @SneakyThrows
  void shouldGetPrescriptions() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val prescription = new Prescription();
    prescription.setPrescriptionId(prescriptionId.getValue());

    val objectMapper = new ObjectMapper();
    val prescriptionJson = objectMapper.writeValueAsString(List.of(prescription));

    when(mockResponse.body()).thenReturn(prescriptionJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val prescriptionsResponse =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.getPrescriptions()));
    val prescriptionResponse =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.getPrescriptionById(prescriptionId.getValue())));
    assertEquals(
        prescriptionId.getValue(),
        prescriptionsResponse.getExpectedResourcesList().getFirst().getPrescriptionId());
    assertEquals(
        prescription.getPrescriptionId(),
        prescriptionResponse.getExpectedResource().getPrescriptionId());
  }

  @Test
  @SneakyThrows
  void shouldLogin() {
    val mockResponse = mock(HttpResponse.class);

    val login = new LoginSuccess();
    login.setAccessToken("test_access_token");

    val objectMapper = new ObjectMapper();
    val loginJson = objectMapper.writeValueAsString(List.of(login));

    when(mockResponse.body()).thenReturn(loginJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.loginWithKvnr(KVNR.randomStringValue())));
    assertEquals("test_access_token", response.getExpectedResource().getAccessToken());
  }

  @Test
  @SneakyThrows
  void shouldPatchPrescription() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val prescription = new Prescription();
    prescription.setPrescriptionId(prescriptionId.getValue());
    prescription.setEuRedeemableByPatient(true);

    val objectMapper = new ObjectMapper();
    val prescriptionJson = objectMapper.writeValueAsString(List.of(prescription));

    when(mockResponse.body()).thenReturn(prescriptionJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.patchPrescriptionById(prescriptionId.getValue(), true)));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getPrescriptionId());
    assertTrue(response.getExpectedResource().getEuRedeemableByPatient());
  }

  @Test
  @SneakyThrows
  void shouldAssignToPharmacy() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    val communication = new Communication();
    communication.setReference(prescriptionId.getValue());
    communication.setType(Communication.TypeEnum.DISP_REQ);

    val objectMapper = new ObjectMapper();
    val communicationJson = objectMapper.writeValueAsString(List.of(communication));

    when(mockResponse.body()).thenReturn(communicationJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.assignToPharmacy(
                        prescriptionId.getValue(), TelematikID.random().getValue(), "delivery")));
    assertEquals(prescriptionId.getValue(), response.getExpectedResource().getReference());
    assertEquals(Communication.TypeEnum.DISP_REQ, response.getExpectedResource().getType());
  }

  @Test
  @SneakyThrows
  void shouldPostEuAccessAuthorization() {
    val accessCode = AccessCode.random().getValue();
    val mockResponse = mock(HttpResponse.class);

    val euAccessAuth = new EUAccessAuthorization();
    euAccessAuth.setAccessCode(accessCode);
    euAccessAuth.setCountry(IsoCountryCodeNCPeH.DE.getCode());

    val objectMapper = new ObjectMapper();
    val euAccessAuthJson = objectMapper.writeValueAsString(List.of(euAccessAuth));

    when(mockResponse.body()).thenReturn(euAccessAuthJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () -> remoteFdVClient.sendRequest(PatientRequests.postEuAccessAuth("DE", accessCode)));
    assertEquals(accessCode, response.getExpectedResource().getAccessCode());
    assertEquals("DE", response.getExpectedResource().getCountry());
  }

  @Test
  @SneakyThrows
  void shouldGetInformation() {
    val mockResponse = mock(HttpResponse.class);

    val info = new Info();
    info.setTitle("Test Information of an FdV");

    val objectMapper = new ObjectMapper();
    val infoJson = objectMapper.writeValueAsString(List.of(info));

    when(mockResponse.body()).thenReturn(infoJson);
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(() -> remoteFdVClient.sendRequest(PatientRequests.getInformation()));
    assertNotNull(response.getExpectedResource());
  }

  @Test
  @SneakyThrows
  void shouldGenerate2dCode() {
    val prescriptionId = PrescriptionId.random();
    val mockResponse = mock(HttpResponse.class);

    when(mockResponse.body()).thenReturn("Test Data Matrix Code");
    when(mockHttpClient.send(any(), any())).thenReturn(mockResponse);

    val response =
        assertDoesNotThrow(
            () ->
                remoteFdVClient.sendRequest(
                    PatientRequests.generateDataMatrixCode(prescriptionId.getValue())));
    assertNotNull(response.getExpectedResource());
  }

  static class InvalidFdVRequest implements FdVRequests<String> {
    private final @Getter Class<String> type = String.class;
    private final @Getter TypeReference<List<String>> typeReference = new TypeReference<>() {};

    @Override
    public void finalizeRequest(HttpRequestInfo rb) {
      rb.setMethod("SOME_INVALID_METHOD");
      rb.setBody("some_invalid_body");
      rb.setResource("some_invalid_resource");
    }
  }
}
