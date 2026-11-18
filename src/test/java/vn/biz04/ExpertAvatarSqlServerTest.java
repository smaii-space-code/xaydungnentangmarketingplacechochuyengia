package vn.biz04;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=${BIZ04_TEST_DB_URL}")
@ActiveProfiles(value = "sqlserver-test", inheritProfiles = false)
@ContextConfiguration(initializers = SqlServerIntegrationTest.SafeTestDatabase.class)
@EnabledIfEnvironmentVariable(named = "BIZ04_TEST_SQLSERVER", matches = "true")
class ExpertAvatarSqlServerTest extends ExpertAvatarTest {
    @Test
    void newWebInstanceServesTheSavedReplacementFromTheSameDatabaseAndFileStore() throws Exception {
        publishService();
        avatars.replace(adminId(), expertId(), file(image("png", Color.RED)));
        byte[] expected = image("webp", Color.BLUE);
        avatars.replace(adminId(), expertId(), file(expected));
        try (var restarted = new SpringApplicationBuilder(MarketplaceApplication.class)
                .profiles("sqlserver-test")
                .initializers(new SqlServerIntegrationTest.SafeTestDatabase())
                .run("--server.port=0", "--spring.datasource.url=" + System.getenv("BIZ04_TEST_DB_URL"),
                        "--biz.demo=false", "--biz.scheduler.enabled=false", "--biz.upload-dir=" + UPLOADS)) {
            int port = ((ServletWebServerApplicationContext) restarted).getWebServer().getPort();
            URI root = URI.create("http://127.0.0.1:" + port);
            URI publicImage = root.resolve(publicUrl(expertId()));
            try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build()) {
                var response = client.send(HttpRequest.newBuilder(publicImage).build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, response.statusCode());
                assertEquals("image/webp", response.headers().firstValue("Content-Type").orElseThrow());
                assertArrayEquals(expected, response.body());

                URI login = root.resolve("/dang-nhap");
                String loginToken = csrf(client, login);
                var signIn = HttpRequest.newBuilder(login).header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + encode(loginToken)
                                + "&email=" + encode(admin.getEmail()) + "&password=" + encode(PASSWORD))).build();
                var authenticated = client.send(signIn, HttpResponse.BodyHandlers.discarding());
                assertEquals(302, authenticated.statusCode());
                assertTrue(authenticated.headers().firstValue("Location").orElseThrow().endsWith("/bang-dieu-khien"));
                String formToken = csrf(client, root.resolve(BASE + expertId()));
                assertActualMultipartUpload(client, root, publicImage, formToken);
            }
        }
    }

    private void assertActualMultipartUpload(HttpClient client, URI root, URI publicImage, String token) throws Exception {
        byte[] jpeg = image("jpeg", Color.BLUE);
        String boundary = "AvatarTest" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n"
                + token + "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"avatar\"; filename=\"test.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(jpeg);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var change = HttpRequest.newBuilder(root.resolve(adminUrl(expertId())))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        assertEquals(302, client.send(change, HttpResponse.BodyHandlers.discarding()).statusCode());
        assertArrayEquals(jpeg, client.send(HttpRequest.newBuilder(publicImage).build(), HttpResponse.BodyHandlers.ofByteArray()).body());
    }

    private static String csrf(HttpClient client, URI uri) throws Exception {
        var page = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        var token = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(page.body());
        assertTrue(token.find());
        return token.group(1);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
