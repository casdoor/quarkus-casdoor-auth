package io.quarkiverse.casdoor.auth.it;

import java.util.TreeSet;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.security.AuthorizationPolicy;

@Path("/api")
public class CasdoorAuthResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me")
    @Authenticated
    public String me() {
        return identity.getAttribute("casdoor.organization") + "/" + identity.getAttribute("casdoor.username") + " "
                + new TreeSet<>(identity.getRoles());
    }

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String admin() {
        return "admin";
    }

    @GET
    @Path("/data1")
    @PermissionsAllowed("data1:read")
    public String data1() {
        return "data1";
    }

    @GET
    @Path("/policy")
    @AuthorizationPolicy(name = "casdoor")
    public String policy() {
        return "policy";
    }
}
