/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.client.transform.transforms.pivot;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.elasticsearch.test.AbstractXContentTestCase;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.instanceOf;

public class GroupConfigTests extends AbstractXContentTestCase<GroupConfig> {

    public static GroupConfig randomGroupConfig() {
        Map<String, SingleGroupSource> groups = new LinkedHashMap<>();

        // ensure that the unlikely does not happen: 2 group_by's share the same name
        Set<String> names = new HashSet<>();
        for (int i = 0; i < randomIntBetween(1, 4); ++i) {
            String targetFieldName = randomAlphaOfLengthBetween(1, 20);
            if (names.add(targetFieldName)) {
                SingleGroupSource groupBy = null;
                SingleGroupSource.Type type = randomFrom(SingleGroupSource.Type.values());
                switch (type) {
                    case TERMS:
                        groupBy = TermsGroupSourceTests.randomTermsGroupSource();
                        break;
                    case HISTOGRAM:
                        groupBy = HistogramGroupSourceTests.randomHistogramGroupSource();
                        break;
                    case DATE_HISTOGRAM:
                        groupBy = DateHistogramGroupSourceTests.randomDateHistogramGroupSource();
                        break;
                    case GEOTILE_GRID:
                        groupBy = GeoTileGroupSourceTests.randomGeoTileGroupSource();
                        break;
                    default:
                        fail("unknown group source type, please implement tests and add support here");
                }
                groups.put(targetFieldName, groupBy);
            }
        }

        return new GroupConfig(groups);
    }

    @Override
    protected GroupConfig createTestInstance() {
        return randomGroupConfig();
    }

    @Override
    protected GroupConfig doParseInstance(XContentParser parser) throws IOException {
        return GroupConfig.fromXContent(parser);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        return field -> field.isEmpty() == false;
    }

    public void testLenientParsing() throws IOException {
        BytesArray json = new BytesArray(
            "{"
                + "  \"unknown-field\": \"foo\","
                + "  \"destination-field\": {"
                + "    \"terms\": {"
                + "      \"field\": \"term-field\""
                + "    }"
                + "  },"
                + "  \"unknown-field-2\": \"bar\","
                + "  \"destination-field2\": {"
                + "    \"terms\": {"
                + "      \"field\": \"term-field2\""
                + "    }"
                + "  },"
                + "  \"array-field\": ["
                + "    1,"
                + "    2"
                + "  ]"
                + "}"
        );
        XContentParser parser = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            json.streamInput()
        );

        GroupConfig gc = GroupConfig.fromXContent(parser);

        assertEquals(gc.getGroups().size(), 2);
        assertTrue(gc.getGroups().containsKey("destination-field"));
        SingleGroupSource groupSource = gc.getGroups().get("destination-field");
        assertThat(groupSource, instanceOf(TermsGroupSource.class));
        assertEquals(groupSource.getField(), "term-field");
    }

    public void testLenientParsingUnknowGroupType() throws IOException {
        BytesArray json = new BytesArray(
            "{"
                + "  \"destination-field1\": {"
                + "    \"newgroup\": {"
                + "      \"field1\": \"bar\","
                + "      \"field2\": \"foo\""
                + "    }"
                + "  },"
                + "  \"unknown-field\": \"bar\","
                + "  \"destination-field2\": {"
                + "    \"terms\": {"
                + "      \"field\": \"term-field\""
                + "    }"
                + "  }"
                + "}"
        );
        XContentParser parser = JsonXContent.jsonXContent.createParser(
            NamedXContentRegistry.EMPTY,
            DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
            json.streamInput()
        );

        GroupConfig gc = GroupConfig.fromXContent(parser);

        assertEquals(gc.getGroups().size(), 1);
        assertTrue(gc.getGroups().containsKey("destination-field2"));
        SingleGroupSource groupSource = gc.getGroups().get("destination-field2");
        assertThat(groupSource, instanceOf(TermsGroupSource.class));
        assertEquals(groupSource.getField(), "term-field");
    }
}
