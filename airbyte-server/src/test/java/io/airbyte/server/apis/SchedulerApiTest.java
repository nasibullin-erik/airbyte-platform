/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.generated.CheckConnectionRead;
import io.airbyte.api.model.generated.DestinationCoreConfig;
import io.airbyte.api.model.generated.SourceCoreConfig;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.validation.json.JsonValidationException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@MicronautTest
@Requires(env = {Environment.TEST})
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class SchedulerApiTest extends BaseControllerTest {

  @Test
  void testExecuteDestinationCheckConnection()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkDestinationConnectionFromDestinationCreate(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/scheduler/destinations/check_connection";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationCoreConfig()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationCoreConfig()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testExecuteSourceCheckConnection()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkSourceConnectionFromSourceCreate(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/scheduler/sources/check_connection";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceCoreConfig()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceCoreConfig()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testExecuteSourceDiscoverSchema()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkSourceConnectionFromSourceCreate(Mockito.any()))
        .thenReturn(new CheckConnectionRead());
    final String path = "/api/v1/scheduler/sources/check_connection";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceCoreConfig()),
        HttpStatus.OK);
  }

}
