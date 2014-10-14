package org.sameersingh.ervisualizer.data

/**
 * @author nacho
 */

case class StalenessKba(cj: Int, inc: Double, dec: Double)

case class DocumentKba(id: String, timestamp: Long, relevance: Int, score: Int, ci: Int, lambdas: Seq[StalenessKba] = Seq.empty)

case class EntityKba(id: String, name: String, documents : Seq[DocumentKba] = Seq.empty)

case class ClusterKba(cj: Int, cj_emb: Seq[WordKba])

case class EmbeddingKba(id: String, timestamp: Long, di: Seq[WordKba], clusters: Seq[ClusterKba])

case class WordKba(t: String, p: Int)
