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

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.openapitools.model.Error;

@Getter
@Setter
public class FdVResponse<R> {
  private List<R> expectedResource;
  private Error operationOutcome;

  @SuppressWarnings("unchecked")
  public Optional<R> getResourceOptional() {
    val resource =
        expectedResource.isEmpty()
            ? Optional.empty()
            : Optional.ofNullable(expectedResource.get(0));
    return (Optional<R>) resource;
  }

  @SuppressWarnings("unchecked")
  public Optional<List<R>> getResourcesListOptional() {
    val resource = expectedResource.isEmpty() ? Optional.empty() : Optional.of(expectedResource);
    return (Optional<List<R>>) resource;
  }

  public R getResource() {
    return getResourceOptional().orElseThrow(() -> new RuntimeException("Missing Resource"));
  }

  public List<R> getResourcesList() {
    return getResourcesListOptional().orElseThrow(() -> new RuntimeException("Missing Resource"));
  }
}
