package org.sameersingh.ervisualizer.data

import play.api.libs.json.{Json, JsValue, Writes}

/**
 * Created by sameer on 7/20/14.
 */
object JsonWrites {
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
