/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.core.retry;

import com.couchbase.client.core.msg.Request;
import com.couchbase.client.core.msg.Response;

import java.time.Duration;
import java.util.Optional;

public interface RetryStrategy {

  /**
   * Checks if the given request should be retried and how long the
   * retry delay should be.
   *
   * @param request the request to be checked.
   * @return If empty, no retry should be done. If a duration is returned it determines
   *         the retry delay.
   */
  Optional<Duration> shouldRetry(Request<? extends Response> request);

}
