package org.sameersingh.ervisualizer.kba

import com.typesafe.config.ConfigFactory
import nlp_serde.FileUtil
import org.sameersingh.ervisualizer.data.{FreebaseReader, EntityUtils}
import org.sameersingh.ervisualizer.kba
import play.api.libs.json.Json

import scala.collection.mutable.HashMap

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
    StalenessReader.readStaleness(baseDir + "/docs.staleness.json.gz", db)
    db
  }
}


object StalenessReader {

  /*
  * fill entityKBA and relationKBA
  */
  def readStaleness(stalenessFile: String, db: InMemEntityKBA): Unit = {
    import org.sameersingh.ervisualizer.kba.JsonReads._
    println("Reading staleness")
    for (line <- FileUtil.inputSource(stalenessFile, true).getLines()) {
      val e = Json.fromJson[kba.Entity](Json.parse(line)).get
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
    }
  }

  import JsonReads._

  def main(args: Array[String]): Unit = {
    for (line <- io.Source.fromFile("data/d2d/docs.staleness.json").getLines()) {
      val e = Json.fromJson[kba.Entity](Json.parse(line)).get
      println(e)
    }
  }
}

