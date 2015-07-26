package org.sameersingh.ervisualizer.kba

import com.typesafe.config.ConfigFactory
import nlp_serde.FileUtil
import org.sameersingh.ervisualizer.data.{FreebaseReader, EntityUtils}
import org.sameersingh.ervisualizer.kba
import play.api.libs.json.Json

import scala.collection.mutable.HashMap
import scala.util.Random

/**
 * @author sameer
 * @since 1/11/15.
 */
trait KBAStore {

  def entityKBA(id: String): Entity


  def relationKBA(sid: String, tid: String): Entity
}

class InMemEntityKBA extends KBAStore {
  val _entityKBA = new HashMap[String, kba.Entity]
  val _relationKBA = new HashMap[(String, String), kba.Entity]

  override def entityKBA(id: String): kba.Entity = _entityKBA.getOrElse(id, EntityUtils.emptyKBA(id))

  override def relationKBA(sid: String, tid: String): kba.Entity = _relationKBA.getOrElse(sid -> tid, EntityUtils.emptyKBA(sid + "|" + tid))
}

object EntityKBAReader {
  def read(): KBAStore = {
    val db = new InMemEntityKBA
    val cfg = ConfigFactory.load()
    val baseDir = cfg.getString("nlp.data.baseDir") //.replaceAll(" ", "\\ ")
    //StalenessReader.readStaleness(baseDir + "/docs.staleness.json.gz", db, Some(100))
    db
  }
}


object StalenessReader {

  val random = new Random(0)
  /*
  * fill entityKBA and relationKBA
  */
  def readStaleness(stalenessFile: String, db: InMemEntityKBA, maxPoints: Option[Int] = None): Unit = {
    import org.sameersingh.ervisualizer.kba.JsonReads._
    println("Reading staleness")
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    for (line <- FileUtil.inputSource(stalenessFile, true).getLines()) {
      val oe = Json.fromJson[kba.Entity](Json.parse(line)).get
      val e = if(maxPoints.isEmpty) oe
      else {
        val stalenessSampleProb = maxPoints.get.toDouble / oe.staleness.size.toDouble
        val docSampleProb = maxPoints.get.toDouble / oe.docs.size.toDouble
        kba.Entity(oe.id, oe.staleness.filter(s => random.nextDouble() < stalenessSampleProb),
          oe.docs.filter(s => random.nextDouble() < docSampleProb), oe.clusters)
      }
      if(e.id.contains("|")) {
        val ids = e.id.split("\\|").map(s => FreebaseReader.convertFbIdToId(s))
        assert(ids.size == 2, s"More than 2 I in id?: ${e.id}: ${ids.mkString(", ")}")
        val rid = ids(0) -> ids(1)
        // db._relationIds += rid
        db._relationKBA(rid) = e
      } else {
        val id = FreebaseReader.convertFbIdToId(e.id)
        db._entityKBA(id) = e
      }
      docIdx += 1
      if (docIdx % dotEvery == 0) print(".")
      if (docIdx % lineEvery == 0) println(": read " + docIdx + " lines.")
    }
  }

  import JsonReads._

  def main(args: Array[String]): Unit = {
    for (line <- io.Source.fromFile(ConfigFactory.load().getString("nlp.data.baseDir") + "/docs.staleness.json").getLines()) {
      val e = Json.fromJson[kba.Entity](Json.parse(line)).get
      println(e)
    }
  }
}

