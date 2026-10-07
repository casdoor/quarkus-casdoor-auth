# Quarkus Casdoor Auth

[![Build](https://img.shields.io/github/actions/workflow/status/casdoor/quarkus-casdoor-auth/build.yml?branch=master&style=flat-square&label=build)](https://github.com/casdoor/quarkus-casdoor-auth/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.quarkiverse.casdoor-auth/quarkus-casdoor-auth?style=flat-square&logo=apache-maven&color=blue)](https://central.sonatype.com/artifact/io.quarkiverse.casdoor-auth/quarkus-casdoor-auth)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.40-4695EB?style=flat-square&logo=quarkus&logoColor=white)](https://quarkus.io)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![Casdoor](https://img.shields.io/badge/Casdoor-casdoor.ai-2C7BE5?style=flat-square)](https://casdoor.ai)
[![Discord](https://img.shields.io/discord/1022748306096537660?style=flat-square&logo=discord&label=Discord&color=5865F2)](https://discord.gg/5rPsrAzK7S)
[![License](https://img.shields.io/github/license/casdoor/quarkus-casdoor-auth?style=flat-square&color=orange)](LICENSE)

A Quarkus extension for [Casdoor](https://casdoor.ai), an open-source identity and access management platform with OAuth 2.0, OIDC, SAML, LDAP and Casbin-based permissions.

The extension builds on `quarkus-oidc`, which handles sign-in and token verification, and adds what is specific to Casdoor:

- **Roles**: the user's Casdoor roles become Quarkus roles, so `@RolesAllowed` works with both the `JWT` and `JWT-Standard` token formats
- **Permissions**: `@PermissionsAllowed("resource:action")` is checked against Casdoor permissions with the Casdoor `/api/enforce` API
- **Path-based authorization**: the `casdoor` HTTP security policy checks the request path and method against Casdoor permissions

## Installation

```xml
<dependency>
    <groupId>io.quarkiverse.casdoor-auth</groupId>
    <artifactId>quarkus-casdoor-auth</artifactId>
    <version>${quarkus-casdoor-auth.version}</version>
</dependency>
```

## Configuration

Create an application in Casdoor and point `quarkus-oidc` at your Casdoor server:

```properties
quarkus.oidc.auth-server-url=https://door.casdoor.com
quarkus.oidc.client-id=<client ID>
quarkus.oidc.credentials.secret=<client secret>
# web-app: redirect users to the Casdoor sign-in page; service (the default): accept bearer tokens only
quarkus.oidc.application-type=web-app
```

## Usage

```java
@Path("/api")
public class OrderResource {

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String admin() {
        return "admin";
    }

    // Casbin request ["<organization>/<username>", "orders", "read"]
    @GET
    @Path("/orders")
    @PermissionsAllowed("orders:read")
    public String orders() {
        return "orders";
    }

    // Casbin request ["<organization>/<username>", "/api/reports", "GET"]
    @GET
    @Path("/reports")
    @AuthorizationPolicy(name = "casdoor")
    public String reports() {
        return "reports";
    }
}
```

See the [documentation](https://docs.quarkiverse.io/quarkus-casdoor-auth/dev/) for role mapping, permission targets, caching and all configuration properties.

## License

[Apache License 2.0](LICENSE)
