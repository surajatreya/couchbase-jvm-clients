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

package com.couchbase.client.java;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.env.Credentials;
import com.couchbase.client.core.env.OwnedSupplier;
import com.couchbase.client.core.env.UsernameAndPassword;
import com.couchbase.client.core.msg.analytics.AnalyticsRequest;
import com.couchbase.client.core.msg.query.QueryRequest;
import com.couchbase.client.core.msg.search.SearchRequest;
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.java.analytics.AnalyticsAccessor;
import com.couchbase.client.java.analytics.AnalyticsOptions;
import com.couchbase.client.java.analytics.AnalyticsResult;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.search.AsyncSearchIndexManager;
import com.couchbase.client.java.manager.bucket.AsyncBucketManager;
import com.couchbase.client.java.manager.user.AsyncUserManager;
import com.couchbase.client.java.query.QueryAccessor;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.search.SearchAccessor;
import com.couchbase.client.java.search.SearchOptions;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchResult;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.couchbase.client.core.util.Golang.encodeDurationToMs;
import static com.couchbase.client.core.util.Validators.notNull;
import static com.couchbase.client.core.util.Validators.notNullOrEmpty;
import static com.couchbase.client.java.ReactiveCluster.DEFAULT_ANALYTICS_OPTIONS;
import static com.couchbase.client.java.ReactiveCluster.DEFAULT_QUERY_OPTIONS;
import static com.couchbase.client.java.ReactiveCluster.DEFAULT_SEARCH_OPTIONS;

/**
 * The {@link AsyncCluster} is the main entry point when connecting to a Couchbase cluster.
 *
 * <p>Note that most of the time you want to use the blocking {@link Cluster} or the powerful
 * reactive {@link ReactiveCluster} API instead. Use this API if you know what you are doing and
 * you want to build low-level, even faster APIs on top.</p>
 */
public class AsyncCluster {

  /**
   * Holds the supplied environment that gets used throughout the lifetime.
   */
  private final Supplier<ClusterEnvironment> environment;

  /**
   * Holds the internal core reference.
   */
  private final Core core;

  private final AsyncSearchIndexManager searchIndexManager;

  private final QueryAccessor queryAccessor;

  private final AsyncUserManager userManager;

  private final AsyncBucketManager bucketManager;

  /**
   * Connect to a Couchbase cluster with a username and a password as credentials.
   *
   * @param connectionString connection string used to locate the Couchbase cluster.
   * @param username the name of the user with appropriate permissions on the cluster.
   * @param password the password of the user with appropriate permissions on the cluster.
   * @return if properly connected, returns a {@link AsyncCluster}.
   */
  public static CompletableFuture<AsyncCluster> connect(final String connectionString, final String username,
                                                        final String password) {
    return connect(connectionString, new UsernameAndPassword(username, password));
  }

  /**
   * Connect to a Couchbase cluster with custom {@link Credentials}.
   *
   * @param connectionString connection string used to locate the Couchbase cluster.
   * @param credentials custom credentials used when connecting to the cluster.
   * @return if properly connected, returns a {@link AsyncCluster}.
   */
  public static CompletableFuture<AsyncCluster> connect(final String connectionString, final Credentials credentials) {
    return Mono.defer(() -> {
      AsyncCluster cluster = new AsyncCluster(new OwnedSupplier<>(
        ClusterEnvironment.create(connectionString, credentials)
      ));
      return cluster.performGlobalConnect().then(Mono.just(cluster));
    }).toFuture();
  }

  /**
   * Connect to a Couchbase cluster with a custom {@link ClusterEnvironment}.
   *
   * @param environment the custom environment with its properties used to connect to the cluster.
   * @return if properly connected, returns a {@link AsyncCluster}.
   */
  public static CompletableFuture<AsyncCluster> connect(final ClusterEnvironment environment) {
    return Mono.defer(() -> {
      AsyncCluster cluster = new AsyncCluster(() -> environment);
      return cluster.performGlobalConnect().then(Mono.just(cluster));
    }).toFuture();
  }

