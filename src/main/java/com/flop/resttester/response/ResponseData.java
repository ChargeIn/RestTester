/*
 * Rest Tester
 * Copyright (C) 2022-2023 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */

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
