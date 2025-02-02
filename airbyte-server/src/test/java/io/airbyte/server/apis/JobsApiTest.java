/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.generated.JobCreate;
import io.airbyte.api.model.generated.JobDebugInfoRead;
import io.airbyte.api.model.generated.JobIdRequestBody;
import io.airbyte.api.model.generated.JobInfoRead;
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
class JobsApiTest extends BaseControllerTest {

  @Test
  void testCreateJob() throws IOException, JsonValidationException, ConfigNotFoundException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.createJob(Mockito.any()))
        .thenReturn(new JobInfoRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/jobs/create";
    testEndpointStatus(
        HttpRequest.POST(path, new JobCreate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new JobCreate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCancelJob() throws IOException {
    Mockito.when(schedulerHandler.cancelJob(Mockito.any()))
        .thenReturn(new JobInfoRead());
    final String path = "/api/v1/jobs/cancel";
    testEndpointStatus(
        HttpRequest.POST(path, new JobIdRequestBody()),
        HttpStatus.OK);
  }

  @Test
  void testGetJobDebugInfo()
      throws IOException, JsonValidationException, ConfigNotFoundException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(jobHistoryHandler.getJobDebugInfo(Mockito.any()))
        .thenReturn(new JobDebugInfoRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/jobs/get_debug_info";
    testEndpointStatus(
        HttpRequest.POST(path, new JobIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new JobIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

}
