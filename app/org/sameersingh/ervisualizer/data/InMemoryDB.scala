package org.sameersingh.ervisualizer.data

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashMap, LinkedHashMap}
import scala.io.Source
import java.io.PrintWriter

import org.sameersingh.ervisualizer.kba

/**
 * @author sameer
 * @since 6/10/14.
 */
class InMemoryDB extends DB {

  // Documents and Sentences
  val _documents = new LinkedHashMap[String, Document]

  override def docIds: Seq[String] = _documents.keysIterator.toSeq

  override def document(docId: String): Document = _documents(docId)

  // Entities
  val _entityIds = new mutable.LinkedHashSet[String]
  val _entityHeader = new HashMap[String, EntityHeader]
  val _entityInfo = new HashMap[String, EntityInfo]
  val _entityFreebase = new HashMap[String, EntityFreebase]
  val _entityText = new HashMap[String, EntityText]
  val _entityTypePredictions = new HashMap[String, Seq[String]]
  val _entityTypeProvenances = new HashMap[String, HashMap[String, TypeModelProvenances]]
  val _docEntityProvenances = new HashMap[(String, Int), HashMap[String, Seq[Provenance]]]

  override def entityIds: Iterable[String] = _entityIds.toIterable

  override def entityHeader(id: String): EntityHeader = _entityHeader(id)

  override def entityInfo(id: String): EntityInfo = _entityInfo.getOrElse(id, EntityUtils.emptyInfo(id))

  override def entityFreebase(id: String): EntityFreebase = _entityFreebase.getOrElse(id, EntityUtils.emptyFreebase(id))

  override def entityText(id: String): EntityText = _entityText.getOrElse(id, EntityUtils.emptyProvenance(id))

  override def entityTypePredictions(id: String): Seq[String] = _entityTypePredictions.getOrElse(id, Seq.empty)

  override def entityTypeProvenances(id: String, etype: String): TypeModelProvenances =
    _entityTypeProvenances.getOrElse(id, new HashMap).getOrElse(etype, EntityUtils.emptyTypeProvenance(id, etype))

  override def docEntityProvenances(docId: String, sentId: Int): Seq[(String, Seq[Provenance])] = _docEntityProvenances.getOrElse(docId -> sentId, Seq.empty).toSeq

  // Relations
  // val _relationIds = new ArrayBuffer[(String, String)]
  val _relationHeader = new LinkedHashMap[(String, String), RelationHeader]
  val _relationFreebase = new HashMap[(String, String), RelationFreebase]
  val _relationText = new HashMap[(String, String), RelationText]
  val _relationPredictions = new HashMap[(String, String), Set[String]]
  val _relationProvenances = new HashMap[(String, String), HashMap[String, RelModelProvenances]]

  override def relationIds: Seq[(String, String)] = _relationHeader.keysIterator.toSeq

  override def relevantRelationIds: Iterator[(String, String)] = throw new Error("should not call relevant relations") //_relevantRelationIds.iterator

  override def relationHeader(sid: String, tid: String): RelationHeader = _relationHeader(sid -> tid)

  override def relationFreebase(sid: String, tid: String): RelationFreebase = _relationFreebase.getOrElse(sid -> tid, RelationUtils.emptyFreebase(sid, tid))

  override def relationText(sid: String, tid: String): RelationText = _relationText.getOrElse(sid -> tid, RelationUtils.emptyProvenance(sid, tid))

  override def relationPredictions(sid: String, tid: String): Seq[String] = _relationPredictions.getOrElse(sid -> tid, Set.empty).toSeq

  override def relationProvenances(sid: String, tid: String, etype: String): RelModelProvenances =
    _relationProvenances.getOrElse(sid -> tid, new HashMap).getOrElse(etype, RelationUtils.emptyRelProvenance(sid, tid, etype))

  def clearTextEvidence() = {
    _documents.clear()
    _docEntityProvenances.clear()

    _entityText.clear()
    _entityTypePredictions.clear()
    _entityTypeProvenances.clear()

    // _relevantRelationIds.clear()
    _relationPredictions.clear()
    _relationProvenances.clear()
    _relationText.clear()
  }
}

object InMemoryDB {

  object Files {
    val sentences = "sentences.tsv"
    val entityInfo = "entity.tsv"
    val entityText = "entityText.tsv"
    val entityTypeProvenances = "entityTypes.tsv"
    val relationInfo = "relation.tsv"
    val relationText = "relationText.tsv"
    val relationProvenances = "relationTypes.tsv"
  }

