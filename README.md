# Keycloak Select Organization Authenticator

A Keycloak Authenticator SPI that adds a **post-login organization picker** for users who belong to multiple [Keycloak Organizations](https://www.keycloak.org/docs/latest/server_admin/#_organizations).

Keycloak's built-in organization support doesn't include a way for users to select which organization to authenticate into when they belong to more than one. This plugin fills that gap.

## How It Works

When added to a browser authentication flow (after credential/OTP verification):

| User's Orgs | Behavior |
|-------------|----------|
| 0 | Denies access — user must belong to at least one organization |
| 1 | Auto-selects the single org silently |
| 2+ | Renders an org picker page for the user to choose |

The selected organization ID is stored in the authentication session notes (`kc.org`), which Keycloak's built-in **Organization Membership** token mapper reads to include the org in the issued JWT.

## Requirements

- **Keycloak 26.x+** (uses `jakarta.ws.rs` and the Organizations API)
- **Java 17+**
- **Organizations enabled** on the realm

## Build

```bash
mvn clean package
```

Output: `target/keycloak-select-org-authenticator.jar`

### Without Maven (using Docker)

```bash
docker run --rm \
  -v $(pwd):/build \
  -w /build \
  maven:3.9-eclipse-temurin-17 \
  mvn clean package -q
```

## Install

Copy the JAR into Keycloak's `providers/` directory and rebuild:

```bash
cp target/keycloak-select-org-authenticator.jar /opt/keycloak/providers/
/opt/keycloak/bin/kc.sh build
```

Then restart Keycloak.

### Docker

If you build a custom Keycloak image, add the JAR in your Dockerfile:

```dockerfile
COPY keycloak-select-org-authenticator.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
```

## Configure

1. Go to **Authentication** > **Flows** in the Keycloak Admin Console
2. Select (or duplicate) your browser flow
3. In the sub-flow that contains your credential steps, click **Add step**
4. Search for **Select Organization (Post-Auth)**
5. Set it to **REQUIRED**
6. Position it **after** your credential/OTP steps
7. Bind the flow to the browser flow for your realm

### Example Flow

```
Browser Flow:
  Cookie (ALTERNATIVE)
  Login Forms (ALTERNATIVE sub-flow):
    Username Password Form (REQUIRED)
    OTP Form (OPTIONAL)
    Select Organization (REQUIRED)    <-- this plugin
```

## Template

The plugin includes a default `select-organization-post-auth.ftl` template (bundled via `theme-resources/templates/`). It works with any Keycloak theme out of the box.

To customize the picker UI, create `select-organization-post-auth.ftl` in your own theme's `login/` directory. The authenticator passes these template attributes:

| Attribute | Type | Description |
|-----------|------|-------------|
| `organizations` | `List<OrgBean>` | User's enabled organizations |
| `username` | `String` | User's email or username |

Each `OrgBean` exposes:
- `.name` — Organization display name
- `.alias` — Organization alias (URL-safe identifier)

## Session Notes

| Note | Scope | Value | Consumer |
|------|-------|-------|----------|
| `kc.org` | authNote + clientNote | Organization ID (UUID) | Keycloak Organization Membership mapper |
| `kc.org.name` | authNote | Organization name | Available for custom mappers |

## Compatibility

| Keycloak Version | Status |
|------------------|--------|
| 26.6.0           | Tested |
| 26.x             | Should work |
| 25.x             | Untested (Organizations API may differ) |
| 27.x             | Should work |

## License

[Apache License 2.0](LICENSE)
