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

package com.couchbase.client.scala.kv.handlers

import com.couchbase.client.core.error.{CASMismatchException, KeyExistsException}
import com.couchbase.client.core.error.subdoc.SubDocumentException
import com.couchbase.client.core.msg.ResponseStatus
import com.couchbase.client.core.msg.kv._
import com.couchbase.client.core.retry.RetryStrategy
import com.couchbase.client.scala.HandlerParams
import com.couchbase.client.scala.durability.Durability
import com.couchbase.client.scala.kv._
import com.couchbase.client.scala.util.Validate

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.util.{Failure, Try}


/**
  * Handles requests and responses for KV SubDocument mutation operations.
  *
  * @author Graham Pople
  * @since 1.0.0
  */
private[scala] class MutateInHandler(hp: HandlerParams) {

  def request[T](id: String,
                 spec: Seq[MutateInSpec],
                 cas: Long,
                 document: DocumentCreation = DocumentCreation.DoNothing,
                 durability: Durability,
                 expiration: java.time.Duration,
                 timeout: java.time.Duration,
                 retryStrategy: RetryStrategy)
  : Try[SubdocMutateRequest] = {
    val validations: Try[SubdocMutateRequest] = for {
      _ <- Validate.notNullOrEmpty(id, "id")
      _ <- Validate.notNull(cas, "cas")
      _ <- Validate.notNull(durability, "durability")
      _ <- Validate.notNull(expiration, "expiration")
      _ <- Validate.notNull(timeout, "timeout")
      _ <- Validate.notNull(retryStrategy, "retryStrategy")
    } yield null

    if (validations.isFailure) {
      validations
    }
    else {
      // Find any decode failure
      val failed: Option[MutateInSpec] = spec
        .filter(_.isInstanceOf[MutateInSpecStandard])
        .find(v => v.asInstanceOf[MutateInSpecStandard].fragment.isFailure)

      failed match {
        case Some(failed: MutateInSpecStandard) =>
          // If any of the decodes failed, abort
          Failure(failed.fragment.failed.get)

        case _ =>

          val commands = new java.util.ArrayList[SubdocMutateRequest.Command]()
          spec.map(_.convert).foreach(commands.add)

          if (commands.isEmpty) {
            Failure(SubdocMutateRequest.errIfNoCommands())
          }
          else if (commands.size > SubdocMutateRequest.SUBDOC_MAX_FIELDS) {
            Failure(SubdocMutateRequest.errIfNoCommands())
          }
          else {
            Try(new SubdocMutateRequest(timeout,
              hp.core.context(),
              hp.collectionIdentifier,
              retryStrategy,
              id,
              document == DocumentCreation.Insert,
              document == DocumentCreation.Upsert,
              commands,
              expiration.getSeconds,
              cas,
              durability.toDurabilityLevel))
          }
      }
    }
  }

  def response(id: String,
               document: DocumentCreation = DocumentCreation.DoNothing,
               response: SubdocMutateResponse): MutateInResult = {
    response.status() match {

      case ResponseStatus.SUCCESS =>
        val values: Seq[SubdocField] = response.values().asScala

        MutateInResult(id, values, response.cas(), response.mutationToken().asScala)

      case ResponseStatus.SUBDOC_FAILURE =>

        response.error().asScala match {
          case Some(err) => throw err
          case _ => throw new SubDocumentException("Unknown SubDocument failure occurred") {}
        }

      case ResponseStatus.EXISTS =>
        document match {
          case DocumentCreation.Insert => throw KeyExistsException.forKey(id)
          case _ => throw CASMismatchException.forKey(id)
        }

      case _ => throw DefaultErrors.throwOnBadResult(id, response.status())
    }
  }
}
