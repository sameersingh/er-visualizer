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
    // _db = InMemoryDB.readFromTSV("public/data/test/")
    _db = D2DDB.readDB()
    println(db)
  }

  init();

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = Action {
    Ok(views.html.index("UW - default"))
  }

  def reset(name: String) = Action {
    _db = D2DDB.readDB(Some(name))
    Ok(views.html.index("UW - " + name))
  }

  def document(docId: String) = Action {
    println("doc: " + docId)
    SeeOther("http://allafrica.com/stories/%s.html?viewall=1" format(docId.take(12)))
    //Ok(Json.prettyPrint(Json.toJson(db.document(docId))))
  }

  def sentence(docId: String, sid: Int) = Action {
    // println("sen: " + docId + ", " + sid)
    Ok(Json.toJson(db.document(docId).sents(sid)))
  }

  def entityHeaders = Action {
    println("Entity Headers")
    Ok(Json.toJson(db.entityIds.filter(id => db.relations(id).size>0).map(id => db.entityHeader(id))))
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

  def entityRelations(id: String) = Action {
    println("eRels: " + id)
    Ok(Json.toJson(db.relations(id)))
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