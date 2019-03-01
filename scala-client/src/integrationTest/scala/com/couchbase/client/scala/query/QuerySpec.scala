package com.couchbase.client.scala.query

import com.couchbase.client.core.error.DecodingFailedException
import com.couchbase.client.scala.json.JsonObject
import com.couchbase.client.scala.{Cluster, TestUtils}
import org.scalatest.FunSuite

import scala.util.{Failure, Success}

class QuerySpec extends FunSuite {

    val cluster = Cluster.connect("localhost", "Administrator", "password")
    val bucket = cluster.bucket("default")
    val coll = bucket.defaultCollection


  def getContent(docId: String): ujson.Obj = {
    coll.get(docId) match {
      case Success(result) =>
        result.contentAs[ujson.Obj] match {
          case Success(content) =>
            content
          case Failure(err) =>
            assert(false, s"unexpected error $err")
            null
        }
      case Failure(err) =>
        assert(false, s"unexpected error $err")
        null
    }
  }

  private def prepare(content: ujson.Value) = {
    val docId = TestUtils.docId()
    val insertResult = coll.insert(docId, content).get
    (docId, insertResult.mutationToken)
  }

  test("hello world") {
    cluster.query("""select 'hello world' as Greeting""") match {
      case Success(result) =>
        assert(result.rows.size == 1)
        assert(result.rows.head.contentAs[String].get == """{"Greeting":"hello world"}""")
      case Failure(err) => throw err
    }
  }

  test("hello world content as JsonObject") {
    cluster.query("""select 'hello world' as Greeting""") match {
      case Success(result) =>
        assert(result.rows.size == 1)
        assert(result.rows.head.contentAs[JsonObject].get.str("Greeting") == "hello world")
      case Failure(err) => throw err
    }
  }

  test("hello world with quotes") {
    cluster.query("""select "hello world" as Greeting""") match {
      case Success(result) =>
        assert(result.rows.size == 1)
        assert(result.rows.head.contentAs[String].get == """{"Greeting":"hello world"}""")
      case Failure(err) => throw err
    }
  }


  test("read 2 docs use keys") {
    val (docId1, _) = prepare(ujson.Obj("name" -> "Andy"))
    val (docId2, _) = prepare(ujson.Obj("name" -> "Beth"))

    val statement =s"""select name from default use keys ['$docId1', '$docId2'];"""
//    val statement = s"""SELECT * FROM default USE KEYS '$docId1';"""
    cluster.query(statement) match {
      case Success(result) =>
        val rows = result.rows.toSeq
        assert(rows.size == 2)
        assert(rows.head.contentAs[ujson.Obj].get("name").str == """Andy""")
        assert(rows.last.contentAs[ujson.Obj].get("name").str == """Beth""")
      case Failure(err) =>
        throw err
    }
  }

  test("error due to bad syntax") {
    cluster.query("""select*from""") match {
      case Success(result) =>
        assert(false)
      case Failure(QueryServiceException(errors)) =>
        println(errors)
      case Failure(err) =>
        throw err
    }
  }

}
