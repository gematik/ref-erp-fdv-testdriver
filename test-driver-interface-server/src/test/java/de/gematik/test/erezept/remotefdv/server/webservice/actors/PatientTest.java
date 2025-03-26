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

package de.gematik.test.erezept.remotefdv.server.webservice.actors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.rest.ErpResponse;
import de.gematik.test.erezept.client.usecases.TaskGetCommand;
import de.gematik.test.erezept.remotefdv.server.actors.Patient;
import lombok.val;
import org.junit.jupiter.api.Test;

class PatientTest {

  @Test
  void shouldNotThrow() {
    val patient = new Patient();
    ErpClient client = mock(ErpClient.class);
    val egk = mock(Egk.class);
    patient.setClient(client);
    patient.setEgk(egk);

    val erpResponse = mock(ErpResponse.class);
    val command = new TaskGetCommand();
    when(client.request(any())).thenReturn(erpResponse);
    assertDoesNotThrow(() -> patient.erpRequest(command));
  }
}
