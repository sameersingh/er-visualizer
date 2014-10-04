package org.sameersingh.ervisualizer.data

/**
 * @author nacho
 */

case class StalenessKba(cj: Int, inc: Double, dec: Double)

case class DocumentKba(id: String, timestamp: Long, relevance: Int, score: Int, ci: Int, lambdas: Seq[StalenessKba] = Seq.empty)

case class EntityKba(id: String, name: String, documents : Seq[DocumentKba] = Seq.empty)

