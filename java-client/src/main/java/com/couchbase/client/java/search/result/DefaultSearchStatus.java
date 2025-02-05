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
package com.couchbase.client.java.search.result;

import com.couchbase.client.core.error.DecodingFailedException;
import com.couchbase.client.java.json.JacksonTransformers;
import com.couchbase.client.java.json.JsonObject;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The default implementation for a {@link SearchStatus}
 *
 * @since 2.3.0
 */
public class DefaultSearchStatus implements SearchStatus {

    private final long totalCount;
    private final long errorCount;
    private final long successCount;

    public DefaultSearchStatus(long totalCount, long errorCount, long successCount) {
        this.totalCount = totalCount;
        this.errorCount = errorCount;
        this.successCount = successCount;
    }

    @Override
    public long totalCount() {
        return this.totalCount;
    }

    @Override
    public long successCount() {
        return this.successCount;
    }

    @Override
    public long errorCount() {
        return this.errorCount;
    }

    @Override
    public boolean isSuccess() {
        return errorCount() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSearchStatus that = (DefaultSearchStatus) o;

        if (totalCount != that.totalCount) {
            return false;
        }
        if (errorCount != that.errorCount) {
            return false;
        }
        return successCount == that.successCount;

    }

    @Override
    public int hashCode() {
        int result = (int) (totalCount ^ (totalCount >>> 32));
        result = 31 * result + (int) (errorCount ^ (errorCount >>> 32));
        result = 31 * result + (int) (successCount ^ (successCount >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "DefaultSearchStatus{" +
                "totalCount=" + totalCount +
                ", errorCount=" + errorCount +
                ", successCount=" + successCount +
                '}';
    }

    public static DefaultSearchStatus fromBytes(byte[] bytes) {
        try {
            JsonObject value = JacksonTransformers.MAPPER.readValue(bytes, JsonObject.class);
            return new DefaultSearchStatus(
                    value.getLong("total"),
                    value.getLong("failed"),
                    value.getLong("successful")
            );
        } catch (IOException e) {
            throw new DecodingFailedException("Failed to decode status '" + new String(bytes, UTF_8) + "'", e);
        }
    }
}
