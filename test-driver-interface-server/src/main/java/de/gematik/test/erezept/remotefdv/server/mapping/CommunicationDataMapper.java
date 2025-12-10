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

import de.gematik.erezept.remotefdv.api.model.Communication;
import de.gematik.erezept.remotefdv.api.model.SupplyOptionsType;
import de.gematik.test.erezept.fhir.r4.erp.ErxCommunication;
import de.gematik.test.erezept.fhir.r4.erp.ICommunicationType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommunicationDataMapper {

  public static Communication from(ErxCommunication resource) {
    val com = new Communication();
    com.setId(resource.getUnqualifiedId());
    com.setReference(resource.getBasedOnReferenceId().getValue());
    com.setSender(resource.getSenderId());
    com.setRecipient(resource.getRecipientId());
    com.setSent(DataMapperUtils.formatToUTCString(resource.getSent()));
    com.setSupplyOptionsType(determineSupplyOptionsType(resource.getMessage()));
    com.setType(determineCommunicationType(resource.getType()));
    return com;
  }

  private static SupplyOptionsType determineSupplyOptionsType(String message) {
    if (message.contains("onPremise")) {
      return SupplyOptionsType.ON_PREMISE;
    } else if (message.contains("shipment")) {
      return SupplyOptionsType.SHIPMENT;
    }
    return SupplyOptionsType.DELIVERY;
  }

  private static Communication.TypeEnum determineCommunicationType(ICommunicationType type) {
    if (type == null) {
      return Communication.TypeEnum.REPRESENTATIVE;
    }

    String typeString = type.getType().toString();
    if (typeString.contains("DISP_REQ")) {
      return Communication.TypeEnum.DISP_REQ;
    } else if (typeString.contains("INFO_REQ")) {
      return Communication.TypeEnum.INFO_REQ;
    } else if (typeString.contains("REPLY")) {
      return Communication.TypeEnum.REPLY;
    } else if (typeString.contains("DIGA")) {
      throw new IllegalArgumentException("DIGA communication type is not supported in Remote FDV");
    }
    return Communication.TypeEnum.REPRESENTATIVE;
  }
}
