/*
 * Copyright 2019 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.core.env;

import java.util.function.BiConsumer;

public class SystemPropertyPropertyLoader implements PropertyLoader<CoreEnvironment.Builder> {
  private static final String PREFIX = "com.couchbase.env.";

  private final BuilderPropertySetter setter = new BuilderPropertySetter();

  @Override
  public void load(CoreEnvironment.Builder builder) {
    forEachStringSystemProperty((name, value) -> {
      if (name.startsWith(PREFIX)) {
        try {
          setter.set(builder, name.substring(PREFIX.length()), value);

        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
            "Failed to apply system property \"" + name + "\". " + e.getMessage(), e);
        }
      }
    });
  }

  private static void forEachStringSystemProperty(BiConsumer<String, String> action) {
    System.getProperties().forEach((key, value) -> {
      if (key instanceof String && value instanceof String) {
        action.accept((String) key, (String) value);
      }
    });
  }
}
