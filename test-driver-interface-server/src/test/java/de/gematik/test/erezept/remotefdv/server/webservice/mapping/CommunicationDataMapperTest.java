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

package de.gematik.test.erezept.remotefdv.server.webservice.mapping;

import static de.gematik.test.erezept.fhir.builder.GemFaker.fakerTelematikId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.utils.PrivateConstructorsUtil;
import de.gematik.test.erezept.fhir.parser.profiles.IStructureDefinition;
import de.gematik.test.erezept.fhir.resources.erp.CommunicationType;
import de.gematik.test.erezept.fhir.resources.erp.ErxCommunication;
import de.gematik.test.erezept.fhir.resources.erp.ICommunicationType;
import de.gematik.test.erezept.fhir.values.KVNR;
import de.gematik.test.erezept.fhir.values.PrescriptionId;
import de.gematik.test.erezept.fhir.values.TaskId;
import de.gematik.test.erezept.remotefdv.server.mapping.CommunicationDataMapper;
import java.util.Date;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import de.gematik.erezept.remotefdv.api.model.SupplyOptionsType;

class CommunicationDataMapperTest {
  private ErxCommunication erxCommunication;
  private IStructureDefinition def;

  @Test
  void shouldNotInstantiate() {
    assertTrue(PrivateConstructorsUtil.isUtilityConstructor(CommunicationDataMapper.class));
  }

  @BeforeEach
  public void setUp() {
    erxCommunication = mock(ErxCommunication.class);
    val comType = mock(ICommunicationType.class);
    def = mock(IStructureDefinition.class);
    when(erxCommunication.getUnqualifiedId()).thenReturn(UUID.randomUUID().toString());
    when(erxCommunication.getBasedOnReferenceId()).thenReturn(TaskId.from(PrescriptionId.random()));
    when(erxCommunication.getSenderId()).thenReturn(fakerTelematikId());
    when(erxCommunication.getRecipientId()).thenReturn(KVNR.random().getValue());
    when(erxCommunication.getSent()).thenReturn(new Date());
    when(erxCommunication.getMessage()).thenReturn("shipment");
    when(erxCommunication.getType()).thenReturn(comType);
    when(comType.getType()).thenReturn(def);
  }

  @ParameterizedTest
  @EnumSource(CommunicationType.class)
  void shouldCreateCommunicationWithType(CommunicationType type) {
    when(def.toString()).thenReturn(type.toString());
    val result = assertDoesNotThrow(() -> CommunicationDataMapper.from(erxCommunication));
    assertEquals(SupplyOptionsType.SHIPMENT, result.getSupplyOptionsType());
    assertEquals(type.name(), result.getType().name());
  }

  @Test
  void shouldCreateCommunicationWithSupplyOptionsTypeDelivery() {
    when(def.toString()).thenReturn("REPRESENTATIVE");
    when(erxCommunication.getMessage()).thenReturn("delivery");
    val result = assertDoesNotThrow(() -> CommunicationDataMapper.from(erxCommunication));
    assertEquals(SupplyOptionsType.DELIVERY, result.getSupplyOptionsType());
  }
}
