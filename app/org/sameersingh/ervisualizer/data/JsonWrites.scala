package org.sameersingh.ervisualizer.data

import play.api.libs.json._
import org.sameersingh.ervisualizer.data.RelModelProvenances
import org.sameersingh.ervisualizer.data.RelationFreebase
import org.sameersingh.ervisualizer.data.Provenance
import org.sameersingh.ervisualizer.data.EntityHeader
import org.sameersingh.ervisualizer.data.Sentence
import org.sameersingh.ervisualizer.data.EntityText
import org.sameersingh.ervisualizer.data.EntityFreebase
import org.sameersingh.ervisualizer.data.TypeModelProvenances
import org.sameersingh.ervisualizer.data.EntityInfo
import org.sameersingh.ervisualizer.data.Document
import org.sameersingh.ervisualizer.data.RelationHeader
import org.sameersingh.ervisualizer.data.RelationText

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
  implicit val provWrites = {
    implicit val seqIntPairWritesImplicit = seqIntPairWrites
    Json.writes[Provenance]
  }
  implicit val senWrites = Json.writes[Sentence]
  implicit val docWrites = Json.writes[Document]

  implicit val entityHeaderWrites = Json.writes[EntityHeader]
  implicit val entityInfoWrites = Json.writes[EntityInfo]
  implicit val entityFbWrites = Json.writes[EntityFreebase]
  implicit val entityTxtWrites = Json.writes[EntityText]
  implicit val entityTypeProvWrites = Json.writes[TypeModelProvenances]

  implicit val relationHeaderWrites = Json.writes[RelationHeader]
  implicit val relationFreebaseWrites = Json.writes[RelationFreebase]
  implicit val relationTextWrites = Json.writes[RelationText]
  implicit val relationProvWrites = Json.writes[RelModelProvenances]

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
  implicit val provReads = {
    implicit val seqIntPairReadsImplicit = seqIntPairReads
    Json.reads[Provenance]
  }
  implicit val senReads = Json.reads[Sentence]
  implicit val docReads = Json.reads[Document]

  implicit val entityHeaderReads = Json.reads[EntityHeader]
  implicit val entityInfoReads = Json.reads[EntityInfo]
  implicit val entityFbReads = Json.reads[EntityFreebase]
  implicit val entityTxtReads = Json.reads[EntityText]
  implicit val entityTypeProvReads = Json.reads[TypeModelProvenances]

  implicit val relationHeaderReads = Json.reads[RelationHeader]
  implicit val relationFreebaseReads = Json.reads[RelationFreebase]
  implicit val relationTextReads = Json.reads[RelationText]
  implicit val relationProvReads = Json.reads[RelModelProvenances]

}
