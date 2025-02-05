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

package com.couchbase.client.java.kv;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.error.*;
import com.couchbase.client.core.msg.kv.PrependRequest;

import java.util.concurrent.CompletableFuture;

import static com.couchbase.client.java.kv.DurabilityUtils.wrapWithDurability;

public class PrependAccessor {

  public static CompletableFuture<MutationResult> prepend(final Core core,
                                                          final PrependRequest request,
                                                          final String key,
                                                          final PersistTo persistTo,
                                                          final ReplicateTo replicateTo) {
    core.send(request);
    final CompletableFuture<MutationResult> mutationResult = request
      .response()
      .thenApply(response -> {
        switch (response.status()) {
          case SUCCESS:
            return new MutationResult(response.cas(), response.mutationToken());
          case EXISTS:
            throw CASMismatchException.forKey(key);
          case NOT_STORED:
            throw KeyNotFoundException.forKey(key);
          default:
            throw DefaultErrorUtil.defaultErrorForStatus(key, response.status());
        }
      });
    return wrapWithDurability(mutationResult, key, persistTo, replicateTo, core, request, false);

  }
}
