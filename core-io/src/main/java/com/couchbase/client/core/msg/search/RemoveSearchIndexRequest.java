/*
 * Copyright (c) 2019 Couchbase, Inc.
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

package com.couchbase.client.core.msg.search;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.DefaultFullHttpRequest;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.FullHttpRequest;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.FullHttpResponse;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpMethod;
import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpVersion;
import com.couchbase.client.core.env.Credentials;
import com.couchbase.client.core.io.netty.HttpProtocol;
import com.couchbase.client.core.msg.BaseRequest;
import com.couchbase.client.core.msg.NonChunkedHttpRequest;
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.core.service.ServiceType;

import java.time.Duration;

import static com.couchbase.client.core.io.netty.HttpProtocol.addHttpBasicAuth;

public class RemoveSearchIndexRequest extends BaseRequest<RemoveSearchIndexResponse>
  implements NonChunkedHttpRequest<RemoveSearchIndexResponse> {

  private static final String PATH = "/api/index/";

  private final String name;
  private final Credentials credentials;

  public RemoveSearchIndexRequest(Duration timeout, CoreContext ctx, RetryStrategy retryStrategy,
                                  Credentials credentials, String name) {
    super(timeout, ctx, retryStrategy);
    this.name = name;
    this.credentials = credentials;
  }

  @Override
  public FullHttpRequest encode() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, PATH + name);
    addHttpBasicAuth(request, credentials);
    return request;
  }

  @Override
  public RemoveSearchIndexResponse decode(final FullHttpResponse response) {
    byte[] dst = new byte[response.content().readableBytes()];
    response.content().readBytes(dst);
    return new RemoveSearchIndexResponse(HttpProtocol.decodeStatus(response.status()), dst);
  }

  @Override
  public ServiceType serviceType() {
    return ServiceType.SEARCH;
  }
}
