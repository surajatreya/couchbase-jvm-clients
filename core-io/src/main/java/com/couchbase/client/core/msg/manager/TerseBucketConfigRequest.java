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

package com.couchbase.client.core.msg.manager;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.env.Credentials;
import com.couchbase.client.core.io.NetworkAddress;
import com.couchbase.client.core.msg.ResponseStatus;
import com.couchbase.client.core.msg.TargetedRequest;
import com.couchbase.client.core.retry.RetryStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.time.Duration;

import static com.couchbase.client.core.io.netty.HttpProtocol.addHttpBasicAuth;

public class TerseBucketConfigRequest extends BaseManagerRequest<TerseBucketConfigResponse> implements TargetedRequest {

  private static final String TERSE_URI = "/pools/default/b/%s";

  private final String bucketName;
  private final Credentials credentials;
  private final NetworkAddress target;

  public TerseBucketConfigRequest(Duration timeout, CoreContext ctx, RetryStrategy retryStrategy,
                                  String bucketName, Credentials credentials, final NetworkAddress target) {
    super(timeout, ctx, retryStrategy);
    this.bucketName = bucketName;
    this.credentials = credentials;
    this.target = target;
  }

  @Override
  public FullHttpRequest encode() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, String.format(TERSE_URI, bucketName));
    addHttpBasicAuth(request, credentials.usernameForBucket(bucketName), credentials.passwordForBucket(bucketName));
    return request;
  }

  @Override
  public NetworkAddress target() {
    return target;
  }

  @Override
  public TerseBucketConfigResponse decode(byte[] content) {
    return new TerseBucketConfigResponse(ResponseStatus.SUCCESS, content);
  }

}
