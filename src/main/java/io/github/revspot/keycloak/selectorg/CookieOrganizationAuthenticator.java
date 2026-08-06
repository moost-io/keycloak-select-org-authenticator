package io.github.revspot.keycloak.selectorg;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.models.*;
import org.keycloak.organization.protocol.mappers.oidc.OrganizationScope;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.stream.Collectors;

/**
 * Derived from org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory
 */
public class CookieOrganizationAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(CookieOrganizationAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(),
                context.getRealm(), true);
        if (authResult == null) {
            log.info("ATTEMPTED: missing AuthResult from cookie authentication");
            context.attempted();
        } else {
            UserSessionModel userSession = authResult.session();
            log.info("authenticate with cookie: " + (userSession != null && userSession.getUser() != null ? userSession.getUser().getUsername() : "(no username)"));
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            LoginProtocol protocol = context.getSession().getProvider(LoginProtocol.class, authSession.getProtocol());
            authSession.setAuthNote(Constants.LOA_MAP, userSession.getNote(Constants.LOA_MAP));
            context.setUser(authResult.user());
            AcrStore acrStore = new AcrStore(context.getSession(), authSession);

            // Cookie re-authentication is skipped if re-authentication is required
            if (protocol.requireReauthentication(userSession, authSession)) {
                // Full re-authentication, so we start with no loa
                acrStore.setLevelAuthenticatedToCurrentRequest(Constants.NO_LOA);
                authSession.setAuthNote(AuthenticationManager.FORCED_REAUTHENTICATION, "true");
                context.setForwardedInfoMessage(Messages.REAUTHENTICATE);
                log.info("ATTEMPTED: requires reauthentication");
                context.attempted();
            } else if(AuthenticatorUtil.isForkedFlow(authSession)){
                context.attempted();
                log.info("ATTEMPTED: forked flow");
            } else {
                String topLevelFlowId = context.getTopLevelFlow().getId();
                int previouslyAuthenticatedLevel = acrStore.getHighestAuthenticatedLevelFromPreviousAuthentication(topLevelFlowId);
                AuthenticatorUtils.updateCompletedExecutions(context.getAuthenticationSession(), userSession, context.getExecution().getId());

                if (acrStore.getRequestedLevelOfAuthentication(context.getTopLevelFlow()) > previouslyAuthenticatedLevel) {
                    // Step-up authentication, we keep the loa from the existing user session.
                    // The cookie alone is not enough and other authentications must follow.
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);

                    if (authSession.getClientNote(Constants.KC_ACTION) != null) {
                        context.setForwardedInfoMessage(Messages.AUTHENTICATE_STRONG);
                    }
                    log.info("ATTEMPTED: the cookie alone is not enough and other authentications must follow: " +
                            acrStore.getRequestedLevelOfAuthentication(context.getTopLevelFlow()) + " > " + previouslyAuthenticatedLevel
                    );
                    context.attempted();
                } else {
                    // Cookie only authentication
                    acrStore.setLevelAuthenticatedToCurrentRequest(previouslyAuthenticatedLevel);
                    authSession.setAuthNote(AuthenticationManager.SSO_AUTH, "true");
                    context.attachUserSession(userSession);

                    if (isOrganizationContext(context)) {
                        // Remark: original CookieAuthenticator would trigger re-authentication - this is the relevant change here
                        log.info("Prev session notes: " + userSession.getNotes().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")));
                        String previousOrgId = OrganizationSessionHelper.getOrganizationId(userSession);
                        String previousOrgName = OrganizationSessionHelper.getOrganizationName(userSession);
                        if (previousOrgId != null && previousOrgName != null) {
                            OrganizationSessionHelper.setSelectedOrganization(context, previousOrgId, previousOrgName);
                            log.info("SUCCESS: authentication for organization context ok");
                            context.success();
                        } else {
                            log.info("ATTEMPTED: authentication for organization context failed, no previous org found");
                            context.attempted();
                        }
                    } else {
                        log.info("SUCCESS: authentication ok");
                        context.success();
                    }
                }
            }
        }

    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }

    private boolean isOrganizationContext(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();

        if (Organizations.isEnabledAndOrganizationsPresent(session)) {
            return OrganizationScope.valueOfScope(session) != null;
        }

        return false;
    }
}
