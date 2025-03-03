/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.xpack.vectors.query;

import org.apache.lucene.search.KnnVectorQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xpack.vectors.mapper.DenseVectorFieldMapper;
import org.elasticsearch.xpack.vectors.mapper.DenseVectorFieldMapper.DenseVectorFieldType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class KnnVectorQueryBuilder extends AbstractQueryBuilder<KnnVectorQueryBuilder> {
    public static final String NAME = "knn";

    private final String fieldName;
    private final float[] queryVector;
    private final int numCands;

    public KnnVectorQueryBuilder(String fieldName, float[] queryVector, int numCands) {
        this.fieldName = fieldName;
        this.queryVector = queryVector;
        this.numCands = numCands;
    }

    public KnnVectorQueryBuilder(StreamInput in) throws IOException {
        super(in);
        this.fieldName = in.readString();
        this.numCands = in.readVInt();
        this.queryVector = in.readFloatArray();
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(fieldName);
        out.writeVInt(numCands);
        out.writeFloatArray(queryVector);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME)
            .field("field", fieldName)
            .field("vector", queryVector)
            .field("num_candidates", numCands);
        builder.endObject();
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    protected Query doToQuery(SearchExecutionContext context) {
        MappedFieldType fieldType = context.getFieldType(fieldName);
        if (fieldType == null) {
            throw new IllegalArgumentException("field [" + fieldName + "] does not exist in the mapping");
        }

        if (fieldType instanceof DenseVectorFieldType == false) {
            throw new IllegalArgumentException("[" + NAME + "] queries are only supported on ["
                + DenseVectorFieldMapper.CONTENT_TYPE + "] fields");
        }

        DenseVectorFieldType vectorFieldType = (DenseVectorFieldType) fieldType;
        if (queryVector.length != vectorFieldType.dims()) {
            throw new IllegalArgumentException("the query vector has a different dimension [" + queryVector.length + "] "
                + "than the index vectors [" + vectorFieldType.dims() + "]");
        }
        if (vectorFieldType.isSearchable() == false) {
            throw new IllegalArgumentException("[" + "[" + NAME + "] queries are not supported if [index] is disabled");
        }
        return new KnnVectorQuery(fieldType.name(), queryVector, numCands);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(fieldName, Arrays.hashCode(queryVector), numCands);
    }

    @Override
    protected boolean doEquals(KnnVectorQueryBuilder other) {
        return Objects.equals(fieldName, other.fieldName) &&
            Arrays.equals(queryVector, other.queryVector) &&
            numCands == other.numCands;
    }
}
