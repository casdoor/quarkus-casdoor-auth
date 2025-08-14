// Copyright 2025 The Casdoor Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package casbin.casdoor.quarkus.auth.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import org.casbin.casdoor.exception.AuthException;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.service.AuthService;

import java.util.function.Function;

@Singleton
public class CasdoorHttpSecurityPolicy implements HttpSecurityPolicy {

    @Inject
    CasdoorConfigResolver configResolver;
    
    @Inject
    AuthService authService;

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        
        return identity.flatMap(new Function<SecurityIdentity, Uni<? extends CheckResult>>() {
            @Override
            public Uni<? extends CheckResult> apply(SecurityIdentity identity) {
                if (identity.isAnonymous()) {
                    return checkAnonymousAccess(routingContext);
                }
                
                return checkAuthenticatedAccess(routingContext, identity);
            }
        });
    }

    /**
     * Checks access for anonymous users.
     * 
     * @param routingContext the routing context
     * @return the check result
     */
    private Uni<CheckResult> checkAnonymousAccess(RoutingContext routingContext) {
        String path = routingContext.normalizedPath();
        
        if (isPublicPath(path)) {
            return CheckResult.permit();
        }
        
        return CheckResult.deny();
    }

    /**
     * Checks access for authenticated users.
     * Uses the AuthService from Casdoor Java SDK to validate the token
     * and extract user information.
     * 
     * @param routingContext the routing context
     * @param identity the authenticated security identity
     * @return the check result
     */
    private Uni<CheckResult> checkAuthenticatedAccess(RoutingContext routingContext, SecurityIdentity identity) {
        AccessTokenCredential credential = identity.getCredential(AccessTokenCredential.class);
        
        if (credential == null) {
            return CheckResult.deny();
        }
        
        String token = credential.getToken();
        
        if (token == null || token.isEmpty()) {
            return CheckResult.deny();
        }
        
        try {
            User user = authService.parseJwtToken(token);
            
            if (user == null) {
                return CheckResult.deny();
            }
            
            String username = user.name;
            if (username == null || username.isEmpty()) {
                return CheckResult.deny();
            }
            
            return CheckResult.permit();
            
        } catch (AuthException e) {
            System.err.println("Error validating Casdoor token: " + e.getMessage());
            return CheckResult.deny();
        } catch (Exception e) {
            System.err.println("Unexpected error during Casdoor authorization: " + e.getMessage());
            return CheckResult.deny();
        }
    }

    /**
     * Extracts the user ID from the security identity.
     * 
     * @param identity the security identity
     * @return the user ID, or null if not found
     */
    private String extractUserId(SecurityIdentity identity) {
        if (identity == null) {
            return null;
        }
        
        String userId = identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
        
        if (userId == null || userId.isEmpty()) {
            userId = identity.getAttribute("sub");
            if (userId == null) {
                userId = identity.getAttribute("preferred_username");
            }
            if (userId == null) {
                userId = identity.getAttribute("username");
            }
        }
        
        return userId;
    }

    /**
     * Checks if the given path is a public path that doesn't require authentication.
     * 
     * @param path the request path
     * @return true if the path is public, false otherwise
     */
    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        
        return path.equals("/") ||
               path.startsWith("/health") ||
               path.startsWith("/metrics") ||
               path.startsWith("/openapi") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/q/") ||
               path.startsWith("/quarkus-casdoor-auth");
    }
}
