package io.quarkiverse.casdoor.auth.runtime;

import java.util.List;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * HTTP security policy named {@code casdoor}: enforces {@code ["<org>/<user>", "<path>", "<method>"]} with Casdoor.
 * <p>
 * Use it with {@code quarkus.http.auth.permission.<name>.policy=casdoor} or {@code @AuthorizationPolicy(name = "casdoor")}.
 * Anonymous requests are denied.
 */
@Singleton
public class CasdoorHttpSecurityPolicy implements HttpSecurityPolicy {

    public static final String NAME = "casdoor";

    private final CasdoorEnforcer enforcer;

    public CasdoorHttpSecurityPolicy(CasdoorEnforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.flatMap(id -> {
            String subject = CasdoorClaims.subject(id);
            if (id.isAnonymous() || subject == null) {
                return CheckResult.deny();
            }
            String organization = id.getAttribute(CasdoorClaims.ORGANIZATION_ATTRIBUTE);
            return enforcer.enforce(organization, List.of(subject, request.normalizedPath(), request.request().method().name()))
                    .map(allowed -> allowed ? CheckResult.PERMIT : CheckResult.DENY);
        });
    }
}
