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
package com.couchbase.client.core.error;

/**
 * Thrown when Couchbase Server returns with an Out of Memory error.
 *
 * @author Michael Nitschinger
 * @since 2.1.0
 */
public class CouchbaseOutOfMemoryException extends CouchbaseException {

    public CouchbaseOutOfMemoryException() {
        super();
    }

    public CouchbaseOutOfMemoryException(String message) {
        super(message);
    }

    public CouchbaseOutOfMemoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public CouchbaseOutOfMemoryException(Throwable cause) {
        super(cause);
    }
}
