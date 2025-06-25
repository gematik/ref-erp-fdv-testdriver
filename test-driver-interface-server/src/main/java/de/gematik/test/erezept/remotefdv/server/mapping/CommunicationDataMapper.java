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

import de.gematik.test.erezept.fhir.resources.erp.ErxCommunication;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.val;
import de.gematik.erezept.remotefdv.api.model.Communication;
import de.gematik.erezept.remotefdv.api.model.SupplyOptionsType;

public class CommunicationDataMapper {
  private CommunicationDataMapper() {
    // hide constructor
    throw new IllegalAccessError("Utility class");
  }

  public static Communication from(ErxCommunication resource) {
    val com = new Communication();
    com.setId(resource.getUnqualifiedId());
    com.setReference(resource.getBasedOnReferenceId().getValue());
    com.setSender(resource.getSenderId());
    com.setRecipient(resource.getRecipientId());
    val instant = resource.getSent().toInstant();
    val odt = instant.atOffset(ZoneOffset.UTC);
    val sentUTC = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    com.setSent(sentUTC);
    SupplyOptionsType supplyOptionsType = SupplyOptionsType.DELIVERY;
    if (resource.getMessage().contains("onPremise")) {
      supplyOptionsType = SupplyOptionsType.ON_PREMISE;
    } else if (resource.getMessage().contains("shipment")) {
      supplyOptionsType = SupplyOptionsType.SHIPMENT;
    }
    com.setSupplyOptionsType(supplyOptionsType);

    Communication.TypeEnum type = Communication.TypeEnum.REPRESENTATIVE;

    if (resource.getType() != null) {
      if (resource.getType().getType().toString().contains("DISP_REQ")) {
        type = Communication.TypeEnum.DISP_REQ;
      } else if (resource.getType().getType().toString().contains("INFO_REQ")) {
        type = Communication.TypeEnum.INFO_REQ;
      } else if (resource.getType().getType().toString().contains("REPLY")) {
        type = Communication.TypeEnum.REPLY;
      }
      com.setType(type);
    }
    return com;
  }
}