  /**
   * Tries to set up the global connect ("gcccp") if possible.
   *
   * @return once this setup is completed, will return.
   */
  Mono<Void> performGlobalConnect() {
    return core.initGlobalConfig().timeout(environment.get().timeoutConfig().connectTimeout());
  }

  /**
   * Creates a new cluster from a {@link ClusterEnvironment}.
   *
   * @param environment the environment to use for this cluster.
   */
  AsyncCluster(final Supplier<ClusterEnvironment> environment) {
    this.environment = environment;
    this.core = Core.create(environment.get());
    this.searchIndexManager = new AsyncSearchIndexManager(core);
    this.queryAccessor = new QueryAccessor(core);
    this.userManager = new AsyncUserManager(core);
    this.bucketManager = new AsyncBucketManager(core);
  }

  /**
   * Provides access to the configured {@link ClusterEnvironment} for this cluster.
   */
  public ClusterEnvironment environment() {
    return environment.get();
  }

  /**
   * Provides access to the underlying {@link Core}.
   *
   * <p>This is advanced API, use with care!</p>
   */
  @Stability.Volatile
  public Core core() {
    return core;
  }

  /**
   * Provides access to the index management capabilities.
   */
  @Stability.Volatile
  public AsyncSearchIndexManager searchIndexes() {
    return searchIndexManager;
  }

  /**
   * Provides access to the user management capabilities.
   */
  @Stability.Volatile
  public AsyncUserManager users() {
    return userManager;
  }

  @Stability.Volatile
  public AsyncBucketManager buckets() {
    return bucketManager;
  }

  /**
   * Performs a N1QL query with default {@link QueryOptions}.
   *
   * @param statement the N1QL query statement as a raw string.
   * @return the {@link QueryResult} once the response arrives successfully.
   */
  public CompletableFuture<QueryResult> query(final String statement) {
    return query(statement, DEFAULT_QUERY_OPTIONS);
  }

  /**
   * Performs a N1QL query with custom {@link QueryOptions}.
   *
   * @param statement the N1QL query statement as a raw string.
   * @param options the custom options for this query.
   * @return the {@link QueryResult} once the response arrives successfully.
   */
  public CompletableFuture<QueryResult> query(final String statement, final QueryOptions options) {
    final QueryOptions.Built opts = options.build();
    return queryAccessor.queryAsync(queryRequest(statement, opts), opts);
  }

  /**
   * Helper method to construct the query request.
   *
   * @param statement the statement of the query.
   * @param options the options.
   * @return the constructed query request.
   */
  QueryRequest queryRequest(final String statement, final QueryOptions.Built options) {
    notNullOrEmpty(statement, "Statement");
    notNull(options, "QueryOptions");

    Duration timeout = options.timeout().orElse(environment.get().timeoutConfig().queryTimeout());
    RetryStrategy retryStrategy = options.retryStrategy().orElse(environment.get().retryStrategy());

    JsonObject query = JsonObject.create();
    query.put("statement", statement);
    query.put("timeout", encodeDurationToMs(timeout));
    options.injectParams(query);

    QueryRequest request = new QueryRequest(timeout, core.context(), retryStrategy, environment.get().credentials(),
      statement, query.toString().getBytes(StandardCharsets.UTF_8));
    request.context().clientContext(options.clientContext());
    return request;
  }

  /**
   * Performs an Analytics query with default {@link AnalyticsOptions}.
   *
   * @param statement the Analytics query statement as a raw string.
   * @return the {@link AnalyticsResult} once the response arrives successfully.
   */
  public CompletableFuture<AnalyticsResult> analyticsQuery(final String statement) {
    return analyticsQuery(statement, DEFAULT_ANALYTICS_OPTIONS);
  }


  /**
   * Performs an Analytics query with custom {@link AnalyticsOptions}.
   *
   * @param statement the Analytics query statement as a raw string.
   * @param options the custom options for this analytics query.
   * @return the {@link AnalyticsResult} once the response arrives successfully.
   */
  public CompletableFuture<AnalyticsResult> analyticsQuery(final String statement,
                                                           final AnalyticsOptions options) {
    return AnalyticsAccessor.analyticsQueryAsync(core, analyticsRequest(statement, options));
  }

