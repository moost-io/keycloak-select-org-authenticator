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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Post-authentication organization selector for Keycloak.
 *
 * <p>When added to a browser authentication flow (typically after credential/OTP
 * verification), this authenticator queries the user's Keycloak Organization
 * memberships and:</p>
 *
 * <ul>
 *   <li><b>0 orgs</b> — denies access (user must belong to at least one organization)</li>
 *   <li><b>1 org</b> — auto-selects, no picker shown</li>
 *   <li><b>2+ orgs</b> — renders {@code select-organization.ftl} for the user to pick</li>
 * </ul>
 *
 * <p>The selected organization ID is stored in the authentication session notes
 * ({@code kc.org}) so downstream token mappers (e.g. Organization Membership mapper)
 * can include it in the issued token.</p>
 */
public class SelectOrganizationAuthenticator implements Authenticator {

    private final static Logger log = Logger.getLogger(SelectOrganizationAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.success();
            return;
        }

        RealmModel realm = context.getRealm();
        if (!realm.isOrganizationsEnabled()) {
            log.info("No organization enabled");
            context.success();
            return;
        }

        OrganizationProvider orgProvider = context.getSession().getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            log.info("No organization provider found");
            context.success();
            return;
        }

        List<OrganizationModel> orgs = orgProvider.getByMember(user)
                .filter(OrganizationModel::isEnabled)
                .collect(Collectors.toList());
        log.info("User " + user.getUsername() + " is assigned to organizations: " + orgs.stream().map(OrganizationModel::getName).collect(Collectors.joining(", ")));

        if (orgs.isEmpty()) {
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        if (orgs.size() == 1) {
            setSelectedOrganization(context, orgs.get(0));
            context.success();
            return;
        }

        // Multiple orgs — show picker
        showOrgPicker(context, user, orgs);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String selectedOrgAlias = formData.getFirst("kc.org");

        if (selectedOrgAlias == null || selectedOrgAlias.isBlank()) {
            authenticate(context);
            return;
        }

        UserModel user = context.getUser();
        OrganizationProvider orgProvider = context.getSession().getProvider(OrganizationProvider.class);

        OrganizationModel selected = orgProvider.getByMember(user)
                .filter(OrganizationModel::isEnabled)
                .filter(org -> selectedOrgAlias.equals(org.getAlias()) || selectedOrgAlias.equals(org.getName()))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        setSelectedOrganization(context, selected);
        context.success();
    }

    private void setSelectedOrganization(AuthenticationFlowContext context, OrganizationModel org) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote("kc.org", org.getId());
        authSession.setClientNote("kc.org", org.getId());
        authSession.setAuthNote("kc.org.name", org.getName());
    }

    private void showOrgPicker(AuthenticationFlowContext context, UserModel user,
                               List<OrganizationModel> orgs) {
        LoginFormsProvider form = context.form();

        List<OrgBean> orgBeans = orgs.stream()
                .map(o -> new OrgBean(o.getName(), o.getAlias()))
                .collect(Collectors.toList());

        form.setAttribute("organizations", orgBeans);
        form.setAttribute("username", user.getEmail() != null ? user.getEmail() : user.getUsername());

        Response challenge = form.createForm("select-organization-post-auth.ftl");
        context.challenge(challenge);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return realm.isOrganizationsEnabled();
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No resources to close
    }

    /**
     * Simple POJO exposed to the FreeMarker template.
     */
    public static class OrgBean {
        private final String name;
        private final String alias;

        public OrgBean(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public String getName() {
            return name;
        }

        public String getAlias() {
            return alias;
        }
    }
}
