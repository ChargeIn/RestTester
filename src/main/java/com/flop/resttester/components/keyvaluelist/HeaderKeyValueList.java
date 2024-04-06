/*
 * Rest Tester
 * Copyright (C) 2022-2024 Florian Plesker <florian dot plesker at web dot de>
 *
 * This file is licensed under LGPLv3
 */
package com.flop.resttester.components.keyvaluelist;

import java.util.List;

public class HeaderKeyValueList extends KeyValueList {

    @Override
    String getKeyPlaceholder() {
        return "Header";
    }

    @Override
    String getValuePlaceholder() {
        return "Value";
    }

    /**
     * Returns the most common header names
     */
    @Override
    List<String> getKeyProposals() {
        return List.of(
                "A-IM",
                "Accept",
                "Accept-Charset",
                "Accept-Datetime",
                "Accept-Encoding",
                "Accept-Language",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Authorization",
                "Cache-Control",
                "Connection",
                "Content-Encoding",
                "Content-Length",
                "Content-MD5",
                "Content-Type",
                "Cookie",
                "Date",
                "Expect",
                "Forwarded",
                "From",
                "Host",
                "HTTP2-Settings",
                "If-Match",
                "If-Modified-Since",
                "If-None-Match",
                "If-Range",
                "If-Unmodified-Since",
                "Max-Forwards",
                "Origin",
                "Pragma",
                "Prefer",
                "Proxy-Authorization",
                "Range",
                "Referer",
                "TE",
                "Trailer",
                "Transfer-Encoding",
                "User-Agent",
                "Upgrade",
                "Via",
                "Warning",
                "WWW-Authenticate:"
        );
    }

    @Override
    List<String> getValueProposals() {
        return List.of(
                "application/json",
                "application/ld+json",
                "application/msword",
                "application/pdf",
                "application/sql",
                "application/vnd.api+json",
                "application/vnd.microsoft.portable-executable",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/x-www-form-urlencoded",
                "application/xml",
                "application/zip",
                "application/zstd",
                "audio/mpeg",
                "audio/ogg",
                "Basic ",
                "Bearer ",
                "image/avif",
                "image/jpeg",
                "image/png",
                "image/svg+xml",
                "image/tiff",
                "keep-alive",
                "model/obj",
                "multipart/form-data",
                "Origin",
                "text/plain",
                "text/css",
                "text/csv",
                "text/html",
                "text/javascript",
                "text/xml"
        );
    }
}
