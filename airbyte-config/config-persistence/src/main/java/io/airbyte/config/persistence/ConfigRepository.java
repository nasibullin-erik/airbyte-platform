/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.config.persistence;

import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import io.airbyte.config.ActorDefinitionBreakingChange;
import io.airbyte.config.ActorDefinitionConfigInjection;
import io.airbyte.config.ActorDefinitionVersion;
import io.airbyte.config.ConfigSchema;
import io.airbyte.config.DestinationConnection;
import io.airbyte.config.Geography;
import io.airbyte.config.SourceConnection;
import io.airbyte.config.StandardDestinationDefinition;
import io.airbyte.config.StandardSourceDefinition;
import io.airbyte.config.StandardSync;
import io.airbyte.config.StandardSyncOperation;
import io.airbyte.config.StandardWorkspace;
import io.airbyte.config.StreamDescriptor;
import io.airbyte.config.WorkspaceServiceAccount;
import io.airbyte.data.services.ConnectionService;
import io.airbyte.data.services.ConnectorBuilderService;
import io.airbyte.data.services.DestinationService;
import io.airbyte.data.services.OperationService;
import io.airbyte.data.services.SourceService;
import io.airbyte.data.services.WorkspaceService;
import io.airbyte.validation.json.JsonValidationException;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Repository of all SQL queries for the Configs Db. We are moving to persistences scoped by
 * resource.
 */
@Deprecated
@SuppressWarnings("PMD.PreserveStackTrace")
public class ConfigRepository {

  /**
   * Query object for querying connections for a workspace.
   *
   * @param workspaceId workspace to fetch connections for
   * @param sourceId fetch connections with this source id
   * @param destinationId fetch connections with this destination id
   * @param includeDeleted include tombstoned connections
   */
  public record StandardSyncQuery(@Nonnull UUID workspaceId, List<UUID> sourceId, List<UUID> destinationId, boolean includeDeleted) {

  }

  /**
   * Query object for paginated querying of connections in multiple workspaces.
   *
   * @param workspaceIds workspaces to fetch connections for
   * @param sourceId fetch connections with this source id
   * @param destinationId fetch connections with this destination id
   * @param includeDeleted include tombstoned connections
   * @param pageSize limit
   * @param rowOffset offset
   */
  public record StandardSyncsQueryPaginated(
                                            @Nonnull List<UUID> workspaceIds,
                                            List<UUID> sourceId,
                                            List<UUID> destinationId,
                                            boolean includeDeleted,
                                            int pageSize,
                                            int rowOffset) {

  }

  /**
   * Query object for paginated querying of sources/destinations in multiple workspaces.
   *
   * @param workspaceIds workspaces to fetch resources for
   * @param includeDeleted include tombstoned resources
   * @param pageSize limit
   * @param rowOffset offset
   * @param nameContains string to search name contains by
   */
  public record ResourcesQueryPaginated(
                                        @Nonnull List<UUID> workspaceIds,
                                        boolean includeDeleted,
                                        int pageSize,
                                        int rowOffset,
                                        String nameContains) {

  }

  /**
   * Query object for paginated querying of resource in an organization.
   *
   * @param organizationId organization to fetch resources for
   * @param includeDeleted include tombstoned resources
   * @param pageSize limit
   * @param rowOffset offset
   */
  public record ResourcesByOrganizationQueryPaginated(
                                                      @Nonnull UUID organizationId,
                                                      boolean includeDeleted,
                                                      int pageSize,
                                                      int rowOffset) {

  }

  /**
   * Query object for paginated querying of resource for a user.
   *
   * @param userId user to fetch resources for
   * @param includeDeleted include tombstoned resources
   * @param pageSize limit
   * @param rowOffset offset
   */
  public record ResourcesByUserQueryPaginated(
                                              @Nonnull UUID userId,
                                              boolean includeDeleted,
                                              int pageSize,
                                              int rowOffset) {}

  private final ConnectionService connectionService;
  private final ConnectorBuilderService connectorBuilderService;
  private final DestinationService destinationService;
  private final OperationService operationService;
  private final SourceService sourceService;
  private final WorkspaceService workspaceService;

  @SuppressWarnings("ParameterName")
  @VisibleForTesting
  public ConfigRepository(final ConnectionService connectionService,
                          final ConnectorBuilderService connectorBuilderService,
                          final DestinationService destinationService,
                          final OperationService operationService,
                          final SourceService sourceService,
                          final WorkspaceService workspaceService) {
    this.connectionService = connectionService;
    this.connectorBuilderService = connectorBuilderService;
    this.destinationService = destinationService;
    this.operationService = operationService;
    this.sourceService = sourceService;
    this.workspaceService = workspaceService;
  }

