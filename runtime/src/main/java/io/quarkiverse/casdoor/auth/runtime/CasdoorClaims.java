package io.quarkiverse.casdoor.auth.runtime;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;

final class CasdoorClaims {

    static final String ORGANIZATION_ATTRIBUTE = "casdoor.organization";
    static final String USERNAME_ATTRIBUTE = "casdoor.username";

    private CasdoorClaims() {
    }

    static String organization(JsonWebToken token) {
        return token.getClaim("owner");
    }

    // The JWT token format puts the username in "name"; JWT-Standard puts it in "preferred_username"
    // and the display name in "name".
    static String username(JsonWebToken token) {
        String username = token.getClaim("preferred_username");
        if (username == null || username.isEmpty()) {
            username = token.getClaim("name");
        }
        return username;
    }

    // Casdoor subjects in permissions and enforce requests are "<organization>/<username>".
    static String subject(SecurityIdentity identity) {
        String organization = identity.getAttribute(ORGANIZATION_ATTRIBUTE);
        String username = identity.getAttribute(USERNAME_ATTRIBUTE);
        if (organization == null || username == null) {
            return null;
        }
        return organization + "/" + username;
    }

    static Set<String> roles(JsonWebToken token, UserInfo userInfo) {
        Set<String> roles = new LinkedHashSet<>();
        Object claim = token.getClaim("roles");
        if (claim instanceof JsonArray array) {
            addNames(array, roles);
        }
        if (roles.isEmpty() && userInfo != null) {
            JsonArray array = userInfo.getArray("roles");
            if (array != null) {
                addNames(array, roles);
            }
        }
        return roles;
    }

    // The JWT token format has role objects ({"owner": ..., "name": ...}); UserInfo and JWT-Custom have role names.
    private static void addNames(JsonArray array, Set<String> names) {
        for (JsonValue value : array) {
            if (value instanceof JsonString string) {
                names.add(string.getString());
            } else if (value instanceof JsonObject object && object.get("name") instanceof JsonString name) {
                names.add(name.getString());
            }
        }
    }
}
