package org.sameersingh.ervisualizer.data

/**
 * @author sameer
 * @since 6/10/14.
 */
trait DB {

  def docIds: Seq[String]

  def document(docId: String): Document

  def sentence(docId: String, sentId: Int): Sentence = document(docId).sents(sentId)

  def docEntityProvenances(docId: String, sentId: Int): Seq[(String, Seq[Provenance])]

  def entityIds: Iterable[String]

  def entityHeader(id: String): EntityHeader

  def entityInfo(id: String): EntityInfo

  def entityFreebase(id: String): EntityFreebase

  def entityText(id: String): EntityText

  def entityTypePredictions(id: String): Seq[String]

  def entityTypeProvenances(id: String, etype: String): TypeModelProvenances

  def relationIds: Seq[(String, String)]

  def relevantRelationIds: Iterator[(String, String)]

  def relations(sourceId: String): Seq[(String, String)] = relationIds.filter(id => id._1 == sourceId || id._2 == sourceId)

  def relationHeader(sid: String, tid: String): RelationHeader

  def relationFreebase(sid: String, tid: String): RelationFreebase

  def relationText(sid: String, tid: String): RelationText

  def relationPredictions(sid: String, tid: String): Seq[String]

  def relationProvenances(sid: String, tid: String, rtype: String): RelModelProvenances

  override def toString: String = {
    val sb = new StringBuilder
    sb append ("------- Documents -------\n")
    sb append ("docIds:\t%d\t%s\n" format(docIds.size, docIds.take(10).mkString(",")))
    val sents = docIds.map(document(_).sents).flatten
    sb append ("sentences:\t%d\t%s\n" format(sents.size, sents.take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("------- Entities -------\n")
    sb append ("entIds:\t%d\t%s\n" format(entityIds.size, entityIds.take(10).mkString(",")))
    sb append ("headers:\t%s\n" format (entityIds.map(entityHeader(_)).take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("info:\t%s\n" format (entityIds.map(entityInfo(_)).take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("freebase:\t%s\n" format (entityIds.map(entityFreebase(_)).take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("types:\t%s\n" format (entityIds.map(entityTypePredictions(_).mkString(",")).take(10).toSet.mkString("\n\t", "\n\t", "...")))
    sb append ("typeProvenaces:\t%s\n" format (
      entityIds.map(id => entityTypePredictions(id).map(t => id -> t)).flatten.take(10).map(idt => entityTypeProvenances(idt._1, idt._2)).mkString("\n\t", "\n\t", "...")))
    sb append ("------- Relations -------\n")
    sb append ("relIds:\t%d\t%s\n" format(relationIds.size, relationIds.take(10).mkString(",")))
    sb append ("headers:\t%s\n" format (relationIds.map(p => relationHeader(p._1, p._2)).take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("freebase:\t%s\n" format (relationIds.map(p => relationFreebase(p._1, p._2)).take(4).mkString("\n\t", "\n\t", "...")))
    sb append ("relations:\t%s\n" format (relationIds.map(p => relationPredictions(p._1, p._2).mkString(",")).take(10).distinct.mkString("\n\t", "\n\t", "...")))
    sb append ("relProvenaces:\t%s\n" format (
      relationIds.map(id => relationPredictions(id._1, id._2).map(t => id -> t)).flatten.take(10).map(idt => relationProvenances(idt._1._1, idt._1._2, idt._2)).mkString("\n\t", "\n\t", "...")))
    sb.toString()
  }
}
