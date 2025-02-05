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

package com.couchbase.client.core.cnc;

import com.couchbase.client.core.cnc.diagnostics.Analyzer;
import com.couchbase.client.core.cnc.diagnostics.GcAnalyzer;
import com.couchbase.client.core.cnc.diagnostics.PauseAnalyzer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@link DiagnosticsMonitor} keeps a close eye on system resources and emits events
 * if they are over a configurable threshold.
 *
 * <p>While not being 100% reliable, the monitor should provide better debugging in the
 * field and in user environments. Together with pieces like tracing the system paints
 * are more accurate picture on what's going on.</p>
 *
 * @since 2.0.0
 */
public class DiagnosticsMonitor {

  /**
   * Creates a new {@link DiagnosticsMonitor} with default settings.
   *
   * @param eventBus the event bus to dispatch the events into.
   * @return a new {@link DiagnosticsMonitor}.
   */
  public static DiagnosticsMonitor create(final EventBus eventBus) {
    return builder(eventBus).build();
  }

  /**
   * Creates a new {@link DiagnosticsMonitor} with custom settings.
   *
   * @param eventBus the event bus to dispatch the events into.
   * @return a builder to configure the monitor.
   */
  public static DiagnosticsMonitor.Builder builder(EventBus eventBus) {
    return new Builder(eventBus);
  }

  /**
   * The parent event bus.
   */
  private final EventBus eventBus;

  private final Duration emitInterval;

  private final Thread diagnosticsThread;
  private final AtomicBoolean diagnosticsRunning = new AtomicBoolean(true);
  private final List<Analyzer> analyzers;

  /**
   * Internal method to create the new monitor from a builder config.
   *
   * @param builder the builder config.
   */
  private DiagnosticsMonitor(final Builder builder) {
    this.eventBus = builder.eventBus;
    this.analyzers = Collections.synchronizedList(new ArrayList<>());
    this.emitInterval = builder.emitInterval;

    diagnosticsThread = new Thread(() -> {
      try {
        while(diagnosticsRunning.get()) {
          Thread.sleep(emitInterval.toMillis());
          for (Analyzer analyzer : analyzers) {
            eventBus.publish(analyzer.fetchEvent(Event.Severity.INFO, context()));
          }
        }
      } catch (InterruptedException e) {
        // bail out on the interrupt.
      }
    }, "cb-diagnostics");
    diagnosticsThread.setDaemon(true);
  }

  /**
   * Starts this {@link DiagnosticsMonitor}.
   */
  public Mono<Void> start() {
    return Mono.defer(() -> {
        diagnosticsThread.start();
        return Mono.empty();
      })
      .then(Mono.defer(() -> {
          GcAnalyzer analyzer = new GcAnalyzer();
          analyzers.add(analyzer);
          return analyzer.start();
      }))
      .then(Mono.defer(() -> {
        PauseAnalyzer analyzer = new PauseAnalyzer();
        analyzers.add(analyzer);
        return analyzer.start();
      }));
  }

  /**
   * Stops the {@link DiagnosticsMonitor}.
   */
  public Mono<Void> stop() {
    return Flux
      .fromIterable(analyzers)
      .flatMap(Analyzer::stop)
      .then(Mono.defer(() -> {
        diagnosticsRunning.set(false);
        diagnosticsThread.interrupt();
        return Mono.empty();
      }));
  }


  public Context context() {
    return null;
  }

  /**
   * Allows to configure the diagnostics monitor.
   */
  public static class Builder {

    private final EventBus eventBus;

    /**
     * By default, emit every 30 minutes.
     */
    private Duration emitInterval = Duration.ofMinutes(30);

    public Builder(final EventBus eventBus) {
      this.eventBus = eventBus;
    }

    public Builder emitInterval(Duration emitInterval) {
      this.emitInterval = emitInterval;
      return this;
    }

    public DiagnosticsMonitor build() {
      return new DiagnosticsMonitor(this);
    }

  }

}
