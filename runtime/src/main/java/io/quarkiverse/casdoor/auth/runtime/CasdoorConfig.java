package io.quarkiverse.casdoor.auth.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Casdoor configuration.
 * <p>
 * Sign-in is handled by {@code quarkus-oidc}: point {@code quarkus.oidc.auth-server-url} at the Casdoor server and set
 * {@code quarkus.oidc.client-id} and {@code quarkus.oidc.credentials.secret} to the Casdoor application's credentials.
 * The properties below only cover what is specific to Casdoor.
 */
@ConfigMapping(prefix = "quarkus.casdoor")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CasdoorConfig {

    /**
     * Base URL of the Casdoor server, for example {@code https://door.casdoor.com}.
     * Defaults to {@code quarkus.oidc.auth-server-url}.
     */
    Optional<String> endpoint();

    /**
     * Client ID used to call the Casdoor API. Defaults to {@code quarkus.oidc.client-id}.
     */
    Optional<String> clientId();

    /**
     * Client secret used to call the Casdoor API. Defaults to {@code quarkus.oidc.credentials.secret}.
     */
    Optional<String> clientSecret();

    /**
     * Role mapping.
     */
    Roles roles();

    /**
     * Authorization with Casdoor permissions.
     */
    Authorization authorization();

    interface Roles {

        /**
         * Add the names of the user's Casdoor roles to the security identity, so that {@code @RolesAllowed} works.
         * Roles are read from the {@code roles} claim of the token; with the {@code JWT-Standard} token format they are
         * read from the UserInfo response, which requires {@code quarkus.oidc.authentication.user-info-required=true}.
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface Authorization {

        /**
         * Check {@code @PermissionsAllowed} permissions and the {@code casdoor} HTTP security policy with the Casdoor
         * {@code /api/enforce} API.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Casdoor permission to enforce, as {@code <organization>/<name>}.
         * <p>
         * Set at most one of {@code permission-id}, {@code model-id}, {@code resource-id} and {@code enforcer-id}.
         * If none is set, all permissions of the user's organization are checked, and access is granted if any of them
         * allows the request.
         */
        Optional<String> permissionId();

        /**
         * Casdoor model whose permissions are enforced, as {@code <organization>/<name>}.
         */
        Optional<String> modelId();

        /**
         * Casdoor resource whose permissions are enforced.
         */
        Optional<String> resourceId();

        /**
         * Casdoor enforcer to use, as {@code <organization>/<name>}.
         */
        Optional<String> enforcerId();

        /**
         * How long an enforce result is cached. {@code 0} disables the cache, so that permission changes in Casdoor
         * take effect immediately.
         */
        @WithDefault("0S")
        Duration cacheTtl();

        /**
         * Timeout of a call to the Casdoor API.
         */
        @WithDefault("10S")
        Duration timeout();
    }
}
