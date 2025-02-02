/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.generated.CompleteDestinationOAuthRequest;
import io.airbyte.api.model.generated.CompleteOAuthResponse;
import io.airbyte.api.model.generated.DestinationOauthConsentRequest;
import io.airbyte.api.model.generated.OAuthConsentRead;
import io.airbyte.api.model.generated.SetInstancewideDestinationOauthParamsRequestBody;
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
class DestinationOauthApiTest extends BaseControllerTest {

  @Test
  void testCompleteDestinationOAuth()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(oAuthHandler.completeDestinationOAuth(Mockito.any()))
        .thenReturn(new CompleteOAuthResponse())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destination_oauths/complete_oauth";
    testEndpointStatus(
        HttpRequest.POST(path, new CompleteDestinationOAuthRequest()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new CompleteDestinationOAuthRequest()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetDestinationOAuthConsent()
      throws JsonValidationException, ConfigNotFoundException, IOException, io.airbyte.data.exceptions.ConfigNotFoundException {
    Mockito.when(oAuthHandler.getDestinationOAuthConsent(Mockito.any()))
        .thenReturn(new OAuthConsentRead())
        .thenThrow(new ConfigNotFoundException("", ""));
    final String path = "/api/v1/destination_oauths/get_consent_url";
    testEndpointStatus(
        HttpRequest.POST(path, new DestinationOauthConsentRequest()),
        HttpStatus.OK);
    testErrorEndpointStatus(
        HttpRequest.POST(path, new DestinationOauthConsentRequest()),
        HttpStatus.NOT_FOUND);
  }

  @Test
  void testDeleteDestination() throws IOException {
    Mockito.doNothing()
        .when(oAuthHandler).setDestinationInstancewideOauthParams(Mockito.any());

    final String path = "/api/v1/destination_oauths/oauth_params/create";
    testEndpointStatus(
        HttpRequest.POST(path, new SetInstancewideDestinationOauthParamsRequestBody()),
        HttpStatus.OK);
  }

}