  def readTSVFile[A](file: String, parse: Array[String] => A): Seq[A] = {
    val source = Source.fromFile(file)
    val result = new ArrayBuffer[A]
    for (line <- source.getLines()) {
      val split = line.split("\\t")
      result += parse(split)
    }
    source.close()
    result
  }

  def parseEntityInfo(split: Array[String]): (EntityHeader, EntityInfo, EntityFreebase) = {
    assert(split.length == 6, split.mkString("-|-"))
    val id = split(0)
    val name = split(1)
    val nerTag = split(2)
    val popularity = split(3).toDouble
    val freebaseInfo = split(4).split("\\|\\|\\|").map(kv => {
      val s = kv.split(":::");
      assert(s.length == 2, kv + "\t" + s.mkString("-|-"));
      s(0) -> s(1)
    }).toMap
    val types = split(5).split(":::").toSeq
    (EntityHeader(id, name, nerTag, popularity), EntityInfo(id, freebaseInfo), EntityFreebase(id, types))
  }

  def parseRelationInfo(split: Array[String]): (RelationHeader, RelationFreebase) = {
    assert(split.length == 4, split.mkString("-|-"))
    val sid = split(0)
    val tid = split(1)
    val popularity = split(2).toDouble
    val rels = split(3).split(":").toSeq
    (RelationHeader(sid, tid, popularity), RelationFreebase(sid, tid, rels))
  }

  def parseProvenance(string: String): Provenance = {
    val split = string.split("\\|")
    assert(split.length == 3, split.mkString("-|-"))
    val docId = split(0)
    val sentId = split(1).toInt
    val tokPos = split(2).split(",").map(kv => {
      val s = kv.split(":");
      assert(s.length == 2, kv + "\t" + s.mkString("-|-"));
      s(0).toInt -> s(1).toInt
    }).toSeq
    Provenance(docId, sentId, tokPos)
  }

  def serializeEntityInfo(eh: EntityHeader, ei: EntityInfo, ef: EntityFreebase): String =
    "%s\t%s\t%s\t%f\t%s\t%s" format(eh.id, eh.name, eh.nerTag, eh.popularity, ei.freebaseInfo.map(kv => kv._1 + ":::" + kv._2.replaceAll("\\t", " ")).mkString("|||"), ef.types.mkString(":::"))

  def serializeRelationInfo(rh: RelationHeader, rf: RelationFreebase): String =
    "%s\t%s\t%f\t%s" format(rh.sourceId, rh.targetId, rh.popularity, rf.rels.mkString(":"))

  def serializeProvenance(p: Provenance): String =
    "%s|%d|%s" format(p.docId, p.sentId, p.tokPos.map(ii => ii._1 + ":" + ii._2).mkString(","))

  def readFromTSV(dir: String): InMemoryDB = {
    val db = new InMemoryDB
    // read documents
    // each sentence is, in order: docId, Text
    val sents = readTSVFile[(String, String)](dir + "/" + Files.sentences, s => (s(0), s(1)))
    for ((docId, docSents) <- sents.groupBy(_._1)) {
      val sentBuffer = new ArrayBuffer[Sentence]
      for (s <- docSents) {
        sentBuffer += Sentence(docId, sentBuffer.size, s._2)
      }
      db._documents(docId) = Document(docId, "", "", "", sentBuffer.map(_.string).mkString(" "), sentBuffer)
    }
    // ENTITIES
    // id and header and info and freebase types
    for ((eh, ei, eft) <- readTSVFile[(EntityHeader, EntityInfo, EntityFreebase)](dir + "/" + Files.entityInfo, parseEntityInfo)) {
      db._entityIds += eh.id
      db._entityHeader(eh.id) = eh
      db._entityInfo(eh.id) = ei
      db._entityFreebase(eh.id) = eft
    }
    // text provenance
    for ((id, idPs) <- readTSVFile[(String, Provenance)](dir + "/" + Files.entityText,
      (arr: Array[String]) => (arr(0), parseProvenance(arr(1)))).groupBy(_._1)) {
      db._entityText(id) = EntityText(id, idPs.map(_._2))
    }
    // type provenances
    val typeProvenances = readTSVFile[(String, String, Provenance)](dir + "/" + Files.entityTypeProvenances,
      (arr: Array[String]) => (arr(0), arr(1), parseProvenance(arr(2))))
    for ((id, idPs) <- typeProvenances.groupBy(_._1).toSeq) {
      val tps = new HashMap[String, TypeModelProvenances]
      for ((et, idTypePs) <- idPs.groupBy(_._2)) {
        tps(et) = TypeModelProvenances(id, et, idTypePs.map(_._3).toSeq)
      }
      db._entityTypePredictions(id) = tps.map(_._1).toSeq
      db._entityTypeProvenances(id) = tps
    }
    // RELATIONS
    // ids and header and freebase types
    for ((eh, eft) <- readTSVFile[(RelationHeader, RelationFreebase)](dir + "/" + Files.relationInfo, parseRelationInfo)) {
      val id = eh.sourceId -> eh.targetId
      // db._relationIds += id
      db._relationHeader(id) = eh
      db._relationFreebase(id) = eft
    }
    // text provenance
    for ((id, idPs) <- readTSVFile[((String, String), Provenance)](dir + "/" + Files.relationText,
      (arr: Array[String]) => (arr(0) -> arr(1), parseProvenance(arr(2)))).groupBy(_._1)) {
      db._relationText(id) = RelationText(id._1, id._2, idPs.map(_._2))
    }
    // type provenances
    val relProvenances = readTSVFile[((String, String), String, Provenance)](dir + "/" + Files.relationProvenances,
      (arr: Array[String]) => (arr(0) -> arr(1), arr(2), parseProvenance(arr(3))))
    for ((id, idPs) <- relProvenances.groupBy(_._1).toSeq) {
      val tps = new HashMap[String, RelModelProvenances]
      for ((et, idTypePs) <- idPs.groupBy(_._2)) {
        tps(et) = RelModelProvenances(id._1, id._2, et, idTypePs.map(_._3).toSeq)
      }
      db._relationPredictions(id) = tps.map(_._1).toSet
      db._relationProvenances(id) = tps
    }
    db
  }