  /**
   * Helper method to craft an analytics request.
   *
   * @param statement the statement to use.
   * @param options the analytics options.
   * @return the created analytics request.
   */
  AnalyticsRequest analyticsRequest(final String statement, final AnalyticsOptions options) {
    notNullOrEmpty(statement, "Statement");
    notNull(options, "AnalyticsOptions");

    AnalyticsOptions.Built opts = options.build();

    Duration timeout = opts.timeout().orElse(environment.get().timeoutConfig().analyticsTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.get().retryStrategy());

    JsonObject query = JsonObject.empty();
    query.put("statement", statement);
    query.put("timeout", encodeDurationToMs(timeout));
    opts.injectParams(query);

    AnalyticsRequest request = new AnalyticsRequest(timeout, core.context(), retryStrategy, environment.get().credentials(),
        query.toString().getBytes(StandardCharsets.UTF_8), opts.priority()
    );
    request.context().clientContext(opts.clientContext());
    return request;
  }

  /**
   * Performs a Full Text Search (FTS) query with default {@link SearchOptions}.
   *
   * @param query the query, in the form of a {@link SearchQuery}
   * @return the {@link SearchRequest} once the response arrives successfully, inside a {@link CompletableFuture}
   */
  public CompletableFuture<SearchResult> searchQuery(final SearchQuery query) {
    return searchQuery(query, DEFAULT_SEARCH_OPTIONS);
  }

  /**
   * Performs a Full Text Search (FTS) query with custom {@link SearchOptions}.
   *
   * @param query the query, in the form of a {@link SearchQuery}
   * @param options the custom options for this query.
   * @return the {@link SearchRequest} once the response arrives successfully, inside a {@link CompletableFuture}
   */
  public CompletableFuture<SearchResult> searchQuery(final SearchQuery query, final SearchOptions options) {
    return SearchAccessor.searchQueryAsync(core, searchRequest(query, options));
  }

  SearchRequest searchRequest(final SearchQuery query, final SearchOptions options) {
    notNull(query, "SearchQuery");
    notNull(options, "SearchOptions");

    SearchOptions.Built opts = options.build();
    JsonObject params = query.export();
    byte[] bytes = params.toString().getBytes(StandardCharsets.UTF_8);

    Duration timeout = opts.timeout().orElse(environment.get().timeoutConfig().searchTimeout());
    RetryStrategy retryStrategy = opts.retryStrategy().orElse(environment.get().retryStrategy());
    SearchRequest request = new SearchRequest(timeout, core.context(), retryStrategy, environment.get().credentials(),
      query.indexName(), bytes);
    request.context().clientContext(opts.clientContext());
    return request;
  }

  /**
   * Opens a {@link AsyncBucket} with the given name.
   *
   * @param name the name of the bucket to open.
   * @return a {@link AsyncBucket} once opened.
   */
  public CompletableFuture<AsyncBucket> bucket(final String name) {
    notNullOrEmpty(name, "Name");
    return core
      .openBucket(name)
      .thenReturn(new AsyncBucket(name, core, environment.get()))
      .toFuture();
  }

  /**
   * Performs a non-reversible shutdown of this {@link AsyncCluster}.
   */
  public CompletableFuture<Void> shutdown() {
    return shutdown(environment.get().timeoutConfig().disconnectTimeout());
  }

  /**
   * Performs a non-reversible shutdown of this {@link AsyncCluster}.
   *
   * @param timeout overriding the default disconnect timeout if needed.
   */
  public CompletableFuture<Void> shutdown(final Duration timeout) {
    return shutdownInternal(timeout).toFuture();
  }

  /**
   * Can be called from other cluster instances so that code is not duplicated.
   *
   * @param timeout the timeout for the environment to shut down if owned.
   * @return a mono once complete.
   */
  Mono<Void> shutdownInternal(final Duration timeout) {
    return core.shutdown().flatMap(ignore -> {
      if (environment instanceof OwnedSupplier) {
        return environment.get().shutdownReactive(timeout);
      } else {
        return Mono.empty();
      }
    });
  }

  /**
   * Provides access to the internal query accessor.
   */
  @Stability.Internal
  QueryAccessor queryAccessor() {
    return queryAccessor;
  }
}
