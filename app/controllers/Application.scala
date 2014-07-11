package controllers

import play.api._
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.{JsValue, Writes, Json}
import play.api.libs.functional.syntax._
import org.sameersingh.ervisualizer.data.EntityHeader
import org.sameersingh.ervisualizer.data.EntityInfo

object Application extends Controller {

  private var _db: DB = null

  def db = _db

  def init() {
    _db = InMemoryDB.readFromTSV("public/data/test/")
    println(db)
  }

  init();

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

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def document(docId: String) = Action {
    println("doc: " + docId)
    Ok(Json.prettyPrint(Json.toJson(db.document(docId))))
  }

  def sentence(docId: String, sid: Int) = Action {
    println("sen: " + docId + ", " + sid)
    Ok(Json.toJson(db.document(docId).sents(sid)))
  }

  def entityHeaders = Action {
    println("Entity Headers")
    Ok(Json.toJson(db.entityIds.map(id => db.entityHeader(id))))
  }

  def entityInfo(id: String) = Action {
    println("eInfo: " + id)
    Ok(Json.toJson(db.entityInfo(id)))
  }

  def entityFreebase(id: String) = Action {
    println("eFb: " + id)
    Ok(Json.toJson(db.entityFreebase(id)))
  }

  def entityText(id: String) = Action {
    println("eTxt: " + id)
    Ok(Json.toJson(db.entityText(id)))
  }

  def entityTypes(id: String) = Action {
    println("eT: " + id + ": " + db.entityTypePredictions(id).mkString(", "))
    Ok(Json.toJson(db.entityTypePredictions(id)))
  }

  def entityTypeProv(id: String, etype: String) = Action {
    println("eTP: " + id + ", " + etype)
    Ok(Json.toJson(db.entityTypeProvenances(id, etype)))
  }

  def relationHeaders = Action {
    println("Relation Headers")
    Ok(Json.toJson(db.relationIds.map(id => db.relationHeader(id._1, id._2))))
  }

  def relationFreebase(sid: String, tid: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationFreebase(sid, tid)))
  }

  def relationText(sid: String, tid: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationText(sid, tid)))
  }

  def relationPredictions(sid: String, tid: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationPredictions(sid, tid)))
  }

  def relationProvenances(sid: String, tid: String, rtype: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationProvenances(sid, tid, rtype)))
  }


}