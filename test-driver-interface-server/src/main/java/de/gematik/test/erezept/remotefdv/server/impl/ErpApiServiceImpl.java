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

package de.gematik.test.erezept.remotefdv.server.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.rest.ErpResponse;
import de.gematik.test.erezept.client.rest.param.SortOrder;
import de.gematik.test.erezept.client.usecases.*;
import de.gematik.test.erezept.client.usecases.search.AuditEventSearch;
import de.gematik.test.erezept.client.usecases.search.TaskSearch;
import de.gematik.test.erezept.fhir.builder.erp.ErxCommunicationBuilder;
import de.gematik.test.erezept.fhir.extensions.erp.SupplyOptionsType;
import de.gematik.test.erezept.fhir.values.AccessCode;
import de.gematik.test.erezept.fhir.values.TaskId;
import de.gematik.test.erezept.fhir.values.json.CommunicationDisReqMessage;
import de.gematik.test.erezept.remotefdv.server.actors.Patient;
import de.gematik.test.erezept.remotefdv.server.config.MyConfigurationFactory;
import de.gematik.test.erezept.remotefdv.server.config.TestFdVFactory;
import de.gematik.test.erezept.remotefdv.server.mapping.*;
import de.gematik.test.erezept.remotefdv.server.search.MedicationDispenseSearch;
import de.gematik.test.erezept.screenplay.util.DataMatrixCodeGenerator;

import java.lang.System;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openapitools.api.ApiResponseMessage;
import org.openapitools.api.ErpApiService;
import org.openapitools.api.NotFoundException;
import org.openapitools.model.*;

@Slf4j
@javax.annotation.Generated(
    value = "io.swagger.codegen.v3.generators.java.JavaJerseyServerCodegen",
    date = "2024-05-24T08:38:25.868116771Z[GMT]")
public class ErpApiServiceImpl extends ErpApiService {
  private ErpClient erpClient;
  private Patient patient;
  private String accessToken;
  private Egk egk;
  private static String startTime;
  private MyConfigurationFactory mcf;

