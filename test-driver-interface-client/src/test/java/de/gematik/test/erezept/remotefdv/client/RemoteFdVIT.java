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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;

@Slf4j
public class RemoteFdVIT {
  @Test
  @SneakyThrows
  void shouldRunAgainstLocalServer() {
    val client =
        RemoteFdVClient.builder().forRemote("http://localhost:8080").apiKey("apiKey2024").build();

    val response = client.sendRequest(PatientRequests.startFdV());
    log.info(response.getExpectedResource().toString());
    val response1 = client.sendRequest(PatientRequests.loginWithKvnr("X110645443"));
    log.info(response1.getExpectedResource().toString());
    val response2 = client.sendRequest(PatientRequests.getAuditEvents());
    log.info(response2.getExpectedResource().toString());
    val response3 = client.sendRequest(PatientRequests.getMedicationDispense("gt2025-01-01"));
    log.info(response3.getExpectedResource().toString());
    val response4 =
        client.sendRequest(
            PatientRequests.assignToPharmacy(
                "160.000.239.733.174.51", "3-1.54.10123404", "delivery"));
    log.info(response4.getExpectedResource().toString());
    val response5 =
        client.sendRequest(PatientRequests.generateDataMatrixCode("160.000.239.733.174.51"));
    log.info(response5.getExpectedResource().toString());
  }
}
