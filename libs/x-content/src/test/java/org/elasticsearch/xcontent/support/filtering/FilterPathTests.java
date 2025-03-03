/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.xcontent.support.filtering;

import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class FilterPathTests extends ESTestCase {

    public void testSimpleFilterPath() {
        final String input = "test";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("test"));

        FilterPath next = filterPath.getNext();
        assertNotNull(next);
        assertThat(next.matches(), is(true));
        assertThat(next.getSegment(), is(emptyString()));
        assertSame(next, FilterPath.EMPTY);
    }

    public void testFilterPathWithSubField() {
        final String input = "foo.bar";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("foo"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("bar"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }

    public void testFilterPathWithSubFields() {
        final String input = "foo.bar.quz";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("foo"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("bar"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("quz"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }

    public void testEmptyFilterPath() {
        FilterPath[] filterPaths = FilterPath.compile(singleton(""));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(0));
    }

    public void testNullFilterPath() {
        FilterPath[] filterPaths = FilterPath.compile(singleton(null));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(0));
    }

    public void testFilterPathWithEscapedDots() {
        String input = "w.0.0.t";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("w"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("0"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("0"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("t"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);

        input = "w\\.0\\.0\\.t";

        filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("w.0.0.t"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);


        input = "w\\.0.0\\.t";

        filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        filterPath = filterPaths[0];
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("w.0"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("0.t"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }

    public void testSimpleWildcardFilterPath() {
        FilterPath[] filterPaths = FilterPath.compile(singleton("*"));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.isSimpleWildcard(), is(true));
        assertThat(filterPath.getSegment(), equalTo("*"));

        FilterPath next = filterPath.matchProperty(randomAlphaOfLength(2));
        assertNotNull(next);
        assertSame(next, FilterPath.EMPTY);
    }

    public void testWildcardInNameFilterPath() {
        String input = "f*o.bar";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("f*o"));
        assertThat(filterPath.matchProperty("foo"), notNullValue());
        assertThat(filterPath.matchProperty("flo"), notNullValue());
        assertThat(filterPath.matchProperty("foooo"), notNullValue());
        assertThat(filterPath.matchProperty("boo"), nullValue());

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("bar"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }

    public void testDoubleWildcardFilterPath() {
        FilterPath[] filterPaths = FilterPath.compile(singleton("**"));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.isDoubleWildcard(), is(true));
        assertThat(filterPath.getSegment(), equalTo("**"));

        FilterPath next = filterPath.matchProperty(randomAlphaOfLength(2));
        assertNotNull(next);
        assertSame(next, FilterPath.EMPTY);
    }

    public void testStartsWithDoubleWildcardFilterPath() {
        String input = "**.bar";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("**"));

        FilterPath next = filterPath.matchProperty(randomAlphaOfLength(2));
        assertNotNull(next);
        assertThat(next.matches(), is(false));
        assertThat(next.getSegment(), equalTo("bar"));

        next = next.getNext();
        assertNotNull(next);
        assertThat(next.matches(), is(true));
        assertThat(next.getSegment(), is(emptyString()));
        assertSame(next, FilterPath.EMPTY);
    }

    public void testContainsDoubleWildcardFilterPath() {
        String input = "foo.**.bar";

        FilterPath[] filterPaths = FilterPath.compile(singleton(input));
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(1));

        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("foo"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.isDoubleWildcard(), equalTo(true));
        assertThat(filterPath.getSegment(), equalTo("**"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("bar"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }

    public void testMultipleFilterPaths() {
        Set<String> inputs = new LinkedHashSet<>(Arrays.asList("foo.**.bar.*", "test.dot\\.ted"));

        FilterPath[] filterPaths = FilterPath.compile(inputs);
        assertNotNull(filterPaths);
        assertThat(filterPaths, arrayWithSize(2));

        // foo.**.bar.*
        FilterPath filterPath = filterPaths[0];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("foo"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.isDoubleWildcard(), equalTo(true));
        assertThat(filterPath.getSegment(), equalTo("**"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("bar"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.isSimpleWildcard(), equalTo(true));
        assertThat(filterPath.getSegment(), equalTo("*"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);

        // test.dot\.ted
        filterPath = filterPaths[1];
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("test"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(false));
        assertThat(filterPath.getSegment(), equalTo("dot.ted"));

        filterPath = filterPath.getNext();
        assertNotNull(filterPath);
        assertThat(filterPath.matches(), is(true));
        assertThat(filterPath.getSegment(), is(emptyString()));
        assertSame(filterPath, FilterPath.EMPTY);
    }
}
