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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.fhir.de.value.KVNR;
import de.gematik.erezept.remotefdv.api.model.ConsentCategory;
import de.gematik.test.erezept.fhir.builder.eu.EuConsentBuilder;
import lombok.val;
import org.hl7.fhir.r4.model.Consent.ConsentState;
import org.junit.jupiter.api.Test;

class ConsentDataMapperTest {
  @Test
  void convertsErxConsentToConsentDto() {
    val consent =
        EuConsentBuilder.forKvnr(KVNR.from("X123456789")).status(ConsentState.ACTIVE).build();
    val result = ConsentDataMapper.from(consent);

    assertEquals(ConsentCategory.EUDISPCONS, result.getCategory());
    assertNotNull(result.getDateTime());
    assertTrue(result.getDateTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
  }

  @Test
  void handlesNullErxConsent() {
    assertThrows(NullPointerException.class, () -> ConsentDataMapper.from(null));
  }
}
