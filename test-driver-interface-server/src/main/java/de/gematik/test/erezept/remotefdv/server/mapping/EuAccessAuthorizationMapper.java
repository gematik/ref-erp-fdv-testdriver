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

import de.gematik.erezept.remotefdv.api.model.EUAccessAuthorization;
import de.gematik.test.erezept.fhir.r4.eu.EuAccessPermission;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EuAccessAuthorizationMapper {
  public static EUAccessAuthorization from(EuAccessPermission permission) {
    val euAccessAuthDto = new EUAccessAuthorization();
    euAccessAuthDto.setAccessCode(permission.getAccessCode().getValue());
    euAccessAuthDto.setCountry(permission.getIsoCountryCode().getCode());
    val createdAt = permission.getCreateAt();
    createdAt.ifPresent(instant -> euAccessAuthDto.setCreatedAt(instant.toString()));
    val validUntil = permission.getValidUntil();
    validUntil.ifPresent(instant -> euAccessAuthDto.setValidUntil(instant.toString()));
    return euAccessAuthDto;
  }
}
