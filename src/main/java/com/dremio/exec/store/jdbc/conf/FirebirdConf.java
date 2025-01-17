/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
 */
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.validation.constraints.NotBlank;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.dremio.options.OptionManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dremio.services.credentials.CredentialsService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.yarn.webapp.example.MyApp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.Tag;

/**
 * Configuration for Firebird sources.
 */
@SourceType(value = "Firebird", label = "Firebird", uiConfig = "firebird-layout.json", externalQuerySupported = true)
public class FirebirdConf extends AbstractArpConf<FirebirdConf> {
  private static final Logger logger = LoggerFactory.getLogger(FirebirdConf.class);

  private static final String ARP_FILENAME = "arp/implementation/firebird-arp.yaml";

  private static final ArpDialect createDialect() {
    logger.debug("createDialect called");
    return AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  }

  private static final ArpDialect ARP_DIALECT = createDialect();
  private static final String DRIVER = "org.firebirdsql.jdbc";

  public FirebirdConf() {
    super();
    logger.debug("FirebirdConf constructor called with dialect " + ARP_DIALECT);
  }

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Database connection string")
  public String connectionString = "jdbc:firebirdsql://host.docker.internal:3050/test_dremio?user=SYSDBA&password=masterkey";

  @Tag(2)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;

  @Tag(3)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(4)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;

  @VisibleForTesting
  public String toJdbcConnectionString() {
    logger.debug("toJdbcConnectionString called");
    return this.connectionString;
  }

  @Override
  @VisibleForTesting
  public JdbcPluginConfig buildPluginConfig(
      JdbcPluginConfig.Builder configBuilder,
      CredentialsService credentialsService,
      OptionManager optionManager) {
    logger.debug("buildPluginConfig called");
    try {
      return configBuilder.withDialect(getDialect())
          .withDialect(getDialect())
          .withFetchSize(fetchSize)
          .withDatasourceFactory(this::newDataSource)
          .clearHiddenSchemas()
          .addHiddenSchema("SYSTEM")
          .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create FirebirdConf.", e);
    }
  }

  private CloseableDataSource newDataSource() {
    logger.debug("newDataSource called");
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
        toJdbcConnectionString(), null, null, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE,
        maxIdleConns, idleTimeSec);
  }

  @Override
  public ArpDialect getDialect() {
    logger.debug("getDialect called" + ARP_DIALECT);
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    logger.debug("getDialectSingleton called" + ARP_DIALECT);
    return ARP_DIALECT;
  }
}
