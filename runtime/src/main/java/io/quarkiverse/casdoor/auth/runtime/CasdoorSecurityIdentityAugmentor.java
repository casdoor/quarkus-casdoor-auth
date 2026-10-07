package io.quarkiverse.casdoor.auth.runtime;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Adds the Casdoor organization, username and roles to the identity, and checks {@code @PermissionsAllowed}
 * permissions with Casdoor: {@code @PermissionsAllowed("data1:read")} enforces {@code ["<org>/<user>", "data1", "read"]}.
 */
@Singleton
public class CasdoorSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    private final CasdoorConfig config;
    private final CasdoorEnforcer enforcer;

    public CasdoorSecurityIdentityAugmentor(CasdoorConfig config, CasdoorEnforcer enforcer) {
        this.config = config;
        this.enforcer = enforcer;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken token)) {
            return Uni.createFrom().item(identity);
        }

        String organization = CasdoorClaims.organization(token);
        String username = CasdoorClaims.username(token);
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
        if (organization != null && username != null) {
            builder.addAttribute(CasdoorClaims.ORGANIZATION_ATTRIBUTE, organization);
            builder.addAttribute(CasdoorClaims.USERNAME_ATTRIBUTE, username);
        }
        if (config.roles().enabled()) {
            UserInfo userInfo = identity.getAttribute("userinfo");
            builder.addRoles(CasdoorClaims.roles(token, userInfo));
        }
        if (config.authorization().enabled() && organization != null && username != null) {
            String subject = organization + "/" + username;
            builder.addPermissionChecker(permission -> checkPermission(organization, subject, permission));
        }
        return Uni.createFrom().item(builder.build());
    }

    private Uni<Boolean> checkPermission(String organization, String subject, Permission permission) {
        if (!(permission instanceof StringPermission)) {
            return Uni.createFrom().item(false);
        }

        String actions = permission.getActions();
        if (actions == null || actions.isEmpty()) {
            return enforcer.enforce(organization, List.of(subject, permission.getName()));
        }

        // Several actions on one resource, e.g. @PermissionsAllowed({"data1:read", "data1:write"}): any of them is enough
        List<Uni<Boolean>> checks = new ArrayList<>();
        for (String action : actions.split(",")) {
            checks.add(enforcer.enforce(organization, List.of(subject, permission.getName(), action)));
        }
        return Multi.createFrom().iterable(checks)
                .onItem().transformToUniAndConcatenate(check -> check)
                .filter(Boolean::booleanValue)
                .toUni()
                .map(allowed -> allowed != null);
    }
}
