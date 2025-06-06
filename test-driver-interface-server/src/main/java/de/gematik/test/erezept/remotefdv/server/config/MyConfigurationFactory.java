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

package de.gematik.test.erezept.remotefdv.server.config;

import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.bbriccs.smartcards.SmartcardArchive;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.cfg.ErpClientFactory;
import de.gematik.test.erezept.config.dto.ConfiguredFactory;
import de.gematik.test.erezept.config.dto.actor.PatientConfiguration;
import de.gematik.test.erezept.config.dto.erpclient.EnvironmentConfiguration;
import de.gematik.test.erezept.config.dto.primsys.PrimsysConfigurationDto;
import de.gematik.test.erezept.remotefdv.server.actors.Patient;
import de.gematik.test.erezept.remotefdv.server.exceptions.NoSuchEnvironmentException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class MyConfigurationFactory extends ConfiguredFactory {
  private final PrimsysConfigurationDto dto;
  private final SmartcardArchive sca = SmartcardArchive.fromResources();

  public MyConfigurationFactory(PrimsysConfigurationDto dto) {
    this.dto = dto;
  }

  public ErpClient createErpClientForPatient(String kvnr, Patient patient) {
    val envConfig = getActiveEnvConfig();
    val egk = getEgkByKvnr(kvnr);
    PatientConfiguration patientCfg = new PatientConfiguration();
    patientCfg.setEgkIccsn(egk.getIccsn());
    val erpClient = ErpClientFactory.createErpClient(envConfig, patientCfg);
    patient.setClient(erpClient);
    patient.setEgk(egk);
    return erpClient;
  }

  public EnvironmentConfiguration getActiveEnvConfig() {
    val activeEnv = System.getenv().getOrDefault("TI_ENV", dto.getActiveEnvironment());
    log.info("Active Environment: " + activeEnv);
    return dto.getEnvironments().stream()
        .filter(e -> e.getName().equalsIgnoreCase(activeEnv))
        .findAny()
        .orElseThrow(() -> new NoSuchEnvironmentException(activeEnv));
  }

  public Egk getEgkByKvnr(String kvnr) {
    return sca.getEgkByKvnr(kvnr);
  }
}
