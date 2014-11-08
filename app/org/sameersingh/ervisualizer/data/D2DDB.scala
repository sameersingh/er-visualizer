package org.sameersingh.ervisualizer.data

import org.sameersingh.ervisualizer.nlp.{ReadMultiROutput, ReadProcessedDocs}
import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.freebase.MongoIO
import scala.collection.mutable
import play.api.libs.json.Json

/**
 * Created by sameer on 7/20/14.
 */
class D2DDB {

  def addRelationInfo(db: InMemoryDB) {
    val maxProvenances = db._relationText.map({
      case (rid, map) => map.provenances.size
    }).max.toDouble
    for ((rid, rt) <- db._relationText) {
      db._relationIds += rid
      // TODO read from freebase
      db._relationFreebase(rid) = RelationFreebase(rid._1, rid._2, Seq.empty)
      db._relationHeader(rid) = RelationHeader(rid._1, rid._2, rt.provenances.size.toDouble / maxProvenances)
    }
    val minScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).min
    val maxScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).max
    for ((pair, relMap) <- db._relationProvenances) {
      for ((r, rmps) <- relMap) {
        relMap(r) = RelModelProvenances(rmps.sourceId, rmps.targetId, rmps.relType, rmps.provenances,
          math.sqrt(rmps.provenances.map(p => (math.log(p.confidence) - minScore) / maxScore).max)) //sum / rmps.provenances.size.toDouble)
      }
    }
  }

  def readFromMongoJson(baseDir: String, db: InMemoryDB) {
    val ehf = io.Source.fromFile(baseDir + "/d2d.ent.head", "UTF-8").getLines()
    val eif = io.Source.fromFile(baseDir + "/d2d.ent.info", "UTF-8").getLines()
    val eff = io.Source.fromFile(baseDir + "/d2d.ent.freebase", "UTF-8").getLines()
    import JsonReads._
    for(ehl <- ehf; eil = eif.next(); efl = eff.next()) {
      val eh = Json.fromJson[EntityHeader](Json.parse(ehl)).get
      val ei = Json.fromJson[EntityInfo](Json.parse(eil)).get
      val ef = Json.fromJson[EntityFreebase](Json.parse(efl)).get
      assert(eh.id == ei.id)
      assert(eh.id == ef.id)
      val mid = eh.id
      if(db._entityHeader.contains(mid)) {
        db._entityHeader(mid) = eh
        db._entityInfo(mid) = ei
        db._entityFreebase(mid) = ef
      }
    }
    assert(eif.isEmpty)
    assert(eff.isEmpty)
  }

  def readDB(filelistSuffix: Option[String] = None): DB = {
    // read raw documents and entity links
    println("Read raw docs")
    val cfg = ConfigFactory.load()
    val baseDir = cfg.getString("nlp.data.baseDir")
    val filelist = if(filelistSuffix.isDefined) "d2d.filelist." + filelistSuffix.get else cfg.getString("nlp.data.filelist")
    val processedDocReader = new ReadProcessedDocs(baseDir, filelist)
    val (db, einfo) = processedDocReader.readAllDocs

    // fill entity info with freebase info
    println("Read mongo info")
    val mongo = new MongoIO("localhost", 27017)
    if(cfg.getBoolean("nlp.data.mongo")) mongo.updateDB(db.asInstanceOf[InMemoryDB])
    else readFromMongoJson(baseDir, db.asInstanceOf[InMemoryDB])

    // read relations and convert that to provenances
    println("Read relations")
    val relReader = new ReadMultiROutput(baseDir, filelist)
    relReader.updateFromAllDocs(db.asInstanceOf[InMemoryDB])

    // aggregate info to relations from provenances
    println("Aggregate relation info")
    addRelationInfo(db.asInstanceOf[InMemoryDB])

    db
  }
}

object D2DDB extends D2DDB

object StalenessReader {
  import org.sameersingh.ervisualizer.kba.JsonReads._
  import org.sameersingh.ervisualizer.kba._
  def main(args: Array[String]): Unit = {
    val inputfile =
    for(line <- io.Source.fromFile("/home/sameer/data/d2d/demo2015/nov/nigeria_dataset_v04.staleness.values.txt").getLines()) {
      val e = Json.fromJson[Entity](Json.parse(line)).get
      println(e)
    }
  }
}