/*
 * Copyright 2026 gematik GmbH
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

public class DataMapperUtils {

  /**
   * ISO-8601 in UTC with a fixed number of fractional digits. ISO_OFFSET_DATE_TIME drops trailing
   * zeros, which renders every tenth timestamp with fewer than three of them.
   */
  private static final java.time.format.DateTimeFormatter UTC_MILLIS =
      java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  private DataMapperUtils() {
    throw new IllegalStateException("Do not instantiate utility class");
  }

  public static String formatToUTCString(java.util.Date date) {
    var instant = date.toInstant();
    var odt = instant.atOffset(java.time.ZoneOffset.UTC);
    return odt.format(UTC_MILLIS);
  }
}
