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

import org.openapitools.model.Info;
import org.openapitools.model.TestFdV;

public class InformationBuilder {
  private InformationBuilder() {
    // hide constructor
    throw new IllegalAccessError("Utility class");
  }

  public static Info build(String startTime) {
    Info info = new Info();
    info.setStartTime(startTime);
    info.setTitle("Test driver interface of ERP Test-FdV Module");
    info.setTestDriverVersion("1.1.0");

    TestFdV testFdV = new TestFdV();
    testFdV.setPlatform(TestFdV.PlatformEnum.ANDROID);
    testFdV.setPlatformVersion("14.0.0");
    testFdV.isEmulated(true);
    testFdV.status(TestFdV.StatusEnum.RUNNING);
    testFdV.setStartTime(startTime);
    info.setTestFdV(testFdV);
    return info;
  }
}
