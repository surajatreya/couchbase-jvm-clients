/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.search.queries;

import com.couchbase.client.java.json.JsonObject;

/**
 * A FTS query that queries fields explicitly indexed as boolean.
 *
 * @author Simon Baslé
 * @author Michael Nitschinger
 * @since 2.3.0
 */
public class BooleanFieldQuery extends AbstractFtsQuery {

    private final boolean value;
    private String field;

    public BooleanFieldQuery(boolean value) {
        super();
        this.value = value;
    }

    public BooleanFieldQuery field(String field) {
        this.field = field;
        return this;
    }

    @Override
    public BooleanFieldQuery boost(double boost) {
        super.boost(boost);
        return this;
    }

    @Override
    protected void injectParams(JsonObject input) {
        if (field != null) {
            input.put("field", field);
        }

        input.put("bool", value);
    }
}
