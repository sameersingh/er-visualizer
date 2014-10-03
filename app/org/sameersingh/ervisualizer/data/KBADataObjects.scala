package org.sameersingh.ervisualizer.data

/**
 * @author nacho
 */

case class StalenessKba(inc: Double, dec: Double)

case class DocumentKba(id: String, timestamp: Long, relevance: Int, score: Int, li: StalenessKba, lijs: Seq[StalenessKba] = Seq.empty)

case class EntityKba(id: String, name: String, documents : Seq[DocumentKba] = Seq.empty)

