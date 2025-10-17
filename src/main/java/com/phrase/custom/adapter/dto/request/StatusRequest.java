package com.phrase.custom.adapter.dto.request;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record StatusRequest(@Nullable Map<String, Object> metadata) {
}
