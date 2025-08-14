# Quarkus Casdoor Auth

A Quarkus extension for integrating with Casdoor authentication and authorization service.

## Overview

Quarkus Casdoor Auth is an extension that integrates [Casdoor](https://www.casdoor.com) - an open-source Identity and Access Management (IAM) solution - with Quarkus applications. This extension provides seamless authentication and authorization capabilities based on OAuth 2.0 / OIDC protocols.

## Features

- OAuth 2.0 / OIDC integration with Casdoor server
- JWT token validation and processing
- Automatic redirection to Casdoor login page for unauthenticated users
- Configurable security policies
- Public path exemptions for resources like health checks, metrics, etc.
- User identity extraction and management
- Native mode compatible

## Installation

Add the extension to your project's `pom.xml`:

```xml
<dependency>
    <groupId>casbin.casdoor</groupId>
    <artifactId>quarkus-casdoor-auth</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configure the extension in your `application.properties` file:

```properties
# Casdoor server configuration
quarkus.casdoor.endpoint=https://casdoor.example.org
quarkus.casdoor.organization-name=example
quarkus.casdoor.client-id=your-client-id
quarkus.casdoor.client-secret=your-client-secret
quarkus.casdoor.application-name=example-app
quarkus.casdoor.certificate=path/to/certificate.pem

# OIDC configuration
quarkus.oidc.auth-server-url=${quarkus.casdoor.endpoint}
quarkus.oidc.client-id=${quarkus.casdoor.client-id}
quarkus.oidc.credentials.secret=${quarkus.casdoor.client-secret}
quarkus.oidc.application-type=web-app

# Security configuration (customize as needed)
quarkus.http.auth.permission.public.paths=/,/health/*,/metrics/*,/openapi/*,/swagger-ui/*,/q/*
quarkus.http.auth.permission.public.policy=permit

quarkus.http.auth.permission.secured.paths=/api/*,/secured/*
quarkus.http.auth.permission.secured.policy=authenticated
```

### Configuration Properties Reference

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `quarkus.casdoor.endpoint` | The base URL of your Casdoor server | - | Yes |
| `quarkus.casdoor.organization-name` | The organization name in Casdoor | - | Yes |
| `quarkus.casdoor.client-id` | The client ID for your application | - | Yes |
| `quarkus.casdoor.client-secret` | The client secret for your application | - | Yes |
| `quarkus.casdoor.application-name` | The application name in Casdoor | - | Yes |
| `quarkus.casdoor.certificate` | The certificate used to verify JWT tokens (file path or content) | - | Yes |
| `quarkus.oidc.auth-server-url` | OIDC server URL (usually same as Casdoor endpoint) | ${quarkus.casdoor.endpoint} | No |

## Usage

### Basic Authentication

Once configured, the extension automatically integrates with the Quarkus security framework. Protected endpoints will require valid authentication tokens from Casdoor. Users accessing protected endpoints without authentication will be automatically redirected to the Casdoor login page.

### Securing Endpoints

You can secure your endpoints using standard Jakarta Security annotations like `@RolesAllowed`, `@PermitAll`, and `@DenyAll`. The extension integrates with Quarkus security system to enforce these access controls.

### Access to User Information

You can inject the `SecurityIdentity` interface to access information about the authenticated user, including the user's principal name, roles, and any additional attributes provided by Casdoor.

### Customizing Authentication Logic

For advanced use cases, you can implement your own `CasdoorConfigResolver` interface to customize how the Casdoor configuration is resolved and provide dynamic configuration capabilities.

## Security Policy

The extension implements `HttpSecurityPolicy` to control access to your application's endpoints. By default, the following paths are public and don't require authentication:

- `/` (root path)
- `/health/*` (health check endpoints)
- `/metrics/*` (metrics endpoints)
- `/openapi/*` (OpenAPI documentation)
- `/swagger-ui/*` (Swagger UI)
- `/q/*` (Quarkus dev UI paths)

All other paths require authentication unless explicitly configured as public in your `application.properties`.


## License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)