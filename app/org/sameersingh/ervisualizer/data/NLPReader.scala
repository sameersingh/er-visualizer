package org.sameersingh.ervisualizer.data

import java.io.{FileOutputStream, OutputStreamWriter, PrintWriter}

import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.Logging
import nlp_serde.{FileUtil, Mention, Entity}
import nlp_serde.readers.PerLineJsonReader
import org.sameersingh.ervisualizer.freebase.MongoIO
import play.api.libs.json.Json

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * @author sameer
 * @since 11/8/14.
 */
object FreebaseReader {

  def convertFbIdToId(mid: String): String = mid.drop(1).replaceFirst("/", "_")

  def convertIdToFbId(mid: String): String = "/" + mid.replaceFirst("_", "/")

  /*
  * add entityIds, and fill entityHeader, entityInfo and entityFreebase
  */
  def readFreebaseInfo(db: InMemoryDB, freebaseDir: String): Unit = {
    println("Reading freebase info")
    val cfg = ConfigFactory.load()
    val useMongo = cfg.getBoolean("nlp.data.mongo")
    if (useMongo) {
      val mongo = new MongoIO(port = 27017)
      mongo.updateDB(db)
    } else {
      val ehf = io.Source.fromFile(freebaseDir + "/ent.head", "UTF-8").getLines()
      val eif = io.Source.fromFile(freebaseDir + "/ent.info", "UTF-8").getLines()
      val eff = io.Source.fromFile(freebaseDir + "/ent.freebase", "UTF-8").getLines()
      import JsonReads._
      for (ehl <- ehf; eil = eif.next(); efl = eff.next()) {
        val eh = Json.fromJson[EntityHeader](Json.parse(ehl)).get
        val ei = Json.fromJson[EntityInfo](Json.parse(eil)).get
        val ef = Json.fromJson[EntityFreebase](Json.parse(efl)).get
        assert(eh.id == ei.id)
        assert(eh.id == ef.id)
        val mid = eh.id
        if(db._entityIds.contains(mid)) {
          //db._entityIds += mid
          val oeh = db._entityHeader(mid)
          println("Found" + oeh + " for " + eh)
          db._entityHeader(mid) = EntityHeader(oeh.id, eh.name, oeh.nerTag, oeh.popularity, eh.geo)
          db._entityInfo(mid) = ei
          db._entityFreebase(mid) = ef
        }
      }
      assert(eif.isEmpty)
      assert(eff.isEmpty)
    }
  }
}

class NLPReader extends Logging {

  def entityId(e: Entity): Option[String] = {
    if (e.ner.get != "O" && e.freebaseIds.size > 0)
      Some(FreebaseReader.convertFbIdToId(e.freebaseIds.maxBy(_._2)._1))
    else None
  }

  def normalizeType(str: String) = {
    var s = str
    val idx = math.max(0, str.indexOf("|"))
    if(s.startsWith("/")) s = s.drop(1)
    s = s.substring(idx).replaceAll("/", ":")
    s
  }

  class EntityMentions {
    val mentions = new mutable.HashMap[String, ArrayBuffer[Mention]]

    def +=(eid: String, m: Mention) {
      mentions.getOrElseUpdate(eid, new ArrayBuffer) += m
    }
  }

  def readDocs(docsFile: String, db: InMemoryDB): Unit = {
    val reader = new PerLineJsonReader()
    readDocs(reader.read(docsFile), db)
  }

