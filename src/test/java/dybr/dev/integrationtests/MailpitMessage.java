package dybr.dev.integrationtests;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MailpitMessage(

        @JsonProperty("Subject")
        String subject,

        @JsonProperty("Text")
        String text,

        @JsonProperty("To")
        List<MailpitAddress> to
) {
}