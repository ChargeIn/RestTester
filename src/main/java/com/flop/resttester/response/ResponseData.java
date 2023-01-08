package com.flop.resttester.response;

import java.util.List;
import java.util.Map;

public record ResponseData(
        Map<String, List<String>> headers,
        int code,
        byte[] content,
        String elapsedTime
) {
}
