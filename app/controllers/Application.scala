package controllers

import org.sameersingh.ervisualizer.kba.{EntityKBAReader, KBAStore}
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.Json

import scala.collection.mutable

object Application extends Controller {

  val defaultDBName = "drug"
  val numProvenances = 10
  private val _db: mutable.Map[String,DB] = new mutable.HashMap[String,DB]
  private var _entKBA: KBAStore = null

  def db(name: Option[String] = None) = _db.getOrElseUpdate(name.getOrElse(defaultDBName), {
    val result = EntityInfoReader.read()
    NLPReader.read(result, Some(name.getOrElse(defaultDBName)))
    result
  })

  def db: DB = db(None)

  def entKBA = _entKBA

  def init() {
    _entKBA = EntityKBAReader.read()
    println(db)
  }

  init()

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = reset(defaultDBName)

  def reset(name: String) = Action {
    db(Some(name))
    Ok(views.html.index("UW - " + name, name))
  }

  def entityKBA(id: String) = Action {
    println("eKBA: " + id)
    Ok(Json.toJson(entKBA.entityKBA(id)))
  }

  def relationKBA(sid: String, tid: String) = Action {
    println("rKBA: " + sid -> tid)
    Ok(Json.toJson(entKBA.relationKBA(sid, tid)))
  }

  def document(docId: String, dbName: Option[String]) = Action {
    println("doc: " + docId)
    //SeeOther("http://allafrica.com/stories/%s.html?viewall=1" format(docId.take(12)))
    Ok(Json.prettyPrint(Json.toJson(db(dbName).document(docId))))
  }

  def sentence(docId: String, sid: Int, dbName: Option[String]) = Action {
    // println("sen: " + docId + ", " + sid)
    Ok(Json.toJson(db(dbName).document(docId).sents(sid)))
  }

  def entityHeaders(dbName: Option[String]) = Action {
    println("Entity Headers")
    // Ok(Json.toJson(db(dbName).entityIds.filter(id => db(dbName).relations(id).size>0).map(id => db(dbName).entityHeader(id))))
    Ok(Json.toJson(db(dbName).relevantEntityIds.map(id => db(dbName).entityHeader(id)).toSeq))
  }

  def entityInfo(id: String, dbName: Option[String]) = Action {
    println("eInfo: " + id)
    Ok(Json.toJson(db(dbName).entityInfo(id)))
  }

  def entityFreebase(id: String, dbName: Option[String]) = Action {
    println("eFb: " + id)
    Ok(Json.toJson(db(dbName).entityFreebase(id)))
  }

  def entityText(id: String, dbName: Option[String]) = Action {
    println("eTxt: " + id)
    //Ok(Json.toJson(db(dbName).entityText(id)))
    Ok(Json.toJson(EntityText(id, db(dbName).entityText(id).provenances.take(numProvenances))))
  }

  def entityProvs(id: String, dbName: Option[String]) = Action {
    println("eTxt: " + id)
    Ok(Json.toJson(db(dbName).entityText(id)))
  }

  def entityRelations(id: String, dbName: Option[String]) = Action {
    println("eRels: " + id)
    Ok(Json.toJson(db(dbName).relations(id)))
  }

  def entityTypes(id: String, dbName: Option[String]) = Action {
    println("eT: " + id + ": " + db(dbName).entityTypePredictions(id).mkString(", "))
    Ok(Json.toJson(db(dbName).entityTypePredictions(id)))
  }

  def entityTypeProv(id: String, etype: String, dbName: Option[String]) = Action {
    println("eTP: " + id + ", " + etype)
    Ok(Json.toJson(db(dbName).entityTypeProvenances(id, etype)))
    Ok(Json.toJson(TypeModelProvenances(id, etype, db(dbName).entityTypeProvenances(id, etype).provenances.take(numProvenances))))
  }

  def relationHeaders(dbName: Option[String]) = Action {
    println("Relation Headers")
    Ok(Json.toJson(db(dbName).relevantRelationIds.map(id => db(dbName).relationHeader(id._1, id._2)).toSeq))
  }

  def relationFreebase(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db(dbName).relationFreebase(sid, tid)))
  }

  def relationText(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelText: " + (sid -> tid))
    Ok(Json.toJson(RelationText(sid, tid, db(dbName).relationText(sid, tid).provenances.take(numProvenances))))
  }

  def relationProvs(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelText: " + (sid -> tid))
    Ok(Json.toJson(db(dbName).relationText(sid, tid)))
  }

  def relationPredictions(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelPred: " + (sid -> tid))
    Ok(Json.toJson(db(dbName).relationPredictions(sid, tid)))
  }

  def relationProvenances(sid: String, tid: String, rtype: String, dbName: Option[String]) = Action {
    println("RelProv: " + (sid -> tid))
    Ok(Json.toJson(RelModelProvenances(sid, tid, rtype, db(dbName).relationProvenances(sid, tid, rtype).provenances.take(numProvenances))))
  }


}