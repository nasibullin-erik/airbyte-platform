/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.server.handlers.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.airbyte.api.model.generated.SourceRead;
import io.airbyte.api.model.generated.SourceSearch;
import org.apache.commons.lang3.StringUtils;

/**
 * Constructs a query for finding a query.
 */
public class SourceMatcher implements Matchable<SourceRead> {

  private final SourceSearch search;

  public SourceMatcher(final SourceSearch search) {
    this.search = search;
  }

  @Override
  public SourceRead match(final SourceRead query) {
    if (search == null) {
      return query;
    }

    final SourceRead fromSearch = new SourceRead();
    fromSearch.name(StringUtils.isBlank(search.getName()) ? query.getName() : search.getName());
    fromSearch.sourceDefinitionId(search.getSourceDefinitionId() == null ? query.getSourceDefinitionId() : search.getSourceDefinitionId());
    fromSearch.sourceId(search.getSourceId() == null ? query.getSourceId() : search.getSourceId());
    fromSearch.sourceName(StringUtils.isBlank(search.getSourceName()) ? query.getSourceName() : search.getSourceName());
    fromSearch.workspaceId(search.getWorkspaceId() == null ? query.getWorkspaceId() : search.getWorkspaceId());
    fromSearch.icon(query.getIcon());
    fromSearch.isVersionOverrideApplied(query.getIsVersionOverrideApplied());
    fromSearch.breakingChanges(query.getBreakingChanges());
    fromSearch.supportState(query.getSupportState());

    if (search.getConnectionConfiguration() == null) {
      fromSearch.connectionConfiguration(query.getConnectionConfiguration());
    } else if (query.getConnectionConfiguration() == null) {
      fromSearch.connectionConfiguration(search.getConnectionConfiguration());
    } else {
      final JsonNode connectionConfiguration = search.getConnectionConfiguration();
      query.getConnectionConfiguration().fieldNames()
          .forEachRemaining(field -> {
            if (!connectionConfiguration.has(field) && connectionConfiguration instanceof ObjectNode) {
              ((ObjectNode) connectionConfiguration).set(field, query.getConnectionConfiguration().get(field));
            }
          });
      fromSearch.connectionConfiguration(connectionConfiguration);
    }

    return fromSearch;
  }

}
