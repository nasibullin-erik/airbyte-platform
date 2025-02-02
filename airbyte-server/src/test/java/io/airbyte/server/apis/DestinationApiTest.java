/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.generated.CheckConnectionRead;
import io.airbyte.api.model.generated.DestinationCloneRequestBody;
import io.airbyte.api.model.generated.DestinationCreate;
import io.airbyte.api.model.generated.DestinationIdRequestBody;
import io.airbyte.api.model.generated.DestinationRead;
import io.airbyte.api.model.generated.DestinationReadList;
import io.airbyte.api.model.generated.DestinationSearch;
import io.airbyte.api.model.generated.DestinationUpdate;
import io.airbyte.api.model.generated.WorkspaceIdRequestBody;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.validation.json.JsonValidationException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@MicronautTest
@Requires(env = {Environment.TEST})
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class DestinationApiTest extends BaseControllerTest {

  @Test
  void testCheckConnectionToDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkDestinationConnectionFromDestinationId(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/check_connection";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCheckConnectionToDestinationForUpdate()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkDestinationConnectionFromDestinationIdForUpdate(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/check_connection_for_update";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationUpdate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationUpdate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCloneDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.cloneDestination(Mockito.any()))
        .thenReturn(new DestinationRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/clone";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationCloneRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationCloneRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCreateDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.createDestination(Mockito.any()))
        .thenReturn(new DestinationRead())
        .thenThrow(new ConstraintViolationException(new HashSet<>()));
    final String path = "/api/v1/destinations/create";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationCreate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationCreate()),
        HttpStatus.BAD_REQUEST);
  }

  @Test
  void testDeleteDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.doNothing()
        .doThrow(new ConfigNotFoundException("", ""))
        .when(destinationHandler).deleteDestination(Mockito.any(DestinationIdRequestBody.class));

    final String path = "/api/v1/destinations/delete";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NO_CONTENT);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetDestination() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.getDestination(Mockito.any()))
        .thenReturn(new DestinationRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/get";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testListDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.listDestinationsForWorkspace(Mockito.any()))
        .thenReturn(new DestinationReadList())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/list";
    testEndpointStatus(
        HttpRequest.POST(path, new WorkspaceIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new WorkspaceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testSearchDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.searchDestinations(Mockito.any()))
        .thenReturn(new DestinationReadList())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/search";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationSearch()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationSearch()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testUpdateDestination()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(destinationHandler.updateDestination(Mockito.any()))
        .thenReturn(new DestinationRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destinations/update";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationUpdate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationUpdate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testUpgradeDestinationVersion() throws IOException, JsonValidationException, ConfigNotFoundException {
    Mockito.doNothing()
        .doThrow(new ConfigNotFoundException("", ""))
        .when(destinationHandler).upgradeDestinationVersion(Mockito.any());
    final String path = "/api/v1/destinations/upgrade_version";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NO_CONTENT);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

}
