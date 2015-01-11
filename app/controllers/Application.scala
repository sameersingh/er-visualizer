package controllers

import org.sameersingh.ervisualizer.kba.{EntityKBAReader, KBAStore}
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.Json

import scala.collection.mutable

object Application extends Controller {

  private val _db: mutable.Map[String,DB] = new mutable.HashMap[String,DB]
  private var _entKBA: KBAStore = null

  def db(name: String = "drug") = _db.getOrElseUpdate(name, {
    val result = EntityInfoReader.read()
    NLPReader.read(result, Some(name))
    result
  })

  def db: DB = db("drug")

  def entKBA = _entKBA

  def init() {
    _entKBA = EntityKBAReader.read()
    println(db)
  }

  init()

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = Action {
    Ok(views.html.index("UW - default"))
  }

  def reset(name: String) = Action {
    db(name)
    Ok(views.html.index("UW - " + name))
  }

  def document(docId: String) = Action {
    println("doc: " + docId)
    //SeeOther("http://allafrica.com/stories/%s.html?viewall=1" format(docId.take(12)))
    Ok(Json.prettyPrint(Json.toJson(db.document(docId))))
  }

  def sentence(docId: String, sid: Int) = Action {
    // println("sen: " + docId + ", " + sid)
    Ok(Json.toJson(db.document(docId).sents(sid)))
  }

  def entityHeaders = Action {
    println("Entity Headers")
    // Ok(Json.toJson(db.entityIds.filter(id => db.relations(id).size>0).map(id => db.entityHeader(id))))
    Ok(Json.toJson(db.relevantEntityIds.map(id => db.entityHeader(id)).toSeq))
  }

  def entityInfo(id: String) = Action {
    println("eInfo: " + id)
    Ok(Json.toJson(db.entityInfo(id)))
  }

  def entityKBA(id: String) = Action {
    println("eKBA: " + id)
    Ok(Json.toJson(entKBA.entityKBA(id)))
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
    Ok(Json.toJson(db.relevantRelationIds.map(id => db.relationHeader(id._1, id._2)).toSeq))
  }

  def relationKBA(sid: String, tid: String) = Action {
    println("rKBA: " + sid -> tid)
    Ok(Json.toJson(entKBA.relationKBA(sid, tid)))
  }

  def relationFreebase(sid: String, tid: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationFreebase(sid, tid)))
  }

  def relationText(sid: String, tid: String) = Action {
    println("RelText: " + (sid -> tid))
    Ok(Json.toJson(db.relationText(sid, tid)))
  }

  def relationPredictions(sid: String, tid: String) = Action {
    println("RelPred: " + (sid -> tid))
    Ok(Json.toJson(db.relationPredictions(sid, tid)))
  }

  def relationProvenances(sid: String, tid: String, rtype: String) = Action {
    println("RelProv: " + (sid -> tid))
    Ok(Json.toJson(db.relationProvenances(sid, tid, rtype)))
  }


}