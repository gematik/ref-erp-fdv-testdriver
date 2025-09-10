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

package de.gematik.test.erezept.remotefdv.server.search;

import de.gematik.test.erezept.client.rest.param.IQueryParameter;
import de.gematik.test.erezept.client.rest.param.QueryParameter;
import de.gematik.test.erezept.client.rest.param.SortOrder;
import de.gematik.test.erezept.client.rest.param.SortParameter;
import de.gematik.test.erezept.client.usecases.MedicationDispenseGetCommand;
import java.util.LinkedList;
import java.util.List;

public class MedicationDispenseSearch {
  private MedicationDispenseSearch() {
    // do not instantiate
    throw new IllegalStateException("Utility class");
  }

  public static Builder searchFor() {
    return new Builder();
  }

  public static MedicationDispenseGetCommand getMedDispensesByHandedOver(String whenHandedOver) {
    return searchFor().whenHandedOver(whenHandedOver).sortedByCreationDate(SortOrder.DESCENDING);
  }

  public static class Builder {
    List<IQueryParameter> searchParams = new LinkedList<>();

    public Builder whenHandedOver(String date) {
      searchParams.add(new QueryParameter("whenhandedover", date));
      return this;
    }

    public MedicationDispenseGetCommand unsorted() {
      return new MedicationDispenseGetCommand(searchParams);
    }

    public MedicationDispenseGetCommand sortedByCreationDate(SortOrder order) {
      searchParams.add(new SortParameter("whenprepared", order));
      return unsorted();
    }
  }
}