  def writeDB(dir: String, db: DB) {
    // docs
    val docsWriter = new PrintWriter(dir + "/" + Files.sentences)
    for (dId <- db.docIds) {
      for (s <- db.document(dId).sents)
        docsWriter.println("%s\t%s" format(dId, s.string))
    }
    docsWriter.flush()
    docsWriter.close()
    // entities
    val entityInfoWriter = new PrintWriter(dir + "/" + Files.entityInfo)
    val entityTextWriter = new PrintWriter(dir + "/" + Files.entityText)
    val entityTypeProvenanceWriter = new PrintWriter(dir + "/" + Files.entityTypeProvenances)
    for (eId <- db.entityIds) {
      // info
      val eh = db.entityHeader(eId)
      val ei = db.entityInfo(eId)
      val ef = db.entityFreebase(eId)
      entityInfoWriter.println(serializeEntityInfo(eh, ei, ef))
      // text
      val etext = db.entityText(eId)
      for (p <- etext.provenances) {
        entityTextWriter.println(eId + "\t" + serializeProvenance(p))
      }
      // type
      for (t <- db.entityTypePredictions(eId))
        for (p <- db.entityTypeProvenances(eId, t).provenances) {
          entityTypeProvenanceWriter.println("%s\t%s\t%s" format(eId, t, serializeProvenance(p)))
        }
    }
    entityInfoWriter.flush()
    entityTextWriter.flush()
    entityTypeProvenanceWriter.flush()
    entityInfoWriter.close()
    entityTextWriter.close()
    entityTypeProvenanceWriter.close()
    // relations
    val relationInfoWriter = new PrintWriter(dir + "/" + Files.relationInfo)
    val relationTextWriter = new PrintWriter(dir + "/" + Files.relationText)
    val relationProvenanceWriter = new PrintWriter(dir + "/" + Files.relationProvenances)
    for ((sid, tid) <- db.relationIds) {
      // info
      val rh = db.relationHeader(sid, tid)
      val rf = db.relationFreebase(sid, tid)
      relationInfoWriter.println(serializeRelationInfo(rh, rf))
      // text
      val rtext = db.relationText(sid, tid)
      for (p <- rtext.provenances) {
        relationTextWriter.println(sid + "\t" + tid + "\t" + serializeProvenance(p))
      }
      // type
      for (t <- db.relationPredictions(sid, tid))
        for (p <- db.relationProvenances(sid, tid, t).provenances) {
          relationProvenanceWriter.println("%s\t%s\t%s\t%s" format(sid, tid, t, serializeProvenance(p)))
        }
    }
    relationInfoWriter.flush()
    relationTextWriter.flush()
    relationProvenanceWriter.flush()
    relationInfoWriter.close()
    relationTextWriter.close()
    relationProvenanceWriter.close()
  }
}