  /*
   * Read the documents, and populate relevantEntityIds and provenances
   */
  def readDocs(docs: Iterator[nlp_serde.Document], db: InMemoryDB): Unit = {
    logger.info("Reading documents")
    //db.clearTextEvidence()
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    val einfo = new EntityMentions
    for (d <- docs) {
      // sentences
      val sents = d.sentences.map(s => Sentence(d.id, s.idx, s.text))
      db._documents(d.id) = Document(d.id, d.path.getOrElse(""), d.attrs.getOrElse("title", ""), "", d.text, sents)
      // entities
      for (e <- d.entities) {
        for (id <- entityId(e)) {
          if (!db._entityIds.contains(id)) {
            db._entityIds += id
          }
          val mentions = e.mids.map(id => d.mentions(id)).toSeq
          mentions.foreach(m => einfo +=(id, m))
          val provenances = mentions.map(m => {
            val startPos = d.sentences(m.sentenceId-1).tokens(m.toks._1 - 1).chars._1 - d.sentences(m.sentenceId-1).chars._1
            val endPos = d.sentences(m.sentenceId-1).tokens(m.toks._2 - 2).chars._2 - d.sentences(m.sentenceId-1).chars._1
            Provenance(d.id, m.sentenceId-1, Seq(startPos -> endPos))
          }).distinct
          val oldEText = db._entityText.getOrElse(id, EntityUtils.emptyText(id))
          val etext = EntityText(id, provenances ++ oldEText.provenances)
          db._entityText(id) = etext
          for (p <- provenances) {
            val map = db._docEntityProvenances.getOrElseUpdate(p.docId -> (p.sentId - 1), new mutable.HashMap)
            map(id) = map.getOrElse(id, Seq.empty) ++ Seq(p)
          }
          // Types
          val types = new mutable.HashSet[String]()
          val typeProvs = new mutable.HashMap[String, TypeModelProvenances]
          for (m <- mentions) {
            val figerStr = m.attrs.get("figer")
            val figerPreds = figerStr.toSeq.filterNot(_.isEmpty).flatMap(str => {
              str.split("[,\t]").map(str => {
                val pair = str.split("@")
                (normalizeType(pair(0)), pair(1).toDouble)
              }).toSeq
            })
            val startPos = d.sentences(m.sentenceId-1).tokens(m.toks._1 - 1).chars._1 - d.sentences(m.sentenceId-1).chars._1
            val endPos = d.sentences(m.sentenceId-1).tokens(m.toks._2 - 2).chars._2 - d.sentences(m.sentenceId-1).chars._1
            // add to local data
            types ++= figerPreds.map(_._1)
            for (tc <- figerPreds; t = tc._1; c = tc._2) {
              val prov = Provenance(d.id, m.sentenceId-1, Seq(startPos -> endPos), c)
              typeProvs(t) = TypeModelProvenances(id, t, typeProvs.get(t).map(_.provenances).getOrElse(Seq.empty) ++ Seq(prov))
            }
          }
          // add to global data
          if (!types.isEmpty) {
            // println(s"$id: " + types.mkString(", "))
            db._entityTypePredictions(id) = (db._entityTypePredictions.getOrElse(id, Seq.empty) ++ types.toSeq).distinct
          }
          for ((t, tmp) <- typeProvs) {
            val map = db._entityTypeProvenances.getOrElseUpdate(id, new mutable.HashMap)
            map(t) = TypeModelProvenances(id, t, map.get(t).map(_.provenances).getOrElse(Seq.empty) ++ tmp.provenances)
          }
        }
      }
      // relations
      for (s <- d.sentences; r <- s.relations;
           m1 = d.mentions(r.m1Id);
           m2 = d.mentions(r.m2Id);
           //m1 <- s.mentions.find(_.id == r.m1Id);
           //m2 <- s.mentions.find(_.id == r.m2Id);
           m1eid <- m1.entityId;
           m2eid <- m2.entityId;
           e1 = d.entity(m1eid); //.find(_.id == m1eid);
           e2 = d.entity(m2eid)) {
        //.find(_.id == m2eid)) {
        assert(e1.id == m1eid, s"e1.id (${e1.id}) is not same as m1eid (${m1eid}), entities: ${d.entities.map(_.id).mkString(",")}")
        assert(e2.id == m2eid, s"e2.id (${e2.id}) is not same as m1eid (${m2eid}), entities: ${d.entities.map(_.id).mkString(",")}")
        for (e1id <- entityId(e1); e2id <- entityId(e2); if (db._entityIds.contains(e1id)); if (db._entityIds.contains(e2id))) {
          val startPos1 = s.tokens(m1.toks._1 - 1).chars._1 - s.chars._1
          val endPos1 = s.tokens(m1.toks._2 - 2).chars._2 - s.chars._1
          val startPos2 = s.tokens(m2.toks._1 - 1).chars._1 - s.chars._1
          val endPos2 = s.tokens(m2.toks._2 - 2).chars._2 - s.chars._1
          val prov = Provenance(d.id, s.idx, Seq(startPos1 -> endPos1, startPos2 -> endPos2))
          val rid = if (e1id < e2id) e1id -> e2id else e2id -> e1id
          for (unnormRel <- r.relations; rel = normalizeType(unnormRel)) {
            // db._relevantRelationIds += rid
            db._relationPredictions(rid) = db._relationPredictions.getOrElse(rid, Set.empty) ++ Seq(rel)
            val rt = db._relationText.getOrElse(rid, RelationText(rid._1, rid._2, Seq.empty))
            db._relationText(rid) = RelationText(rt.sourceId, rt.targetId, rt.provenances ++ Seq(prov))
            val rmp = db._relationProvenances.getOrElseUpdate(rid, new mutable.HashMap).getOrElseUpdate(rel, RelModelProvenances(rid._1, rid._2, rel, Seq.empty))
            db._relationProvenances(rid)(rel) = RelModelProvenances(rmp.sourceId, rmp.targetId, rmp.relType, rmp.provenances ++ Seq(prov))
          }
        }
      }
      docIdx += 1
      if (docIdx % dotEvery == 0) print(".")
      if (docIdx % lineEvery == 0) println(": read " + docIdx + " docs")
    }
    println(" Done.")
    logger.info("Entities: " + db.entityIds.size)
    logger.info("Relations: " + db._relationText.size)
    // entity header
    if (einfo.mentions.size != 0) {
      val maxMentions = einfo.mentions.values.map(_.size).max
      for (eid <- db.entityIds) {
        if (!einfo.mentions.contains(eid)) {
          logger.info(s"Cannot find $eid in the documents..")
          // System.exit(0)
        }
        val ments = einfo.mentions.getOrElse(eid, Seq.empty)
        val name = if (ments.isEmpty) "unknown" else ments.map(_.text).groupBy(x => x).map(p => p._1 -> p._2.size).maxBy(_._2)._1
        val ner = if (ments.isEmpty) "O" else ments.flatMap(_.ner.toSeq).groupBy(x => x).map(p => p._1 -> p._2.size).maxBy(_._2)._1
        db._entityHeader(eid) = EntityHeader(eid, name, ner, ments.size.toDouble / maxMentions)
      }
    }
  }

