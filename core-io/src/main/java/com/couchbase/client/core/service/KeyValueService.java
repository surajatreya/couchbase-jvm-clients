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

package com.couchbase.client.core.service;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.endpoint.Endpoint;
import com.couchbase.client.core.endpoint.KeyValueEndpoint;
import com.couchbase.client.core.env.Credentials;
import com.couchbase.client.core.service.strategy.PartitionSelectionStrategy;

import java.util.Optional;

/**
 *
 */
public class KeyValueService extends PooledService {

  private static final EndpointSelectionStrategy STRATEGY = new PartitionSelectionStrategy();

  private final String hostname;
  private final int port;
  private final Optional<String> bucketname;
  private final Credentials credentials;

  public KeyValueService(final ServiceConfig serviceConfig, final CoreContext coreContext,
                         final String hostname, final int port, final Optional<String> bucketname,
                         final Credentials credentials) {
    super(serviceConfig, new ServiceContext(coreContext, hostname, port, ServiceType.KV, bucketname));
    this.hostname = hostname;
    this.port = port;
    this.bucketname = bucketname;
    this.credentials = credentials;
  }

  @Override
  protected Endpoint createEndpoint() {
    return new KeyValueEndpoint(serviceContext(), hostname, port, bucketname, credentials);
  }

  @Override
  protected EndpointSelectionStrategy selectionStrategy() {
    return STRATEGY;
  }

  @Override
  public ServiceType type() {
    return ServiceType.KV;
  }
}
