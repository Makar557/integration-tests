package dybr.dev.integrationtests;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MailpitAddress(

        @JsonProperty("Name")
        String name,

        @JsonProperty("Address")
        String address
) {
}