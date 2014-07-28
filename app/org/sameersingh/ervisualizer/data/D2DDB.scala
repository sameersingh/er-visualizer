package org.sameersingh.ervisualizer.data

import org.sameersingh.ervisualizer.nlp.{ReadMultiROutput, ReadProcessedDocs}
import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.freebase.MongoIO

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

  def readDB: DB = {
    // read raw documents and entity links
    println("Read raw docs")
    val cfg = ConfigFactory.load()
    val baseDir = cfg.getString("nlp.data.baseDir")
    val filelist = cfg.getString("nlp.data.filelist")
    val processedDocReader = new ReadProcessedDocs(baseDir, filelist)
    val (db, einfo) = processedDocReader.readAllDocs

    // fill entity info with freebase info
    println("Read mongo info")
    val mongo = new MongoIO("localhost", 27017)
    if(cfg.getBoolean("nlp.data.mongo")) mongo.updateDB(db.asInstanceOf[InMemoryDB])

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