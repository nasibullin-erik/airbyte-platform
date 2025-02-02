/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.generated.ActorCatalogWithUpdatedAt;
import io.airbyte.api.model.generated.CheckConnectionRead;
import io.airbyte.api.model.generated.DiscoverCatalogResult;
import io.airbyte.api.model.generated.SourceCloneRequestBody;
import io.airbyte.api.model.generated.SourceCreate;
import io.airbyte.api.model.generated.SourceDiscoverSchemaRead;
import io.airbyte.api.model.generated.SourceDiscoverSchemaRequestBody;
import io.airbyte.api.model.generated.SourceDiscoverSchemaWriteRequestBody;
import io.airbyte.api.model.generated.SourceIdRequestBody;
import io.airbyte.api.model.generated.SourceRead;
import io.airbyte.api.model.generated.SourceReadList;
import io.airbyte.api.model.generated.SourceSearch;
import io.airbyte.api.model.generated.SourceUpdate;
import io.airbyte.api.model.generated.WorkspaceIdRequestBody;
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
class SourceApiTest extends BaseControllerTest {

  @Test
  void testCheckConnectionToSource()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkSourceConnectionFromSourceId(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/check_connection";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCheckConnectionToSourceForUpdate()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.checkSourceConnectionFromSourceIdForUpdate(Mockito.any()))
        .thenReturn(new CheckConnectionRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/check_connection_for_update";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceUpdate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceUpdate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCloneSource() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.cloneSource(Mockito.any()))
        .thenReturn(new SourceRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/clone";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceCloneRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceCloneRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testCreateSource() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.createSourceWithOptionalSecret(Mockito.any()))
        .thenReturn(new SourceRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/create";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceCreate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceCreate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testDeleteSource() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.doNothing()
        .doThrow(new ConfigNotFoundException("", ""))
        .when(sourceHandler).deleteSource(Mockito.any(SourceIdRequestBody.class));

    final String path = "/api/v1/sources/delete";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NO_CONTENT);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testDiscoverSchemaForSource()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(schedulerHandler.discoverSchemaForSourceFromSourceId(Mockito.any()))
        .thenReturn(new SourceDiscoverSchemaRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/discover_schema";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceDiscoverSchemaRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceDiscoverSchemaRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetSource() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.getSource(Mockito.any()))
        .thenReturn(new SourceRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/get";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetMostRecentSourceActorCatalog() throws IOException {
    Mockito.when(sourceHandler.getMostRecentSourceActorCatalogWithUpdatedAt(Mockito.any()))
        .thenReturn(new ActorCatalogWithUpdatedAt());
    final String path = "/api/v1/sources/most_recent_source_actor_catalog";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.OK);
  }

  @Test
  void testListSourcesForWorkspace()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.listSourcesForWorkspace(Mockito.any()))
        .thenReturn(new SourceReadList())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/list";
    testEndpointStatus(
        HttpRequest.POST(path, new WorkspaceIdRequestBody()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new WorkspaceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testSearchSources() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.searchSources(Mockito.any()))
        .thenReturn(new SourceReadList())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/search";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceSearch()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceSearch()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testUpdateSources() throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(sourceHandler.updateSource(Mockito.any()))
        .thenReturn(new SourceRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/sources/update";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceUpdate()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceUpdate()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testWriteDiscoverCatalogResult() throws JsonValidationException, IOException {
    Mockito.when(sourceHandler.writeDiscoverCatalogResult(Mockito.any()))
        .thenReturn(new DiscoverCatalogResult());
    final String path = "/api/v1/sources/write_discover_catalog_result";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceDiscoverSchemaWriteRequestBody()),
        HttpStatus.OK);
  }

  @Test
  void testUpgradeSourceVersion() throws IOException, JsonValidationException, ConfigNotFoundException {
    Mockito.doNothing()
        .doThrow(new ConfigNotFoundException("", ""))
        .when(sourceHandler).upgradeSourceVersion(Mockito.any());
    final String path = "/api/v1/sources/upgrade_version";
    testEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NO_CONTENT);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new SourceIdRequestBody()),
        HttpStatus.NOT_FOUND);
  }

}
