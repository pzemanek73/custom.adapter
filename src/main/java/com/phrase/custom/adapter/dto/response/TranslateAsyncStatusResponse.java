package com.phrase.custom.adapter.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.EnumUtils.getEnum;
import static org.apache.commons.lang3.StringUtils.toRootUpperCase;

public record TranslateAsyncStatusResponse(@NotNull AsyncStatus status, @Nullable String detail) {

    public enum AsyncStatus {
        RUNNING("running"),
        DONE("done"),
        FAILED("failed");

        private final String value;

        AsyncStatus(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JsonCreator
        public static AsyncStatus fromValue(String value) {
            AsyncStatus asyncStatus = getEnum(AsyncStatus.class, toRootUpperCase(value));
            if (isNull(asyncStatus)) {
                throw new IllegalArgumentException("Invalid status value: %s".formatted(value));
            }
            return asyncStatus;
        }

    }

}