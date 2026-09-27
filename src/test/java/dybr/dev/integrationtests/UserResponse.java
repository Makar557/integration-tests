package dybr.dev.integrationtests;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age
) {}
