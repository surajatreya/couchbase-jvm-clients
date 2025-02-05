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

package com.couchbase.client.scala.search.queries

import com.couchbase.client.scala.json.{JsonArray, JsonObject}

/** A compound FTS query that performs a logical AND between all its sub-queries (conjunction).
  *
  * @since 1.0.0
  */
case class ConjunctionQuery(private[scala] val queries: Seq[AbstractFtsQuery] = Seq.empty,
                            private[scala] val field: Option[String] = None,
                            private[scala] val boost: Option[Double] = None) extends AbstractCompoundQuery {

  /** The boost parameter is used to increase the relative weight of a clause (with a boost greater than 1) or decrease
    * the relative weight (with a boost between 0 and 1)
    *
    * @param boost the boost parameter, which must be >= 0
    *
    * @return a copy of this, for chaining
    */
  def boost(boost: Double): ConjunctionQuery = {
    copy(boost = Some(boost))
  }

  /** Adds more sub-queries to the conjunction.
    *
    * @return a copy of this, for chaining
    */
  def and(queries: AbstractFtsQuery*): ConjunctionQuery = {
    copy(this.queries ++ queries.toSeq)
  }

  override protected def injectParams(input: JsonObject): Unit = {
    val conjuncts = JsonArray.create
    for ( childQuery <- queries ) {
      val childJson = JsonObject.create
      childQuery.injectParamsAndBoost(childJson)
      conjuncts.add(childJson)
    }
    input.put("conjuncts", conjuncts)
    boost.foreach(v => input.put("boost", v))
  }
}
