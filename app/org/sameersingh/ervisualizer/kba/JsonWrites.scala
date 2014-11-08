package org.sameersingh.ervisualizer.kba

import org.sameersingh.ervisualizer.kba._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by sameer on 7/20/14.
 */
object JsonWrites {
  implicit val seqStringPairWrites: Writes[Seq[(String, String)]] = new Writes[Seq[(String, String)]] {
    override def writes(o: Seq[(String, String)]): JsValue = {
      Json.toJson(o.map(p => Json.toJson(Seq(p._1, p._2))))
    }
  }
  val seqIntPairWrites: Writes[Seq[(Int, Int)]] = new Writes[Seq[(Int, Int)]] {
    override def writes(o: Seq[(Int, Int)]): JsValue = {
      Json.toJson(o.map(p => Json.toJson(Seq(p._1, p._2))))
    }
  }

  implicit val stalenessKbaWrites = Json.writes[StalenessKba]

  implicit val docKbaWrites: Writes[DocumentKba] = (
  (JsPath \ "streamid").write[String] and  
  (JsPath \ "timestamp").write[Long] and
  (JsPath \ "relevance").write[Int] and
  (JsPath \ "score").write[Int] and
  (JsPath \ "ci").write[Int] and
  (JsPath \ "lambdas").write[Seq[StalenessKba]]
  )(unlift(DocumentKba.unapply))

  implicit val entityKbaWrites = Json.writes[EntityKba]

  implicit val wordKbaWrites = Json.writes[WordKba]

}

object JsonReads {
  implicit val seqStringPairReads: Reads[Seq[(String, String)]] = new Reads[Seq[(String, String)]] {
    override def reads(json: JsValue): JsResult[Seq[(String, String)]] = {
      Json.fromJson[Seq[Seq[String]]](json).flatMap(seqs => JsSuccess(seqs.map(seq => seq(0) -> seq(1))))
    }
  }
  val seqIntPairReads: Reads[Seq[(Int, Int)]] = new Reads[Seq[(Int, Int)]] {
    override def reads(json: JsValue): JsResult[Seq[(Int, Int)]] = {
      Json.fromJson[Seq[Seq[Int]]](json).flatMap(seqs => JsSuccess(seqs.map(seq => seq(0) -> seq(1))))
    }
  }

  implicit val wordReads = Json.reads[Word]
  implicit val clusterReads = Json.reads[Cluster]
  implicit val stalenessReads = Json.reads[Staleness]
  implicit val documentReads = Json.reads[Doc]
  implicit val entityReads = Json.reads[Entity]

  implicit val stalenessKbaReads = Json.reads[StalenessKba]

  implicit val docKbaReads: Reads[DocumentKba] = (
  (JsPath \ "streamid").read[String] and  
  (JsPath \ "timestamp").read[Long] and
  (JsPath \ "relevance").read[Int] and
  (JsPath \ "score").read[Int] and
  (JsPath \ "ci").read[Int] and
  (JsPath \ "lambdas").read[Seq[StalenessKba]]
  )(DocumentKba.apply _)

  implicit val entityKbaReads = Json.reads[EntityKba]

  implicit val wordKbaReads = Json.reads[WordKba]

  implicit val clusterKbaReads: Reads[ClusterKba] = (
  (JsPath \ "cj").read[Int] and  
  (JsPath \ "cj_emb").read[Seq[WordKba]]
  )(ClusterKba.apply _)

  implicit val embeddingKbaReads: Reads[EmbeddingKba] = (
  (JsPath \ "streamid").read[String] and  
  (JsPath \ "timestamp").read[Long] and
  (JsPath \ "di").read[Seq[WordKba]] and
  (JsPath \ "clusters").read[Seq[ClusterKba]]
  )(EmbeddingKba.apply _)

}
