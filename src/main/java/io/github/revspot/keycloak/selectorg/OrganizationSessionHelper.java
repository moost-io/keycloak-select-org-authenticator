package io.github.revspot.keycloak.selectorg;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class OrganizationSessionHelper {

    static String getOrganizationId(UserSessionModel userSession) {
        return userSession.getNote(OrganizationModel.ORGANIZATION_ATTRIBUTE);
    }

    static String getOrganizationName(UserSessionModel userSession) {
        return userSession.getNote(OrganizationModel.ORGANIZATION_NAME_ATTRIBUTE);
    }

    static void setSelectedOrganization(AuthenticationFlowContext context, String orgId, String orgName) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, orgId);
        authSession.setClientNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, orgId);
        authSession.setAuthNote(OrganizationModel.ORGANIZATION_NAME_ATTRIBUTE, orgName);
        // additionally add to the user session so that CookieOrganizationAuthenticator could recover from the previous session (e.g., on page reload)
        authSession.setUserSessionNote(OrganizationModel.ORGANIZATION_ATTRIBUTE, orgId);
        authSession.setUserSessionNote(OrganizationModel.ORGANIZATION_NAME_ATTRIBUTE, orgName);
    }
}
