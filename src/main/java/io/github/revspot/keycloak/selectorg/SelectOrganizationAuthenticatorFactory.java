/*
 * Copyright 2024 Revspot AI
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
package io.github.revspot.keycloak.selectorg;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;

public class SelectOrganizationAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "select-organization";
    public static final String REQUIRES_USER_MEMBERSHIP = "requiresUserMembership";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Select Organization (Post-Auth)";
    }

    @Override
    public String getHelpText() {
        return "Shows an organization picker after authentication for users in multiple organizations.";
    }

    @Override
    public String getReferenceCategory() {
        return "organization";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }


    @Override
    public Authenticator create(KeycloakSession session) {
        return new SelectOrganizationAuthenticator();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.singletonList(new ProviderConfigProperty(REQUIRES_USER_MEMBERSHIP, "Requires user membership", "Enforces that users authenticating in the scope of an organization are members. If not a member, the user won't be able to proceed authenticating to the realm", BOOLEAN_TYPE, null));
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
