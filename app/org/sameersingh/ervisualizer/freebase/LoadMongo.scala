package org.sameersingh.ervisualizer.freebase

import com.mongodb.casbah.Imports._
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import scala.collection.mutable.ArrayBuffer

/**
 * Created by sameer on 7/11/14.
 */
class LoadMongo(host: String = "localhost", port: Int) {
  val dbName = "freebase"

  val client = MongoClient(host, port)
  val db = client.getDB(dbName)

  def stripRDF(url: String): String = {
    if(url.startsWith("<http://rdf.freebase.com") && url.endsWith(">"))
        url.replaceAll("<http://rdf.freebase.com/ns/", "").replaceAll("<http://rdf.freebase.com/key/", "").dropRight(1)
      else url
  }

  class MongoInsertBuffer(val coll: MongoCollection, val size: Int) {
    val buffer = new ArrayBuffer[MongoDBObject]

    def insert(d: MongoDBObject) {
      buffer += d
      if(buffer.size >= size) {
        forceInsert()
      }
    }

    def insertAll(ds: Iterable[MongoDBObject]) {
      buffer ++= ds
      if(buffer.size >= size) {
        forceInsert()
      }
    }

    def forceInsert() {
      //print("mongo: inserting %d objects... " format (buffer.size))
      coll.insert(buffer:_*)
      //println("done.")
      buffer.clear()
    }
  }

  def loadEntityNames(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).endsWith("@en"))
    }

    def cleanValue(value: String): String = value.drop(1).dropRight(4)

    val buffer = new MongoInsertBuffer(db("entityNames"), 100000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for(l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      if(filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "name" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex("entity")
  }

  def loadEntityImages(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).startsWith("m."))
    }

    def cleanValue(value: String): String = value

    val buffer = new MongoInsertBuffer(db("entityImages"), 100000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for(l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      split(2) = stripRDF(split(2))
      if(filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "img" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex("entity")
  }

  def loadEntityDescription(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).endsWith("@en"))
    }

    def cleanValue(value: String): String = value.drop(1).dropRight(4)

    val buffer = new MongoInsertBuffer(db("entityDescription"), 10000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for(l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      if(filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "desc" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex("entity")
  }

  def loadEntityTypes(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).startsWith("m."))
    }

    def cleanValue(value: String): String = value

    val buffer = new MongoInsertBuffer(db("entityTypes"), 100000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for(l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      split(2) = stripRDF(split(2))
      if(filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "type" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex("entity")
  }
}

object LoadMongo extends LoadMongo("localhost", 27017) {
  val baseDir = "/home/sameer/data/freebase/"
  def main(args: Array[String]) {
    print("Writing names... ")
    this.loadEntityNames(baseDir + "type.object.name.gz")
    println("done.")

    print("Writing images... ")
    this.loadEntityImages(baseDir + "common.topic.image.gz")
    println("done.")

    print("Writing description... ")
    this.loadEntityDescription(baseDir + "common.topic.description.gz")
    println("done.")

    print("Writing types... ")
    this.loadEntityTypes(baseDir + "common.topic.notable_types.gz")
    println("done.")
  }
}