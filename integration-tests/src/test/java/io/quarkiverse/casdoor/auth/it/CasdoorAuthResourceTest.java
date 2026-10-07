package io.quarkiverse.casdoor.auth.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class CasdoorAuthResourceTest {

    @OidcWireMock
    WireMockServer server;

    @BeforeEach
    void stubEnforce() {
        server.resetRequests();
        server.stubFor(post(urlPathEqualTo("/auth/api/enforce"))
                .willReturn(okJson("{\"status\":\"ok\",\"msg\":\"\",\"data\":[false],\"data2\":[\"\"]}")));
        server.stubFor(post(urlPathEqualTo("/auth/api/enforce"))
                .withQueryParam("owner", equalTo("my-org"))
                .withBasicAuth("quarkus-app", "secret")
                .withRequestBody(equalToJson("[\"my-org/alice\",\"data1\",\"read\"]"))
                .willReturn(okJson("{\"status\":\"ok\",\"msg\":\"\",\"data\":[false,true],\"data2\":[\"\",\"\"]}")));
        server.stubFor(post(urlPathEqualTo("/auth/api/enforce"))
                .withRequestBody(equalToJson("[\"my-org/alice\",\"/api/policy\",\"GET\"]"))
                .willReturn(okJson("{\"status\":\"ok\",\"msg\":\"\",\"data\":[true],\"data2\":[\"\"]}")));
    }

    @Test
    public void rolesFromJwtFormat() {
        given().auth().oauth2(jwtToken("alice"))
                .get("/api/me")
                .then().statusCode(200).body(is("my-org/alice [admin, staff]"));
        given().auth().oauth2(jwtToken("alice"))
                .get("/api/admin")
                .then().statusCode(200);
    }

    @Test
    public void rolesFromStandardFormat() {
        String token = Jwt.preferredUserName("bob")
                .claim("owner", "my-org")
                .claim("name", "Bob Smith")
                .claim("roles", List.of("staff"))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();
        given().auth().oauth2(token)
                .get("/api/me")
                .then().statusCode(200).body(is("my-org/bob [staff]"));
        given().auth().oauth2(token)
                .get("/api/admin")
                .then().statusCode(403);
    }

    @Test
    public void permissionsAllowed() {
        given().auth().oauth2(jwtToken("alice"))
                .get("/api/data1")
                .then().statusCode(200).body(is("data1"));
        given().auth().oauth2(jwtToken("carol"))
                .get("/api/data1")
                .then().statusCode(403);
        given().get("/api/data1")
                .then().statusCode(401);
    }

    @Test
    public void httpSecurityPolicy() {
        given().auth().oauth2(jwtToken("alice"))
                .get("/api/policy")
                .then().statusCode(200).body(is("policy"));
        given().auth().oauth2(jwtToken("carol"))
                .get("/api/policy")
                .then().statusCode(403);
    }

    @Test
    public void enforceErrorDenies() {
        server.stubFor(post(urlPathEqualTo("/auth/api/enforce"))
                .withRequestBody(equalToJson("[\"my-org/dave\",\"data1\",\"read\"]"))
                .willReturn(okJson("{\"status\":\"error\",\"msg\":\"permission not found\"}")));
        given().auth().oauth2(jwtToken("dave"))
                .get("/api/data1")
                .then().statusCode(403);
    }

    // Casdoor's default JWT token format: username in "name", roles as objects
    private static String jwtToken(String username) {
        return Jwt.claim("owner", "my-org")
                .claim("name", username)
                .claim("roles", List.of(
                        Map.of("owner", "my-org", "name", "admin", "displayName", "Admin"),
                        Map.of("owner", "my-org", "name", "staff", "displayName", "Staff")))
                .subject("e4c1f5f4-0000-0000-0000-" + String.format("%012d", username.length()))
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder okJson(String body) {
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body);
    }
}
