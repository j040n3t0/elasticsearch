/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.core.ml.inference.trainedmodel.inference;

import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.TrainedModel;

import java.io.IOException;

import static org.elasticsearch.xcontent.ToXContent.EMPTY_PARAMS;

final class InferenceModelTestUtils {

    static <T extends TrainedModel, U extends InferenceModel> U deserializeFromTrainedModel(
        T trainedModel,
        NamedXContentRegistry registry,
        CheckedFunction<XContentParser, U, IOException> parser) throws IOException {
        try(XContentBuilder builder = trainedModel.toXContent(XContentFactory.jsonBuilder(), EMPTY_PARAMS);
            XContentParser xContentParser = XContentType.JSON
                .xContent()
                .createParser(registry,
                    DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                    BytesReference.bytes(builder).streamInput())) {
            return parser.apply(xContentParser);
        }
    }


}
