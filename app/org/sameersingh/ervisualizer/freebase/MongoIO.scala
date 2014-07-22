package org.sameersingh.ervisualizer.freebase

import com.mongodb.casbah.Imports._
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import org.sameersingh.ervisualizer.data.{EntityFreebase, EntityInfo, InMemoryDB}

/**
 * Created by sameer on 7/11/14.
 */
class MongoIO(host: String = "localhost", port: Int) {
  val dbName = "freebase"

  val client = MongoClient(host, port)
  val db = client.getDB(dbName)

  def stripRDF(url: String): String = {
    if (url.startsWith("<http://rdf.freebase.com") && url.endsWith(">"))
      url.replaceAll("<http://rdf.freebase.com/ns/", "").replaceAll("<http://rdf.freebase.com/key/", "").dropRight(1)
    else url
  }

  class MongoInsertBuffer(val coll: MongoCollection, val size: Int) {
    val buffer = new ArrayBuffer[MongoDBObject]

    def insert(d: MongoDBObject) {
      buffer += d
      if (buffer.size >= size) {
        forceInsert()
      }
    }

    def insertAll(ds: Iterable[MongoDBObject]) {
      buffer ++= ds
      if (buffer.size >= size) {
        forceInsert()
      }
    }

    def forceInsert() {
      //print("mongo: inserting %d objects... " format (buffer.size))
      coll.insert(buffer: _*)
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
    for (l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      if (filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "name" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex(MongoDBObject("entity" -> 1))
  }

  def loadEntityImages(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).startsWith("m."))
    }

    def cleanValue(value: String): String = value

    val buffer = new MongoInsertBuffer(db("entityImages"), 100000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for (l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      split(2) = stripRDF(split(2))
      if (filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "img" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex(MongoDBObject("entity" -> 1))
  }

  def loadEntityDescription(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).endsWith("@en"))
    }

    def cleanValue(value: String): String = value.drop(1).dropRight(4)

    val buffer = new MongoInsertBuffer(db("entityDescription"), 10000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for (l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      if (filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "desc" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex(MongoDBObject("entity" -> 1))
  }

  def loadEntityTypes(fname: String) {

    def filter(split: Array[String]): Boolean = {
      (split(0).startsWith("m.") && split(2).startsWith("m."))
    }

    def cleanValue(value: String): String = value

    val buffer = new MongoInsertBuffer(db("entityTypes"), 100000)
    val source = io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(fname)))
    for (l <- source.getLines()) {
      val split = l.split("\\t")
      split(0) = stripRDF(split(0))
      split(2) = stripRDF(split(2))
      if (filter(split)) {
        val d = MongoDBObject("entity" -> split(0), "type" -> cleanValue(split(2)))
        buffer.insert(d)
      }
    }
    source.close()
    buffer.forceInsert()
    buffer.coll.createIndex(MongoDBObject("entity" -> 1))
  }

  def readFromDB(mids: Seq[String],
                 collName: String,
                 value: DBObject => String): scala.collection.Map[String, String] = {
    val coll = db(collName)
    val result = new mutable.HashMap[String, String]
    for (mid <- mids) {
      coll.findOne("entity" $eq mid).foreach(o => result(mid) = value(o))
    }
    result
  }

  def readNames(mids: Seq[String]): scala.collection.Map[String, String] = readFromDB(mids, "entityNames", o => o.get("name").toString)

  def readImages(mids: Seq[String]): scala.collection.Map[String, String] = readFromDB(mids, "entityImages", o => o.get("img").toString)

  def readDescriptions(mids: Seq[String]): scala.collection.Map[String, String] = readFromDB(mids, "entityDescription", o => o.get("desc").toString)

  def readTypes(mids: Seq[String]): scala.collection.Map[String, String] = readFromDB(mids, "entityTypes", o => o.get("type").toString)

  def updateDB(store: InMemoryDB) {
    val nameColl = db("entityNames")
    val imgColl = db("entityImages")
    val descColl = db("entityDescription")
    val typeColl = db("entityTypes")
    var index = 0
    val numEnts = store.entityIds.size
    for (mid <- store.entityIds) {
      // info
      // TODO: query mongo
      val info = new mutable.HashMap[String, String]
      val queryID = mid.replace('_', '.')
      info("/mid") = "/" + mid.replace('_', '/')
      nameColl.findOne("entity" $eq queryID).map(o => o.get("name").toString).foreach(v => info("Name") = v)
      imgColl.findOne("entity" $eq queryID).map(o => o.get("img").toString).foreach(v => info("/common/topic/image") = "/" + v.replace('.', '/'))
      descColl.findOne("entity" $eq queryID).map(o => o.get("desc").toString).foreach(v => info("/common/topic/description") = v)
      store._entityInfo(mid) = EntityInfo(mid, info.toMap)
      // freebase
      store._entityFreebase(mid) = EntityFreebase(mid, typeColl.find("entity" $eq queryID).map(o => o.get("type").toString).map(imgID => nameColl.findOne("entity" $eq imgID).map(_.get("name").toString)).flatten.toSeq)
      // print it
      //println(store.entityHeader(mid))
      //println(store.entityInfo(mid))
      //println(store.entityFreebase(mid))
      //println("---------------------")
      index += 1
      if(index % (numEnts/100) == 0) print(".")
    }
    println
  }
}

object LoadMongo extends MongoIO("localhost", 27017) {
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