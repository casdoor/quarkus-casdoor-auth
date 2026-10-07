package io.quarkiverse.casdoor.auth.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * Calls the Casdoor {@code /api/enforce} API with the credentials of the Casdoor application.
 */
@Startup
@Singleton
public class CasdoorEnforcer {

    private static final Logger LOG = Logger.getLogger(CasdoorEnforcer.class);
    private static final int MAX_CACHE_SIZE = 10_000;

    private final CasdoorConfig.Authorization config;
    private final WebClient client;
    private final String enforceUrl;
    private final String clientId;
    private final String clientSecret;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public CasdoorEnforcer(CasdoorConfig casdoorConfig, Vertx vertx) {
        this.config = casdoorConfig.authorization();
        this.client = WebClient.create(vertx);

        Config mpConfig = ConfigProvider.getConfig();
        String endpoint = casdoorConfig.endpoint()
                .or(() -> mpConfig.getOptionalValue("quarkus.oidc.auth-server-url", String.class))
                .orElse(null);
        this.enforceUrl = endpoint == null ? null : stripTrailingSlash(endpoint) + "/api/enforce";
        this.clientId = casdoorConfig.clientId()
                .or(() -> mpConfig.getOptionalValue("quarkus.oidc.client-id", String.class))
                .orElse(null);
        this.clientSecret = casdoorConfig.clientSecret()
                .or(() -> mpConfig.getOptionalValue("quarkus.oidc.credentials.secret", String.class))
                .or(() -> mpConfig.getOptionalValue("quarkus.oidc.credentials.client-secret.value", String.class))
                .orElse(null);

        long targets = List.of(config.permissionId(), config.modelId(), config.resourceId(), config.enforcerId())
                .stream().filter(Optional::isPresent).count();
        if (targets > 1) {
            throw new IllegalStateException("Set at most one of quarkus.casdoor.authorization.permission-id, "
                    + "model-id, resource-id and enforcer-id");
        }
    }

    @PreDestroy
    void close() {
        client.close();
    }

    /**
     * Enforces a Casbin request, for example {@code ["my-org/alice", "data1", "read"]}.
     *
     * @param organization the user's organization, whose permissions are checked when no permission, model, resource
     *        or enforcer is configured
     * @param request the Casbin request
     * @return whether any of the matched Casdoor permissions allows the request; {@code false} if the call fails
     */
    public Uni<Boolean> enforce(String organization, List<String> request) {
        if (enforceUrl == null || clientId == null || clientSecret == null) {
            LOG.warn("Casdoor endpoint or application credentials are not configured, denying access");
            return Uni.createFrom().item(false);
        }

        String key = organization + "\n" + String.join("\n", request);
        Duration ttl = config.cacheTtl();
        if (!ttl.isZero()) {
            CachedResult cached = cache.get(key);
            if (cached != null && cached.expiresAt > System.nanoTime()) {
                return Uni.createFrom().item(cached.allowed);
            }
        }

        HttpRequest<?> httpRequest = client.postAbs(enforceUrl)
                .basicAuthentication(clientId, clientSecret)
                .timeout(config.timeout().toMillis());
        if (config.permissionId().isPresent()) {
            httpRequest.addQueryParam("permissionId", config.permissionId().get());
        } else if (config.modelId().isPresent()) {
            httpRequest.addQueryParam("modelId", config.modelId().get());
        } else if (config.resourceId().isPresent()) {
            httpRequest.addQueryParam("resourceId", config.resourceId().get());
        } else if (config.enforcerId().isPresent()) {
            httpRequest.addQueryParam("enforcerId", config.enforcerId().get());
        } else {
            httpRequest.addQueryParam("owner", organization);
        }

        return httpRequest.sendJson(new JsonArray(List.copyOf(request)))
                .map(response -> {
                    if (response.statusCode() != 200) {
                        LOG.warnf("Casdoor enforce returned HTTP %d: %s", response.statusCode(), response.bodyAsString());
                        return false;
                    }
                    // Casdoor reports errors with HTTP 200 and "status": "error"
                    JsonObject body = response.bodyAsJsonObject();
                    if (!"ok".equals(body.getString("status"))) {
                        LOG.warnf("Casdoor enforce failed: %s", body.getString("msg"));
                        return false;
                    }
                    boolean allowed = body.getJsonArray("data", new JsonArray()).stream()
                            .anyMatch(Boolean.TRUE::equals);
                    if (!ttl.isZero()) {
                        if (cache.size() >= MAX_CACHE_SIZE) {
                            cache.clear();
                        }
                        cache.put(key, new CachedResult(allowed, System.nanoTime() + ttl.toNanos()));
                    }
                    return allowed;
                })
                .onFailure().recoverWithItem(failure -> {
                    LOG.warnf(failure, "Casdoor enforce request to %s failed", enforceUrl);
                    return false;
                });
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record CachedResult(boolean allowed, long expiresAt) {
    }
}
