package com.phrase.custom.adapter.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.EnumUtils.getEnum;
import static org.apache.commons.lang3.StringUtils.toRootUpperCase;

public record StatusResponse(@NotNull Status status) {

    public enum Status {
        OK("ok"),
        NOT_OK("not_ok");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            Status status = getEnum(Status.class, toRootUpperCase(value));
            if (isNull(status)) {
                throw new IllegalArgumentException("Invalid status value: %s".formatted(value));
            }
            return status;
        }

    }

}
