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

package com.couchbase.client.core.node;

import com.couchbase.client.core.CoreContext;

import java.util.Map;
import java.util.Optional;

import static com.couchbase.client.core.logging.RedactableArgument.redactSystem;

public class NodeContext extends CoreContext {

  /**
   * The hostname of this node.
   */
  private final NodeIdentifier nodeIdentifier;
  private final Optional<String> alternateAddress;

  public NodeContext(CoreContext ctx, NodeIdentifier nodeIdentifier, Optional<String> alternateAddress) {
    super(ctx.core(), ctx.id(), ctx.environment());
    this.nodeIdentifier = nodeIdentifier;
    this.alternateAddress = alternateAddress;
  }

  public String remoteHostname() {
    return nodeIdentifier.address();
  }

  public Optional<String> alternateAddress() {
    return alternateAddress;
  }

  @Override
  protected void injectExportableParams(final Map<String, Object> input) {
    super.injectExportableParams(input);
    input.put("remote", redactSystem(remoteHostname()));
    input.put("managerPort", redactSystem(nodeIdentifier.managerPort()));
    alternateAddress.ifPresent(a -> input.put("alternateRemote", a));
  }


}