  /*
  * Fill relationFreebase and relationHeaders, and update relationProvenances
  */
  def addRelationInfo(db: InMemoryDB) {
    println("Add relational info")
    if (db._relationText.size > 0) {
      val maxProvenances = db._relationText.map({
        case (rid, map) => map.provenances.size
      }).max.toDouble
      for ((rid, rt) <- db._relationText) {
        db._relationFreebase(rid) = RelationFreebase(rid._1, rid._2, Seq.empty)
        db._relationHeader(rid) = RelationHeader(rid._1, rid._2, rt.provenances.size.toDouble / maxProvenances)
      }
      // val minScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).min
      // val maxScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).max
      val maxProvs = db._relationProvenances.values.flatten.map(_._2.provenances.size).max
      for ((pair, relMap) <- db._relationProvenances) {
        for ((r, rmps) <- relMap) {
          relMap(r) = RelModelProvenances(rmps.sourceId, rmps.targetId, rmps.relType, rmps.provenances,
            math.sqrt(rmps.provenances.size.toDouble / maxProvs))
          //math.sqrt(rmps.provenances.map(p => (math.log(p.confidence) - minScore) / maxScore).max)) //sum / rmps.provenances.size.toDouble)
        }
      }
    }
  }

  /*
  * remove all the non-location entities that don't participate in relations
  */
  def removeSingletonEntities(db: InMemoryDB): Unit = {
    println("Removing singleton entities")
    val entsToRemove = new ArrayBuffer[String]
    for (eid <- db.entityIds) {
      val location = !db.entityHeader(eid).geo.isEmpty // nerTag == "LOCATION"
      if (!location) {
        val relations = db.relations(eid)
        if (relations.size == 0) entsToRemove += eid
      }
    }
    for (eid <- entsToRemove) db._entityIds -= eid
  }

  def read(db: DB): Unit = db match {
    case inDB: InMemoryDB => {
      val cfg = ConfigFactory.load()
      val baseDir = cfg.getString("nlp.data.baseDir") //.replaceAll(" ", "\\ ")
      val docsFile = cfg.getString("nlp.data.docsFile")
      readDocs(baseDir + "/" + docsFile, inDB)
      addRelationInfo(inDB)
      removeSingletonEntities(inDB)
    }
  }

}

object EntityInfoReader {
  def read(db: InMemoryDB): DB = {
    val cfg = ConfigFactory.load()
    val baseDir = cfg.getString("nlp.data.baseDir") //.replaceAll(" ", "\\ ")
    //StalenessReader.readStaleness(baseDir + "/docs.staleness.json.gz", db)
    FreebaseReader.readFreebaseInfo(db, baseDir)
    db
  }
}

object NLPReader extends NLPReader
