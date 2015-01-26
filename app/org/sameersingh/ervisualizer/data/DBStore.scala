package org.sameersingh.ervisualizer.data

import scala.collection.mutable
import scala.collection.mutable.HashMap

/**
 * @author sameer
 * @since 1/25/15.
 */
class DBStore(docs: DocumentStore) {
  type Id = String

  val maxDBs = 100
  val dbMap = new HashMap[Id, DB]
  val dbQueue = new mutable.Queue[Id]()
  val queryMap = new HashMap[String, Id]
  val queryIdMap = new HashMap[Id, String]

  def query(string: String): (Id, DB) = {
    val id = queryId(string)
    val odb = dbMap.get(id)
    id -> odb.getOrElse({
      val docIds = docs.query(string)
      val result = EntityInfoReader.read()
      //NLPReader.read(result, Some(name.getOrElse(defaultDBName)))
      if(dbMap.size == maxDBs) {
        val id = dbQueue.dequeue()
        dbMap.remove(id)
      }
      dbQueue += id
      dbMap(id) = result
      result
    })
  }

  def id(id: String) = {
    dbMap.getOrElse(id, query(queryIdMap(id))._2)
  }

  private def queryId(string: String): Id = {
    queryMap.getOrElseUpdate(string, {
      val id = "db" + queryMap.size
      queryIdMap(id) = string
      id
    })
  }

}
