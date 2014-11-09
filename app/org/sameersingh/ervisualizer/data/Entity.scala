package org.sameersingh.ervisualizer.data

/**
 * @author sameer
 * @since 6/10/14.
 */
case class EntityHeader(id: String, name: String, nerTag: String, popularity: Double, geo: Seq[Double] = Seq.empty)

case class EntityInfo(id: String, freebaseInfo: Map[String, String])

case class EntityFreebase(id: String, types: Seq[String])

case class EntityText(id: String, provenances: Seq[Provenance])

case class TypeModelProvenances(id: String, entityType: String, provenances: Seq[Provenance])

object EntityUtils {
  def emptyInfo(eid: String) = EntityInfo(eid, Map.empty)

  def emptyText(eid: String) = EntityText(eid, Seq.empty)

  def emptyKBA(eid: String) = org.sameersingh.ervisualizer.kba.Entity(eid, Seq.empty, Seq.empty, Seq.empty)

  def emptyFreebase(eid: String) = EntityFreebase(eid, Seq.empty)

  def emptyProvenance(eid: String) = EntityText(eid, Seq.empty)

  def emptyTypeProvenance(eid: String, et: String) = TypeModelProvenances(eid, et, Seq.empty)
}

case class RelationHeader(sourceId: String, targetId: String, popularity: Double)

case class RelationFreebase(sourceId: String, targetId: String, rels: Seq[String])

case class RelationText(sourceId: String, targetId: String, provenances: Seq[Provenance])

case class RelModelProvenances(sourceId: String, targetId: String, relType: String, provenances: Seq[Provenance], confidence: Double = 1.0)

object RelationUtils {
  def emptyFreebase(sid: String, tid: String) = RelationFreebase(sid, tid, Seq.empty)

  def emptyProvenance(sid: String, tid: String) = RelationText(sid, tid, Seq.empty)

  def emptyRelProvenance(sid: String, tid: String, rt: String) = RelModelProvenances(sid, tid, rt, Seq.empty)
}
