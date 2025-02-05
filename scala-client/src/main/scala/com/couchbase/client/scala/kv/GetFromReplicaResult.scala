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
package com.couchbase.client.scala.kv

import com.couchbase.client.scala.json.JsonObject

import scala.concurrent.duration.Duration

class GetFromReplicaResult(id: String,
                           // It's Right only in the case where projections were requested
                           private val _content: Either[Array[Byte], JsonObject],
                           flags: Int,
                           cas: Long,
                           expiration: Option[Duration],

                           /** Whether this came from the active vbucket, or a replica. */
                           val isMaster: Boolean) extends GetResult(id, _content, flags, cas, expiration)
