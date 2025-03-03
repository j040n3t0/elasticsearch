/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.elasticsearch.action.ingest.DeletePipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.ingest.SimulatePipelineRequest;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.test.ESTestCase;
import org.junit.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class IngestRequestConvertersTests extends ESTestCase {

    public void testPutPipeline() throws IOException {
        String pipelineId = "some_pipeline_id";
        PutPipelineRequest request = new PutPipelineRequest(
            "some_pipeline_id",
            new BytesArray("{}".getBytes(StandardCharsets.UTF_8)),
            XContentType.JSON
        );
        Map<String, String> expectedParams = new HashMap<>();
        RequestConvertersTests.setRandomMasterTimeout(request, expectedParams);
        RequestConvertersTests.setRandomTimeout(request::timeout, AcknowledgedRequest.DEFAULT_ACK_TIMEOUT, expectedParams);

        Request expectedRequest = IngestRequestConverters.putPipeline(request);
        StringJoiner endpoint = new StringJoiner("/", "/", "");
        endpoint.add("_ingest/pipeline");
        endpoint.add(pipelineId);
        Assert.assertEquals(endpoint.toString(), expectedRequest.getEndpoint());
        Assert.assertEquals(HttpPut.METHOD_NAME, expectedRequest.getMethod());
        Assert.assertEquals(expectedParams, expectedRequest.getParameters());
    }

    public void testGetPipeline() {
        String pipelineId = "some_pipeline_id";
        Map<String, String> expectedParams = new HashMap<>();
        GetPipelineRequest request = new GetPipelineRequest("some_pipeline_id");
        RequestConvertersTests.setRandomMasterTimeout(request, expectedParams);
        Request expectedRequest = IngestRequestConverters.getPipeline(request);
        StringJoiner endpoint = new StringJoiner("/", "/", "");
        endpoint.add("_ingest/pipeline");
        endpoint.add(pipelineId);
        Assert.assertEquals(endpoint.toString(), expectedRequest.getEndpoint());
        Assert.assertEquals(HttpGet.METHOD_NAME, expectedRequest.getMethod());
        Assert.assertEquals(expectedParams, expectedRequest.getParameters());
    }

    public void testDeletePipeline() {
        String pipelineId = "some_pipeline_id";
        Map<String, String> expectedParams = new HashMap<>();
        DeletePipelineRequest request = new DeletePipelineRequest(pipelineId);
        RequestConvertersTests.setRandomMasterTimeout(request, expectedParams);
        RequestConvertersTests.setRandomTimeout(request::timeout, AcknowledgedRequest.DEFAULT_ACK_TIMEOUT, expectedParams);
        Request expectedRequest = IngestRequestConverters.deletePipeline(request);
        StringJoiner endpoint = new StringJoiner("/", "/", "");
        endpoint.add("_ingest/pipeline");
        endpoint.add(pipelineId);
        Assert.assertEquals(endpoint.toString(), expectedRequest.getEndpoint());
        Assert.assertEquals(HttpDelete.METHOD_NAME, expectedRequest.getMethod());
        Assert.assertEquals(expectedParams, expectedRequest.getParameters());
    }

    public void testSimulatePipeline() throws IOException {
        String pipelineId = ESTestCase.randomBoolean() ? "some_pipeline_id" : null;
        boolean verbose = ESTestCase.randomBoolean();
        String json = "{"
            + "  \"pipeline\": {"
            + "    \"description\": \"_description\","
            + "    \"processors\": ["
            + "      {"
            + "        \"set\": {"
            + "          \"field\": \"field2\","
            + "          \"value\": \"_value\""
            + "        }"
            + "      }"
            + "    ]"
            + "  },"
            + "  \"docs\": ["
            + "    {"
            + "      \"_index\": \"index\","
            + "      \"_type\": \"_doc\","
            + "      \"_id\": \"id\","
            + "      \"_source\": {"
            + "        \"foo\": \"rab\""
            + "      }"
            + "    }"
            + "  ]"
            + "}";
        SimulatePipelineRequest request = new SimulatePipelineRequest(
            new BytesArray(json.getBytes(StandardCharsets.UTF_8)),
            XContentType.JSON
        );
        request.setId(pipelineId);
        request.setVerbose(verbose);
        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put("verbose", Boolean.toString(verbose));

        Request expectedRequest = IngestRequestConverters.simulatePipeline(request);
        StringJoiner endpoint = new StringJoiner("/", "/", "");
        endpoint.add("_ingest/pipeline");
        if (pipelineId != null && pipelineId.isEmpty() == false) {
            endpoint.add(pipelineId);
        }
        endpoint.add("_simulate");
        Assert.assertEquals(endpoint.toString(), expectedRequest.getEndpoint());
        Assert.assertEquals(HttpPost.METHOD_NAME, expectedRequest.getMethod());
        Assert.assertEquals(expectedParams, expectedRequest.getParameters());
        RequestConvertersTests.assertToXContentBody(request, expectedRequest.getEntity());
    }
}
