package dybr.dev.integrationtests;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceE2ETest {

    private final RestClient client = RestClient.create("http://localhost:8090");
    private final RestClient mailpitClient = RestClient.create("http://localhost:8025");

    @Test
    void shouldCreateUserThroughGateway() {

        mailpitClient.delete()
                .uri("/api/v1/messages")
                .retrieve()
                .toBodilessEntity();

        String request = """
                {
                    "name": "Integration Test",
                    "email": "m32044403@gmail.com",
                    "age": 25
                }
                """;

        var response = client.post()
                .uri("/api/users")
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .toEntity(UserResponse.class);

        assertEquals(201, response.getStatusCode().value());

        UserResponse userResponse = response.getBody();

        assertNotNull(userResponse);
        assertEquals(25, userResponse.age());
        assertEquals("m32044403@gmail.com", userResponse.email());
        assertEquals("Integration Test", userResponse.name());

        Long userId = userResponse.id();
        assertNotNull(userId);

        awaitForMail(
                "Здравствуйте! Ваш аккаунт на сайте был успешно создан."
        );

        var getResponse = client.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .toEntity(UserResponse.class);

        UserResponse user = getResponse.getBody();

        assertEquals(200, getResponse.getStatusCode().value());

        assertNotNull(user);
        assertEquals(25, user.age());
        assertEquals("m32044403@gmail.com", user.email());
        assertEquals("Integration Test", user.name());
        assertEquals(userId, user.id());

        var delResponse = client.delete()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .toEntity(UserResponse.class);

        UserResponse delUser = delResponse.getBody();

        assertEquals(200, delResponse.getStatusCode().value());

        assertNotNull(delUser);
        assertEquals(25, delUser.age());
        assertEquals("m32044403@gmail.com", delUser.email());
        assertEquals("Integration Test", delUser.name());
        assertEquals(userId, delUser.id());

        awaitForMail(
                "Здравствуйте! Ваш аккаунт был удалён."
        );

        var exception = assertThrows(
                HttpClientErrorException.NotFound.class,
                () -> client.get()
                        .uri("/api/users/{userId}", userId)
                        .retrieve()
                        .toEntity(UserResponse.class)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    private void awaitForMail(String expectedText) {

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> {

                    try {
                        var message = mailpitClient.get()
                                .uri("/api/v1/message/latest")
                                .retrieve()
                                .toEntity(MailpitMessage.class)
                                .getBody();

                        return message != null
                                && "От Макара".equals(message.subject())
                                && message.to() != null
                                && !message.to().isEmpty()
                                && "m32044403@gmail.com"
                                .equals(message.to().getFirst().address())
                                && expectedText.equals(message.text());

                    } catch (HttpClientErrorException.NotFound e) {
                        return false;
                    }
                });
    }

    record MailpitMessage(
            @JsonProperty("Subject")
            String subject,

            @JsonProperty("Text")
            String text,

            @JsonProperty("To")
            List<MailpitAddress> to
    ) {
    }

    record MailpitAddress(
            @JsonProperty("Name")
            String name,

            @JsonProperty("Address")
            String address
    ) {
    }
}