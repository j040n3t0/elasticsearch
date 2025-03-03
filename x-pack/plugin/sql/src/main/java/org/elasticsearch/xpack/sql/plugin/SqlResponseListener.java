/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.sql.plugin;

import org.elasticsearch.xcontent.MediaType;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestResponseListener;
import org.elasticsearch.xpack.sql.action.SqlQueryRequest;
import org.elasticsearch.xpack.sql.action.SqlQueryResponse;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.elasticsearch.xpack.sql.proto.Protocol.HEADER_NAME_ASYNC_ID;
import static org.elasticsearch.xpack.sql.proto.Protocol.HEADER_NAME_ASYNC_PARTIAL;
import static org.elasticsearch.xpack.sql.proto.Protocol.HEADER_NAME_ASYNC_RUNNING;
import static org.elasticsearch.xpack.sql.proto.Protocol.HEADER_NAME_CURSOR;
import static org.elasticsearch.xpack.sql.proto.Protocol.HEADER_NAME_TOOK_NANOS;
import static org.elasticsearch.xpack.sql.proto.Protocol.URL_PARAM_DELIMITER;

class SqlResponseListener extends RestResponseListener<SqlQueryResponse> {

    private final long startNanos = System.nanoTime();
    private final MediaType mediaType;
    private final RestRequest request;


    SqlResponseListener(RestChannel channel, RestRequest request, SqlQueryRequest sqlRequest) {
        super(channel);
        this.request = request;

        this.mediaType = SqlMediaTypeParser.getResponseMediaType(request, sqlRequest);

        /*
         * Special handling for the "delimiter" parameter which should only be
         * checked for being present or not in the case of CSV format. We cannot
         * override {@link BaseRestHandler#responseParams()} because this
         * parameter should only be checked for CSV, not always.
         */
        if (mediaType != TextFormat.CSV && request.hasParam(URL_PARAM_DELIMITER)) {
            String message = String.format(Locale.ROOT, "request [%s] contains unrecognized parameter: [" + URL_PARAM_DELIMITER + "]",
                request.path());
            throw new IllegalArgumentException(message);
        }
    }

    SqlResponseListener(RestChannel channel, RestRequest request) {
        super(channel);
        this.request = request;
        this.mediaType = SqlMediaTypeParser.getResponseMediaType(request);
    }

    @Override
    public RestResponse buildResponse(SqlQueryResponse response) throws Exception {
        RestResponse restResponse;

        // XContent branch
        if (mediaType instanceof XContentType) {
            XContentType type = (XContentType) mediaType;
            XContentBuilder builder = channel.newBuilder(request.getXContentType(), type, true);
            response.toXContent(builder, request);
            restResponse = new BytesRestResponse(RestStatus.OK, builder);
        } else { // TextFormat
            TextFormat type = (TextFormat) mediaType;
            final String data = type.format(request, response);

            restResponse = new BytesRestResponse(RestStatus.OK, type.contentType(request),
                data.getBytes(StandardCharsets.UTF_8));

            if (response.hasCursor()) {
                restResponse.addHeader(HEADER_NAME_CURSOR, response.cursor());
            }

            if (response.hasId()) {
                restResponse.addHeader(HEADER_NAME_ASYNC_ID, response.id());
                restResponse.addHeader(HEADER_NAME_ASYNC_PARTIAL, String.valueOf(response.isPartial()));
                restResponse.addHeader(HEADER_NAME_ASYNC_RUNNING, String.valueOf(response.isRunning()));
            }
        }

        restResponse.addHeader(HEADER_NAME_TOOK_NANOS, Long.toString(System.nanoTime() - startNanos));
        return restResponse;
    }
}