  public void setErpClient(ErpClient erpClient) {
    this.erpClient = erpClient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public Response.ResponseBuilder checkRequiredFields() {
    if (patient == null) {
      return Response.status(400)
          .entity(
              new ApiResponseMessage(
                  ApiResponseMessage.ERROR, "Failed - you need to start Test-FdV first"));
    }
    if (erpClient == null) {
      return Response.status(403)
          .entity(
              new ApiResponseMessage(
                  ApiResponseMessage.ERROR, "Forbidden - you need to login first"));
    }
    return Response.ok();
  }

  public Response buildOperationOutcomeDto(ErpResponse response) {
    val oo = response.getAsOperationOutcome();
    val dto = new OperationOutcome();
    dto.setStatusCode(BigDecimal.valueOf(response.getStatusCode()));
    dto.setDetails(oo.getIssueFirstRep().getDetails().getText());
    dto.setCode(OperationOutcome.CodeEnum.fromValue(oo.getIssueFirstRep().getCode().toCode()));
    dto.setDiagnostics(oo.getIssueFirstRep().getDiagnostics());
    return Response.status(response.getStatusCode()).entity(dto).build();
  }

  @Override
  public Response erpTestdriverApiV1AuditEventsGet(SecurityContext securityContext)
      throws NotFoundException {
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(AuditEventSearch.getAuditEvents());
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    val dtos =
        response.getExpectedResource().getAuditEvents().stream()
            .map(AuditEventDataMapper::from)
            .toList();
    return responseBuilder.entity(dtos).build();
  }

  @Override
  public Response erpTestdriverApiV1CommunicationGet(SecurityContext securityContext)
      throws NotFoundException {
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(new CommunicationGetCommand());
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    val dtos =
        response.getExpectedResource().getCommunications().stream()
            .map(CommunicationDataMapper::from)
            .toList();
    return responseBuilder.entity(dtos).build();
  }

  @Override
  public Response erpTestdriverApiV1CommunicationIdDelete(
      String id, SecurityContext securityContext) throws NotFoundException {
    if (id == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "CommunicationId is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(new CommunicationDeleteCommand(id));
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    return Response.status(204).build();
  }

  @Override
  public Response erpTestdriverApiV1InfoGet(SecurityContext securityContext)
      throws NotFoundException {
    val info = InformationBuilder.build(startTime);
    val activeEnv = mcf.getActiveEnvConfig().getName();
    val fdBaseUrl = mcf.getActiveEnvConfig().getInternet().getFdBaseUrl();

    val testEnvironment = new TestEnvironmentInfo();
    testEnvironment.setName(TestEnvironmentInfo.NameEnum.valueOf(activeEnv));
    testEnvironment.setServiceUrl(TestEnvironmentInfo.ServiceUrlEnum.fromValue(fdBaseUrl));
    info.setTestEnvironment(testEnvironment);
    return Response.ok().entity(info).build();
  }

  @Override
  public Response erpTestdriverApiV1LoginPut(String kvnr, SecurityContext securityContext)
      throws NotFoundException {
    if (patient == null) {
      return Response.status(400)
          .entity(
              new ApiResponseMessage(
                  ApiResponseMessage.ERROR, "Failed - you need to start Test-FdV first"))
          .build();
    }
    if (kvnr == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Request body is required"))
          .build();
    }
    String configPath = System.getenv("CONFIG_PATH");
    if (configPath == null) {
      configPath =
          "test-driver-interface-server/src/main/java/de/gematik/test/erezept/remotefdv/server/config/config.yaml";
    }
    try {
      mcf = TestFdVFactory.loadConfig(configPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    egk = mcf.getEgkByKvnr(kvnr);
    erpClient = mcf.createErpClientForPatient(kvnr, patient);
    erpClient.authenticateWith(egk);

    LoginSuccess loginSuccess = new LoginSuccess();
    accessToken = erpClient.getAuthentication().get().getAccessToken().getRawString();
    loginSuccess.setAccessToken(accessToken);
    val dtos = List.of(loginSuccess);
    return Response.ok().entity(dtos).build();
  }

  @Override
  public Response erpTestdriverApiV1MedicationdispenseGet(
      String whenhandedover, SecurityContext securityContext) throws NotFoundException {
    if (whenhandedover == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(
              new ApiResponseMessage(ApiResponseMessage.ERROR, "Date when handed over is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response =
        patient.erpRequest(MedicationDispenseSearch.getMedDispensesByHandedOver(whenhandedover));
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    val medDispenseBundle = response.getExpectedResource();
    val medDispenses = medDispenseBundle.getMedicationDispenses().stream().limit(50).toList();

    val kbvMedicationsDto =
        medDispenses.stream()
            .filter(md -> !(md.getContainedKbvMedication().isEmpty()))
            .flatMap(md -> medDispenseBundle.unpackDispensePairBy(md.getPrescriptionId()).stream())
            .map(MedicationDispenseDataMapper::fromKbvErpMedication)
            .toList();

    val gemMedicationDto =
        medDispenses.stream()
            .filter(md -> md.getContainedKbvMedication().isEmpty())
            .flatMap(md -> medDispenseBundle.getDispensePairBy(md.getPrescriptionId()).stream())
            .map(MedicationDispenseDataMapper::fromGemErpMedication)
            .toList();

    val dtos = new ArrayList<MedicationDispense>();
    dtos.addAll(kbvMedicationsDto);
    dtos.addAll(gemMedicationDto);
    return responseBuilder.entity(dtos).build();
  }

  @Override
  public Response erpTestdriverApiV1Pharmacy2dCodePost(String body, SecurityContext securityContext)
      throws NotFoundException {
    if (body == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "body is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    JSONObject jsonObject = new JSONObject(body);
    String taskId;
    try {
      taskId = jsonObject.get("taskId").toString();
    } catch (JSONException e) {
      return Response.status(400)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Entry taskId is required"))
          .build();
    }
    // send a TaskGetByIdCommand to get the access code of the given prescription
    val res = patient.erpRequest(new TaskGetByIdCommand(TaskId.from(taskId)));
    if (res.isOperationOutcome() || res.getStatusCode() > 299) {
      return buildOperationOutcomeDto(res);
    }
    val accessCode = res.getExpectedResource().getTask().getAccessCode().getValue();
    val baoDmc = DataMatrixCodeGenerator.writeToStream(taskId, AccessCode.fromString(accessCode));
    val encoded = Base64.getEncoder().encodeToString(baoDmc.toByteArray());
    return responseBuilder.entity(encoded).build();
  }

  @Override
  public Response erpTestdriverApiV1PharmacyAssignmentPost(
      Object body, SecurityContext securityContext) throws NotFoundException {
    if (body == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Body is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    Gson gson = new GsonBuilder().create();
    String json = gson.toJson(body);
    JSONObject object = new JSONObject(json);
    val dispReqMessage = new CommunicationDisReqMessage(SupplyOptionsType.DELIVERY, null);
    String taskId;
    String telematikId;
    try {
      taskId = object.get("prescriptionId").toString();
    } catch (JSONException e) {
      return Response.status(400)
          .entity(
              new ApiResponseMessage(ApiResponseMessage.ERROR, "Entry prescriptionId is required"))
          .build();
    }
    // send a TaskGetByIdCommand to get the access code of the given prescription
    val res = patient.erpRequest(new TaskGetByIdCommand(TaskId.from(taskId)));
    if (res.isOperationOutcome() || res.getStatusCode() > 299) {
      return buildOperationOutcomeDto(res);
    }
    val accessCode = res.getExpectedResource().getTask().getAccessCode().getValue();
    try {
      telematikId = object.get("telematikId").toString();
    } catch (JSONException e) {
      return Response.status(400)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Entry telematikId is required"))
          .build();
    }
    val erxCommunication =
        ErxCommunicationBuilder.builder()
            .basedOnTask(taskId, accessCode)
            .recipient(telematikId)
            .buildDispReq(dispReqMessage);

    val response = patient.erpRequest(new CommunicationPostCommand(erxCommunication));
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    val resource = response.getExpectedResource();
    val communication = CommunicationDataMapper.from(resource);
    return responseBuilder.entity(List.of(communication)).build();
  }

  @Override
  public Response erpTestdriverApiV1PharmacySearchGet(
      String near, String addressCity, String name, SecurityContext securityContext)
      throws NotFoundException {
    return Response.ok().build();
  }

  @Override
  public Response erpTestdriverApiV1PrescriptionGet(SecurityContext securityContext)
      throws NotFoundException {

    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(TaskSearch.getSortedByAuthoredOn(SortOrder.DESCENDING));
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    val dtos =
        response.getExpectedResource().getTasks().stream()
            .map(PrescriptionDataMapper::from)
            .toList();
    return responseBuilder.entity(dtos).build();
  }

  @Override
  public Response erpTestdriverApiV1PrescriptionIdDelete(String id, SecurityContext securityContext)
      throws NotFoundException {
    if (id == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "PrescriptionId is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(new TaskAbortCommand(TaskId.from(id)));
    if (response.isOperationOutcome() || response.getStatusCode() > 299) {
      return buildOperationOutcomeDto(response);
    }
    return Response.status(204).build();
  }

  @Override
  public Response erpTestdriverApiV1PrescriptionIdGet(String id, SecurityContext securityContext)
      throws NotFoundException {
    if (id == null) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "PrescriptionId is required"))
          .build();
    }
    val responseBuilder = checkRequiredFields();
    if (responseBuilder.build().getStatus() >= 400) {
      return responseBuilder.build();
    }
    val response = patient.erpRequest(new TaskGetByIdCommand(TaskId.from(id)));
    if (response.isOperationOutcome()) {
      return buildOperationOutcomeDto(response);
    }
    val kbvBundle = response.getExpectedResource().getKbvBundle().get();
    val prescription =
        PrescriptionDataMapper.from(response.getExpectedResource().getTask(), kbvBundle);
    return responseBuilder.entity(List.of(prescription)).build();
  }

  @Override
  public Response erpTestdriverApiV1StartPut(SecurityContext securityContext)
      throws NotFoundException {
    if (patient != null) {
      return Response.status(400)
          .entity(new ApiResponseMessage(ApiResponseMessage.ERROR, "Test-FdV is already running"))
          .build();
    }
    patient = new Patient();
    val zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    startTime = zonedDateTime.format(formatter);

    return Response.ok()
        .entity(new ApiResponseMessage(ApiResponseMessage.OK, "Test-FdV is running"))
        .build();
  }

  @Override
  public Response erpTestdriverApiV1StopPut(SecurityContext securityContext)
      throws NotFoundException {
    if (patient == null) {
      return Response.status(400)
          .entity(
              new ApiResponseMessage(ApiResponseMessage.ERROR, "Test-FdV is already not running"))
          .build();
    }
    patient = null;
    return Response.ok()
        .entity(new ApiResponseMessage(ApiResponseMessage.OK, "Test-FdV is not running"))
        .build();
  }
}