  /**
   * Get workspace.
   *
   * @param workspaceId workspace id
   * @param includeTombstone include tombstoned workspace
   * @return workspace
   * @throws JsonValidationException - throws if returned sources are invalid
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no source with that id can be found.
   */
  @Deprecated
  public StandardWorkspace getStandardWorkspaceNoSecrets(final UUID workspaceId, final boolean includeTombstone)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return workspaceService.getStandardWorkspaceNoSecrets(workspaceId, includeTombstone);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(ConfigSchema.STANDARD_WORKSPACE, workspaceId.toString());
    }
  }

  /**
   * Get workspace from slug.
   *
   * @param slug to use to find the workspace
   * @param includeTombstone include tombstoned workspace
   * @return workspace, if present.
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public Optional<StandardWorkspace> getWorkspaceBySlugOptional(final String slug, final boolean includeTombstone)
      throws IOException {
    return workspaceService.getWorkspaceBySlugOptional(slug, includeTombstone);
  }

  /**
   * Get workspace from slug.
   *
   * @param slug to use to find the workspace
   * @param includeTombstone include tombstoned workspace
   * @return workspace
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no source with that id can be found.
   */
  @Deprecated
  @SuppressWarnings("PMD")
  public StandardWorkspace getWorkspaceBySlug(final String slug, final boolean includeTombstone) throws IOException, ConfigNotFoundException {
    try {
      return workspaceService.getWorkspaceBySlug(slug, includeTombstone);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(ConfigSchema.STANDARD_WORKSPACE, slug);
    }
  }

  /**
   * List workspaces.
   *
   * @param includeTombstone include tombstoned workspaces
   * @return workspaces
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardWorkspace> listStandardWorkspaces(final boolean includeTombstone) throws IOException {
    return workspaceService.listStandardWorkspaces(includeTombstone);
  }

  /**
   * List workspaces with given ids.
   *
   * @param includeTombstone include tombstoned workspaces
   * @return workspaces
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardWorkspace> listStandardWorkspacesWithIds(final List<UUID> workspaceIds, final boolean includeTombstone) throws IOException {
    return workspaceService.listStandardWorkspacesWithIds(workspaceIds, includeTombstone);
  }

  /**
   * List ALL workspaces (paginated) with some filtering.
   *
   * @param resourcesQueryPaginated - contains all the information we need to paginate
   * @return A List of StandardWorkspace objects
   * @throws IOException you never know when you IO
   */
  @Deprecated
  public List<StandardWorkspace> listAllWorkspacesPaginated(final ResourcesQueryPaginated resourcesQueryPaginated) throws IOException {
    return workspaceService.listAllWorkspacesPaginated(
        new io.airbyte.data.services.shared.ResourcesQueryPaginated(
            resourcesQueryPaginated.workspaceIds(),
            resourcesQueryPaginated.includeDeleted(),
            resourcesQueryPaginated.pageSize(),
            resourcesQueryPaginated.rowOffset(),
            resourcesQueryPaginated.nameContains()));
  }

  /**
   * List workspaces (paginated).
   *
   * @param resourcesQueryPaginated - contains all the information we need to paginate
   * @return A List of StandardWorkspace objects
   * @throws IOException you never know when you IO
   */
  @Deprecated
  public List<StandardWorkspace> listStandardWorkspacesPaginated(final ResourcesQueryPaginated resourcesQueryPaginated) throws IOException {
    return workspaceService.listStandardWorkspacesPaginated(
        new io.airbyte.data.services.shared.ResourcesQueryPaginated(
            resourcesQueryPaginated.workspaceIds(),
            resourcesQueryPaginated.includeDeleted(),
            resourcesQueryPaginated.pageSize(),
            resourcesQueryPaginated.rowOffset(),
            resourcesQueryPaginated.nameContains()));
  }

  /**
   * MUST NOT ACCEPT SECRETS - Should only be called from the config-secrets module.
   * <p>
   * Write a StandardWorkspace to the database.
   *
   * @param workspace - The configuration of the workspace
   * @throws JsonValidationException - throws is the workspace is invalid
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeStandardWorkspaceNoSecrets(final StandardWorkspace workspace) throws JsonValidationException, IOException {
    workspaceService.writeStandardWorkspaceNoSecrets(workspace);
  }

  /**
   * Set user feedback on workspace.
   *
   * @param workspaceId workspace id.
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void setFeedback(final UUID workspaceId) throws IOException {
    workspaceService.setFeedback(workspaceId);
  }

  /**
   * Get source definition.
   *
   * @param sourceDefinitionId source definition id
   * @return source definition
   * @throws JsonValidationException - throws if returned sources are invalid
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no source with that id can be found.
   */
  @Deprecated
  public StandardSourceDefinition getStandardSourceDefinition(final UUID sourceDefinitionId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return sourceService.getStandardSourceDefinition(sourceDefinitionId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Get source definition form source.
   *
   * @param sourceId source id
   * @return source definition
   */
  @Deprecated
  public StandardSourceDefinition getSourceDefinitionFromSource(final UUID sourceId) {
    return sourceService.getSourceDefinitionFromSource(sourceId);
  }

  /**
   * Get source definition used by a connection.
   *
   * @param connectionId connection id
   * @return source definition
   */
  @Deprecated
  public StandardSourceDefinition getSourceDefinitionFromConnection(final UUID connectionId) {
    return sourceService.getSourceDefinitionFromConnection(connectionId);
  }

  /**
   * Get workspace for a connection.
   *
   * @param connectionId connection id
   * @param isTombstone include tombstoned workspaces
   * @return workspace to which the connection belongs
   */
  @Deprecated
  @SuppressWarnings("PMD")
  public StandardWorkspace getStandardWorkspaceFromConnection(final UUID connectionId, final boolean isTombstone) throws ConfigNotFoundException {
    try {
      return workspaceService.getStandardWorkspaceFromConnection(connectionId, isTombstone);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * List standard source definitions.
   *
   * @param includeTombstone include tombstoned source
   * @return list source definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardSourceDefinition> listStandardSourceDefinitions(final boolean includeTombstone) throws IOException {
    return sourceService.listStandardSourceDefinitions(includeTombstone);
  }

  /**
   * List public source definitions.
   *
   * @param includeTombstone include tombstoned source
   * @return public source definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardSourceDefinition> listPublicSourceDefinitions(final boolean includeTombstone) throws IOException {
    return sourceService.listPublicSourceDefinitions(includeTombstone);
  }

  /**
   * List granted source definitions for workspace.
   *
   * @param workspaceId workspace id
   * @param includeTombstones include tombstoned destinations
   * @return list standard source definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardSourceDefinition> listGrantedSourceDefinitions(final UUID workspaceId, final boolean includeTombstones)
      throws IOException {
    return sourceService.listGrantedSourceDefinitions(workspaceId, includeTombstones);
  }

  /**
   * List source to which we can give a grant.
   *
   * @param workspaceId workspace id
   * @param includeTombstones include tombstoned definitions
   * @return list of pairs from source definition and whether it can be granted
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<Entry<StandardSourceDefinition, Boolean>> listGrantableSourceDefinitions(final UUID workspaceId,
                                                                                       final boolean includeTombstones)
      throws IOException {
    return sourceService.listGrantableSourceDefinitions(workspaceId, includeTombstones);
  }

  /**
   * Update source definition.
   *
   * @param sourceDefinition source definition
   * @throws JsonValidationException - throws if returned sources are invalid
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void updateStandardSourceDefinition(final StandardSourceDefinition sourceDefinition)
      throws IOException, JsonValidationException, ConfigNotFoundException {
    try {
      sourceService.updateStandardSourceDefinition(sourceDefinition);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Get destination definition.
   *
   * @param destinationDefinitionId destination definition id
   * @return destination definition
   * @throws JsonValidationException - throws if returned sources are invalid
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no source with that id can be found.
   */
  @Deprecated
  public StandardDestinationDefinition getStandardDestinationDefinition(final UUID destinationDefinitionId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return destinationService.getStandardDestinationDefinition(destinationDefinitionId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Get destination definition form destination.
   *
   * @param destinationId destination id
   * @return destination definition
   */
  @Deprecated
  public StandardDestinationDefinition getDestinationDefinitionFromDestination(final UUID destinationId) {
    return destinationService.getDestinationDefinitionFromDestination(destinationId);
  }

  /**
   * Get destination definition used by a connection.
   *
   * @param connectionId connection id
   * @return destination definition
   */
  @Deprecated
  public StandardDestinationDefinition getDestinationDefinitionFromConnection(final UUID connectionId) {
    return destinationService.getDestinationDefinitionFromConnection(connectionId);
  }

  /**
   * List standard destination definitions.
   *
   * @param includeTombstone include tombstoned destinations
   * @return list destination definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardDestinationDefinition> listStandardDestinationDefinitions(final boolean includeTombstone) throws IOException {
    return destinationService.listStandardDestinationDefinitions(includeTombstone);
  }

  /**
   * List public destination definitions.
   *
   * @param includeTombstone include tombstoned destinations
   * @return public destination definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardDestinationDefinition> listPublicDestinationDefinitions(final boolean includeTombstone) throws IOException {
    return destinationService.listPublicDestinationDefinitions(includeTombstone);
  }

  /**
   * List granted destination definitions for workspace.
   *
   * @param workspaceId workspace id
   * @param includeTombstones include tombstoned destinations
   * @return list standard destination definitions
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<StandardDestinationDefinition> listGrantedDestinationDefinitions(final UUID workspaceId, final boolean includeTombstones)
      throws IOException {
    return destinationService.listGrantedDestinationDefinitions(workspaceId, includeTombstones);
  }

  /**
   * List destinations to which we can give a grant.
   *
   * @param workspaceId workspace id
   * @param includeTombstones include tombstoned definitions
   * @return list of pairs from destination definition and whether it can be granted
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<Entry<StandardDestinationDefinition, Boolean>> listGrantableDestinationDefinitions(final UUID workspaceId,
                                                                                                 final boolean includeTombstones)
      throws IOException {
    return destinationService.listGrantableDestinationDefinitions(workspaceId, includeTombstones);
  }

  /**
   * Update destination definition.
   *
   * @param destinationDefinition destination definition
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void updateStandardDestinationDefinition(final StandardDestinationDefinition destinationDefinition)
      throws IOException, JsonValidationException, ConfigNotFoundException {
    try {
      destinationService.updateStandardDestinationDefinition(destinationDefinition);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Write metadata for a destination connector. Writes global metadata (destination definition) and
   * versioned metadata (info for actor definition version to set as default). Sets the new version as
   * the default version and updates actors accordingly, based on whether the upgrade will be breaking
   * or not.
   *
   * @param destinationDefinition standard destination definition
   * @param actorDefinitionVersion actor definition version
   * @param breakingChangesForDefinition - list of breaking changes for the definition
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeConnectorMetadata(final StandardDestinationDefinition destinationDefinition,
                                     final ActorDefinitionVersion actorDefinitionVersion,
                                     final List<ActorDefinitionBreakingChange> breakingChangesForDefinition)
      throws IOException {
    destinationService.writeConnectorMetadata(destinationDefinition, actorDefinitionVersion, breakingChangesForDefinition);
  }

  /**
   * Write metadata for a destination connector. Writes global metadata (destination definition) and
   * versioned metadata (info for actor definition version to set as default). Sets the new version as
   * the default version and updates actors accordingly, based on whether the upgrade will be breaking
   * or not. Usage of this version of the method assumes no new breaking changes need to be persisted
   * for the definition.
   *
   * @param destinationDefinition standard destination definition
   * @param actorDefinitionVersion actor definition version
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeConnectorMetadata(final StandardDestinationDefinition destinationDefinition,
                                     final ActorDefinitionVersion actorDefinitionVersion)
      throws IOException {
    destinationService.writeConnectorMetadata(destinationDefinition, actorDefinitionVersion, List.of());
  }

  /**
   * Write metadata for a source connector. Writes global metadata (source definition, breaking
   * changes) and versioned metadata (info for actor definition version to set as default). Sets the
   * new version as the default version and updates actors accordingly, based on whether the upgrade
   * will be breaking or not.
   *
   * @param sourceDefinition standard source definition
   * @param actorDefinitionVersion actor definition version, containing tag to set as default
   * @param breakingChangesForDefinition - list of breaking changes for the definition
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeConnectorMetadata(final StandardSourceDefinition sourceDefinition,
                                     final ActorDefinitionVersion actorDefinitionVersion,
                                     final List<ActorDefinitionBreakingChange> breakingChangesForDefinition)
      throws IOException {
    sourceService.writeConnectorMetadata(sourceDefinition, actorDefinitionVersion, breakingChangesForDefinition);
  }

  /**
   * Write metadata for a source connector. Writes global metadata (source definition) and versioned
   * metadata (info for actor definition version to set as default). Sets the new version as the
   * default version and updates actors accordingly, based on whether the upgrade will be breaking or
   * not. Usage of this version of the method assumes no new breaking changes need to be persisted for
   * the definition.
   *
   * @param sourceDefinition standard source definition
   * @param actorDefinitionVersion actor definition version
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeConnectorMetadata(final StandardSourceDefinition sourceDefinition,
                                     final ActorDefinitionVersion actorDefinitionVersion)
      throws IOException {
    sourceService.writeConnectorMetadata(sourceDefinition, actorDefinitionVersion, List.of());
  }

  /**
   * Write metadata for a custom destination: global metadata (destination definition) and versioned
   * metadata (actor definition version for the version to use).
   *
   * @param destinationDefinition destination definition
   * @param defaultVersion default actor definition version
   * @param scopeId workspace or organization id
   * @param scopeType enum of workspace or organization
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeCustomConnectorMetadata(final StandardDestinationDefinition destinationDefinition,
                                           final ActorDefinitionVersion defaultVersion,
                                           final UUID scopeId,
                                           final io.airbyte.config.ScopeType scopeType)
      throws IOException {
    destinationService.writeCustomConnectorMetadata(destinationDefinition, defaultVersion, scopeId, scopeType);
  }

  /**
   * Write metadata for a custom source: global metadata (source definition) and versioned metadata
   * (actor definition version for the version to use).
   *
   * @param sourceDefinition source definition
   * @param defaultVersion default actor definition version
   * @param scopeId scope id
   * @param scopeType enum which defines if the scopeId is a workspace or organization id
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeCustomConnectorMetadata(final StandardSourceDefinition sourceDefinition,
                                           final ActorDefinitionVersion defaultVersion,
                                           final UUID scopeId,
                                           final io.airbyte.config.ScopeType scopeType)
      throws IOException {
    sourceService.writeCustomConnectorMetadata(sourceDefinition, defaultVersion, scopeId, scopeType);
  }

  /**
   * Delete connection.
   *
   * @param syncId connection id
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void deleteStandardSync(final UUID syncId) throws IOException {
    connectionService.deleteStandardSync(syncId);
  }

  /**
   * Test if workspace id has access to a connector definition.
   *
   * @param actorDefinitionId actor definition id
   * @param workspaceId id of the workspace
   * @return true, if the workspace has access. otherwise, false.
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public boolean workspaceCanUseDefinition(final UUID actorDefinitionId, final UUID workspaceId) throws IOException {
    return workspaceService.workspaceCanUseDefinition(actorDefinitionId, workspaceId);
  }

  /**
   * Test if workspace is has access to a custom connector definition.
   *
   * @param actorDefinitionId custom actor definition id
   * @param workspaceId workspace id
   * @return true, if the workspace has access. otherwise, false.
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public boolean workspaceCanUseCustomDefinition(final UUID actorDefinitionId, final UUID workspaceId) throws IOException {
    return workspaceService.workspaceCanUseCustomDefinition(actorDefinitionId, workspaceId);
  }

  /**
   * Returns source with a given id. Does not contain secrets. To hydrate with secrets see the
   * config-secrets module.
   *
   * @param sourceId - id of source to fetch.
   * @return sources
   * @throws JsonValidationException - throws if returned sources are invalid
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no source with that id can be found.
   */
  @Deprecated
  public SourceConnection getSourceConnection(final UUID sourceId) throws JsonValidationException, ConfigNotFoundException, IOException {
    try {
      return sourceService.getSourceConnection(sourceId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * MUST NOT ACCEPT SECRETS - Should only be called from the config-secrets module.
   * <p>
   * Write a SourceConnection to the database. The configuration of the Source will be a partial
   * configuration (no secrets, just pointer to the secrets store).
   *
   * @param partialSource - The configuration of the Source will be a partial configuration (no
   *        secrets, just pointer to the secrets store)
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeSourceConnectionNoSecrets(final SourceConnection partialSource) throws IOException {
    sourceService.writeSourceConnectionNoSecrets(partialSource);
  }

  /**
   * Returns all sources in the database. Does not contain secrets. To hydrate with secrets see the
   * config-secrets module.
   *
   * @return sources
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<SourceConnection> listSourceConnection() throws IOException {
    return sourceService.listSourceConnection();
  }

  /**
   * Returns all sources for a workspace. Does not contain secrets.
   *
   * @param workspaceId - id of the workspace
   * @return sources
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<SourceConnection> listWorkspaceSourceConnection(final UUID workspaceId) throws IOException {
    return sourceService.listWorkspaceSourceConnection(workspaceId);
  }

  /**
   * Returns all sources for a set of workspaces. Does not contain secrets.
   *
   * @param resourcesQueryPaginated - Includes all the things we might want to query
   * @return sources
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<SourceConnection> listWorkspacesSourceConnections(final ResourcesQueryPaginated resourcesQueryPaginated) throws IOException {
    return sourceService.listWorkspacesSourceConnections(new io.airbyte.data.services.shared.ResourcesQueryPaginated(
        resourcesQueryPaginated.workspaceIds,
        resourcesQueryPaginated.includeDeleted,
        resourcesQueryPaginated.pageSize,
        resourcesQueryPaginated.rowOffset,
        resourcesQueryPaginated.nameContains));
  }

  /**
   * Returns destination with a given id. Does not contain secrets. To hydrate with secrets see the
   * config-secrets module.
   *
   * @param destinationId - id of destination to fetch.
   * @return destinations
   * @throws JsonValidationException - throws if returned destinations are invalid
   * @throws IOException - you never know when you IO
   * @throws ConfigNotFoundException - throws if no destination with that id can be found.
   */
  @Deprecated
  public DestinationConnection getDestinationConnection(final UUID destinationId)
      throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return destinationService.getDestinationConnection(destinationId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * MUST NOT ACCEPT SECRETS - Should only be called from the config-secrets module.
   * <p>
   * Write a DestinationConnection to the database. The configuration of the Destination will be a
   * partial configuration (no secrets, just pointer to the secrets store).
   *
   * @param partialDestination - The configuration of the Destination will be a partial configuration
   *        (no secrets, just pointer to the secrets store)
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public void writeDestinationConnectionNoSecrets(final DestinationConnection partialDestination) throws IOException {
    destinationService.writeDestinationConnectionNoSecrets(partialDestination);
  }

  /**
   * Returns all destinations in the database. Does not contain secrets. To hydrate with secrets see
   * the config-secrets module.
   *
   * @return destinations
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<DestinationConnection> listDestinationConnection() throws IOException {
    return destinationService.listDestinationConnection();
  }

  /**
   * Returns all destinations for a workspace. Does not contain secrets.
   *
   * @param workspaceId - id of the workspace
   * @return destinations
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<DestinationConnection> listWorkspaceDestinationConnection(final UUID workspaceId) throws IOException {
    return destinationService.listWorkspaceDestinationConnection(workspaceId);
  }

  /**
   * Returns all destinations for a list of workspaces. Does not contain secrets.
   *
   * @param resourcesQueryPaginated - Includes all the things we might want to query
   * @return destinations
   * @throws IOException - you never know when you IO
   */
  @Deprecated
  public List<DestinationConnection> listWorkspacesDestinationConnections(final ResourcesQueryPaginated resourcesQueryPaginated) throws IOException {
    final var query = new io.airbyte.data.services.shared.ResourcesQueryPaginated(
        resourcesQueryPaginated.workspaceIds,
        resourcesQueryPaginated.includeDeleted,
        resourcesQueryPaginated.pageSize,
        resourcesQueryPaginated.rowOffset,
        resourcesQueryPaginated.nameContains);
    return destinationService.listWorkspacesDestinationConnections(query);
  }

  /**
   * List active workspace IDs with most recently running jobs within a given time window (in hours).
   *
   * @param timeWindowInHours - integer, e.g. 24, 48, etc
   * @return list of workspace IDs
   * @throws IOException - failed to query data
   */
  @Deprecated
  public List<UUID> listActiveWorkspacesByMostRecentlyRunningJobs(final int timeWindowInHours) throws IOException {
    return workspaceService.listActiveWorkspacesByMostRecentlyRunningJobs(timeWindowInHours);
  }

  /**
   * Returns all active sources using a definition.
   *
   * @param definitionId - id for the definition
   * @return sources
   * @throws IOException - exception while interacting with the db
   */
  @Deprecated
  public List<SourceConnection> listSourcesForDefinition(final UUID definitionId) throws IOException {
    return sourceService.listSourcesForDefinition(definitionId);
  }

  /**
   * Returns all active destinations using a definition.
   *
   * @param definitionId - id for the definition
   * @return destinations
   * @throws IOException - exception while interacting with the db
   */
  @Deprecated
  public List<DestinationConnection> listDestinationsForDefinition(final UUID definitionId) throws IOException {
    return destinationService.listDestinationsForDefinition(definitionId);
  }

  /**
   * Get connection.
   *
   * @param connectionId connection id
   * @return connection
   * @throws JsonValidationException if the workspace is or contains invalid json
   * @throws ConfigNotFoundException if the config does not exist
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public StandardSync getStandardSync(final UUID connectionId) throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return connectionService.getStandardSync(connectionId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Write connection.
   *
   * @param standardSync connection
   * @throws IOException - exception while interacting with the db
   */
  @Deprecated
  public void writeStandardSync(final StandardSync standardSync) throws IOException {
    connectionService.writeStandardSync(standardSync);
  }

  /**
   * List connections.
   *
   * @return connections
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StandardSync> listStandardSyncs() throws IOException {
    return connectionService.listStandardSyncs();
  }

  /**
   * List connections using operation.
   *
   * @param operationId operation id.
   * @return Connections that use the operation.
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StandardSync> listStandardSyncsUsingOperation(final UUID operationId) throws IOException {
    return connectionService.listStandardSyncsUsingOperation(operationId);
  }

  /**
   * List connections for workspace.
   *
   * @param workspaceId workspace id
   * @param includeDeleted include deleted
   * @return list of connections
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  @Trace
  public List<StandardSync> listWorkspaceStandardSyncs(final UUID workspaceId, final boolean includeDeleted) throws IOException {
    return connectionService.listWorkspaceStandardSyncs(workspaceId, includeDeleted);
  }

  /**
   * List connections for workspace via a query.
   *
   * @param standardSyncQuery query
   * @return list of connections
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StandardSync> listWorkspaceStandardSyncs(final StandardSyncQuery standardSyncQuery) throws IOException {
    final var query = new io.airbyte.data.services.shared.StandardSyncQuery(
        standardSyncQuery.workspaceId(),
        standardSyncQuery.sourceId(),
        standardSyncQuery.destinationId(),
        standardSyncQuery.includeDeleted());
    return connectionService.listWorkspaceStandardSyncs(query);
  }

  /**
   * List connection IDs for active syncs based on the given query.
   *
   * @param standardSyncQuery query
   * @return list of connection IDs
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<UUID> listWorkspaceActiveSyncIds(final StandardSyncQuery standardSyncQuery) throws IOException {
    return workspaceService.listWorkspaceActiveSyncIds(new io.airbyte.data.services.shared.StandardSyncQuery(
        standardSyncQuery.workspaceId(),
        standardSyncQuery.sourceId(),
        standardSyncQuery.destinationId(),
        standardSyncQuery.includeDeleted()));
  }

  /**
   * List connections. Paginated.
   */
  @Deprecated
  public Map<UUID, List<StandardSync>> listWorkspaceStandardSyncsPaginated(
                                                                           final List<UUID> workspaceIds,
                                                                           final boolean includeDeleted,
                                                                           final int pageSize,
                                                                           final int rowOffset)
      throws IOException {
    return connectionService.listWorkspaceStandardSyncsPaginated(workspaceIds, includeDeleted, pageSize, rowOffset);
  }

  /**
   * List connections for workspace. Paginated.
   *
   * @param standardSyncsQueryPaginated query
   * @return Map of workspace ID -> list of connections
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public Map<UUID, List<StandardSync>> listWorkspaceStandardSyncsPaginated(final StandardSyncsQueryPaginated standardSyncsQueryPaginated)
      throws IOException {
    final var query = new io.airbyte.data.services.shared.StandardSyncsQueryPaginated(
        standardSyncsQueryPaginated.workspaceIds(),
        standardSyncsQueryPaginated.sourceId(),
        standardSyncsQueryPaginated.destinationId(),
        standardSyncsQueryPaginated.includeDeleted(),
        standardSyncsQueryPaginated.pageSize(),
        standardSyncsQueryPaginated.rowOffset());
    return connectionService.listWorkspaceStandardSyncsPaginated(query);
  }

  /**
   * List connections that use a source.
   *
   * @param sourceId source id
   * @param includeDeleted include deleted
   * @return connections that use the provided source
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StandardSync> listConnectionsBySource(final UUID sourceId, final boolean includeDeleted) throws IOException {
    return connectionService.listConnectionsBySource(sourceId, includeDeleted);
  }

  /**
   * List connections that use a particular actor definition.
   *
   * @param actorDefinitionId id of the source or destination definition.
   * @param actorTypeValue either 'source' or 'destination' enum value.
   * @param includeDeleted whether to include tombstoned records in the return value.
   * @return List of connections that use the actor definition.
   * @throws IOException you never know when you IO
   */
  @Deprecated
  public List<StandardSync> listConnectionsByActorDefinitionIdAndType(final UUID actorDefinitionId,
                                                                      final String actorTypeValue,
                                                                      final boolean includeDeleted)
      throws IOException {
    return connectionService.listConnectionsByActorDefinitionIdAndType(
        actorDefinitionId,
        actorTypeValue,
        includeDeleted);
  }

  /**
   * Disable a list of connections by setting their status to inactive.
   *
   * @param connectionIds list of connection ids to disable
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public void disableConnectionsById(final List<UUID> connectionIds) throws IOException {
    connectionService.disableConnectionsById(connectionIds);
  }

  /**
   * Get sync operation.
   *
   * @param operationId operation id
   * @return sync operation
   * @throws JsonValidationException if the workspace is or contains invalid json
   * @throws ConfigNotFoundException if the config does not exist
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public StandardSyncOperation getStandardSyncOperation(final UUID operationId) throws JsonValidationException, IOException, ConfigNotFoundException {
    try {
      return operationService.getStandardSyncOperation(operationId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Write standard sync operation.
   *
   * @param standardSyncOperation standard sync operation.
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public void writeStandardSyncOperation(final StandardSyncOperation standardSyncOperation) throws IOException {
    operationService.writeStandardSyncOperation(standardSyncOperation);
  }

  /**
   * List standard sync operations.
   *
   * @return standard sync operations.
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StandardSyncOperation> listStandardSyncOperations() throws IOException {
    return operationService.listStandardSyncOperations();
  }

  /**
   * Updates {@link io.airbyte.db.instance.configs.jooq.generated.tables.ConnectionOperation} records
   * for the given {@code connectionId}.
   *
   * @param connectionId ID of the associated connection to update operations for
   * @param newOperationIds Set of all operationIds that should be associated to the connection
   * @throws IOException - exception while interacting with the db
   */
  @Deprecated
  public void updateConnectionOperationIds(final UUID connectionId, final Set<UUID> newOperationIds) throws IOException {
    operationService.updateConnectionOperationIds(connectionId, newOperationIds);
  }

  /**
   * Delete standard sync operation.
   *
   * @param standardSyncOperationId standard sync operation id
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public void deleteStandardSyncOperation(final UUID standardSyncOperationId) throws IOException {
    operationService.deleteStandardSyncOperation(standardSyncOperationId);
  }

  /**
   * Pair of source and its associated definition.
   * <p>
   * Data-carrier records to hold combined result of query for a Source or Destination and its
   * corresponding Definition. This enables the API layer to process combined information about a
   * Source/Destination/Definition pair without requiring two separate queries and in-memory join
   * operation, because the config models are grouped immediately in the repository layer.
   *
   * @param source source
   * @param definition its corresponding definition
   */
  @VisibleForTesting
  public record SourceAndDefinition(SourceConnection source, StandardSourceDefinition definition) {

  }

  /**
   * Pair of destination and its associated definition.
   *
   * @param destination destination
   * @param definition its corresponding definition
   */
  @VisibleForTesting
  public record DestinationAndDefinition(DestinationConnection destination, StandardDestinationDefinition definition) {

  }

  /**
   * Get source and definition from sources ids.
   *
   * @param sourceIds source ids
   * @return pair of source and definition
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<SourceAndDefinition> getSourceAndDefinitionsFromSourceIds(final List<UUID> sourceIds) throws IOException {
    return sourceService.getSourceAndDefinitionsFromSourceIds(sourceIds)
        .stream()
        .map(record -> new SourceAndDefinition(record.source(), record.definition()))
        .toList();
  }

  /**
   * Get destination and definition from destinations ids.
   *
   * @param destinationIds destination ids
   * @return pair of destination and definition
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<DestinationAndDefinition> getDestinationAndDefinitionsFromDestinationIds(final List<UUID> destinationIds) throws IOException {
    return destinationService.getDestinationAndDefinitionsFromDestinationIds(destinationIds)
        .stream()
        .map(record -> new DestinationAndDefinition(record.destination(), record.definition()))
        .toList();
  }

  /**
   * Count connections in workspace.
   *
   * @param workspaceId workspace id
   * @return number of connections in workspace
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public int countConnectionsForWorkspace(final UUID workspaceId) throws IOException {
    return workspaceService.countConnectionsForWorkspace(workspaceId);
  }

  /**
   * Count sources in workspace.
   *
   * @param workspaceId workspace id
   * @return number of sources in workspace
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public int countSourcesForWorkspace(final UUID workspaceId) throws IOException {
    return workspaceService.countSourcesForWorkspace(workspaceId);
  }

  /**
   * Count destinations in workspace.
   *
   * @param workspaceId workspace id
   * @return number of destinations in workspace
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public int countDestinationsForWorkspace(final UUID workspaceId) throws IOException {
    return workspaceService.countDestinationsForWorkspace(workspaceId);
  }

  /**
   * Get workspace service account without secrets.
   *
   * @param workspaceId workspace id
   * @return workspace service account
   * @throws ConfigNotFoundException if the config does not exist
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  @SuppressWarnings("PMD")
  public WorkspaceServiceAccount getWorkspaceServiceAccountNoSecrets(final UUID workspaceId) throws IOException, ConfigNotFoundException {
    // breaking the pattern of doing a list query, because we never want to list this resource without
    // scoping by workspace id.
    try {
      return workspaceService.getWorkspaceServiceAccountNoSecrets(workspaceId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(ConfigSchema.WORKSPACE_SERVICE_ACCOUNT, workspaceId.toString());
    }
  }

  /**
   * Write workspace service account with no secrets.
   *
   * @param workspaceServiceAccount workspace service account
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public void writeWorkspaceServiceAccountNoSecrets(final WorkspaceServiceAccount workspaceServiceAccount) throws IOException {
    workspaceService.writeWorkspaceServiceAccountNoSecrets(workspaceServiceAccount);
  }

  /**
   * Get all streams for connection.
   *
   * @param connectionId connection id
   * @return list of streams for connection
   * @throws ConfigNotFoundException if the config does not exist
   * @throws IOException if there is an issue while interacting with db.
   */
  @Deprecated
  public List<StreamDescriptor> getAllStreamsForConnection(final UUID connectionId) throws ConfigNotFoundException, IOException {
    try {
      return connectionService.getAllStreamsForConnection(connectionId);
    } catch (final io.airbyte.data.exceptions.ConfigNotFoundException e) {
      throw new ConfigNotFoundException(e.getType(), e.getConfigId());
    }
  }

  /**
   * Get geography for a connection.
   *
   * @param connectionId connection id
   * @return geography
   * @throws IOException exception while interacting with the db
   */
  @Deprecated
  public Geography getGeographyForConnection(final UUID connectionId) throws IOException {
    return connectionService.getGeographyForConnection(connectionId);
  }

  /**
   * Get geography for a workspace.
   *
   * @param workspaceId workspace id
   * @return geography
   * @throws IOException exception while interacting with the db
   */
  @Deprecated
  public Geography getGeographyForWorkspace(final UUID workspaceId) throws IOException {
    return workspaceService.getGeographyForWorkspace(workspaceId);
  }

  /**
   * Specialized query for efficiently determining eligibility for the Free Connector Program. If a
   * workspace has at least one Alpha or Beta connector, users of that workspace will be prompted to
   * sign up for the program. This check is performed on nearly every page load so the query needs to
   * be as efficient as possible.
   * <p>
   * This should only be used for efficiently determining eligibility for the Free Connector Program.
   * Anything that involves billing should instead use the ActorDefinitionVersionHelper to determine
   * the ReleaseStages.
   *
   * @param workspaceId ID of the workspace to check connectors for
   * @return boolean indicating if an alpha or beta connector exists within the workspace
   */
  @Deprecated
  public boolean getWorkspaceHasAlphaOrBetaConnector(final UUID workspaceId) throws IOException {
    return workspaceService.getWorkspaceHasAlphaOrBetaConnector(workspaceId);
  }

  /**
   * Specialized query for efficiently determining a connection's eligibility for the Free Connector
   * Program. If a connection has at least one Alpha or Beta connector, it will be free to use as long
   * as the workspace is enrolled in the Free Connector Program. This check is used to allow free
   * connections to continue running even when a workspace runs out of credits.
   * <p>
   * This should only be used for efficiently determining eligibility for the Free Connector Program.
   * Anything that involves billing should instead use the ActorDefinitionVersionHelper to determine
   * the ReleaseStages.
   *
   * @param connectionId ID of the connection to check connectors for
   * @return boolean indicating if an alpha or beta connector is used by the connection
   */
  @Deprecated
  public boolean getConnectionHasAlphaOrBetaConnector(final UUID connectionId) throws IOException {
    return connectionService.getConnectionHasAlphaOrBetaConnector(connectionId);
  }

  /**
   * Load all config injection for an actor definition.
   *
   * @param actorDefinitionId id of the actor definition to fetch
   * @return stream of config injection objects
   * @throws IOException exception while interacting with db
   */
  @Deprecated
  public Stream<ActorDefinitionConfigInjection> getActorDefinitionConfigInjections(final UUID actorDefinitionId) throws IOException {
    return connectorBuilderService.getActorDefinitionConfigInjections(actorDefinitionId);
  }

  /**
   * Update or create a config injection object. If there is an existing config injection for the
   * given actor definition and path, it is updated. If there isn't yet, a new config injection is
   * created.
   *
   * @param actorDefinitionConfigInjection the config injection object to write to the database
   * @throws IOException exception while interacting with db
   */
  @Deprecated
  public void writeActorDefinitionConfigInjectionForPath(final ActorDefinitionConfigInjection actorDefinitionConfigInjection) throws IOException {
    connectorBuilderService.writeActorDefinitionConfigInjectionForPath(actorDefinitionConfigInjection);
  }

  @Deprecated
  public Set<Long> listEarlySyncJobs(final int freeUsageInterval, final int jobsFetchRange)
      throws IOException {
    return connectionService.listEarlySyncJobs(freeUsageInterval, jobsFetchRange);
  }

}
