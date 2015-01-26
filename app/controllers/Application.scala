package controllers

import org.sameersingh.ervisualizer.kba.{EntityKBAReader, KBAStore}
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.Json

import scala.collection.mutable

object Application extends Controller {

  val defaultDBName = "drug"

  val _docs = new DocumentStore()
  val _dbStore = new DBStore(_docs)

  private val _db: mutable.Map[String, DB] = new mutable.HashMap[String, DB]
  private var _entKBA: KBAStore = null

  def dbId(id: String) = _dbStore.id(id)
  def dbQueryId(query: String) = _dbStore.query(query)._1

  def db(name: Option[String] = None) = _db.getOrElseUpdate(name.getOrElse(throw new Error("None as dbName")), {
    val result = EntityInfoReader.read()
    //NLPReader.read(result, Some(name.getOrElse(defaultDBName)))
    result
  })

  def entKBA = _entKBA

  def init() {
    _entKBA = EntityKBAReader.read()
    // TODO: initialize _docs
  }

  init()

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = search // reset(defaultDBName)

  def search = Action {
    Ok(views.html.search("UW Visualizer"))
  }

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

  def entityText(id: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("eTxt: " + id)
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(EntityText(id, db(dbName).entityText(id).provenances.take(limit.get))))
    else Ok (Json.toJson(db(dbName).entityText(id)))
  }

  def entityProvs(id: String, dbName: Option[String]) = Action {
    println("eTxt: " + id)
    Ok(views.html.provs("Entity " + id, Seq(id), dbName.getOrElse(defaultDBName)))
    //Ok(Json.toJson(db(dbName).entityText(id)))
  }

  def entityRelations(id: String, dbName: Option[String]) = Action {
    println("eRels: " + id)
    Ok(Json.toJson(db(dbName).relations(id)))
  }

  def entityTypes(id: String, dbName: Option[String]) = Action {
    println("eT: " + id + ": " + db(dbName).entityTypePredictions(id).mkString(", "))
    Ok(Json.toJson(db(dbName).entityTypePredictions(id)))
  }

  def entityTypeProv(id: String, etype: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("eTP: " + id + ", " + etype)
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(TypeModelProvenances(id, etype, db(dbName).entityTypeProvenances(id, etype).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName).entityTypeProvenances(id, etype)))
  }

  def relationHeaders(dbName: Option[String]) = Action {
    println("Relation Headers")
    Ok(Json.toJson(db(dbName).relevantRelationIds.map(id => db(dbName).relationHeader(id._1, id._2)).toSeq))
  }

  def relationFreebase(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db(dbName).relationFreebase(sid, tid)))
  }

  def relationText(sid: String, tid: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("RelText: " + (sid -> tid))
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(RelationText(sid, tid, db(dbName).relationText(sid, tid).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName).relationText(sid, tid)))
  }

  def relationProvs(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelText: " + (sid -> tid))
    Ok(views.html.provs("Relation: %s -> %s ".format(sid, tid), Seq(sid, tid), dbName.getOrElse(defaultDBName)))
    //Ok(Json.toJson(db(dbName).relationText(sid, tid)))
  }

  def relationPredictions(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelPred: " + (sid -> tid))
    Ok(Json.toJson(db(dbName).relationPredictions(sid, tid)))
  }

  def relationProvenances(sid: String, tid: String, rtype: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("RelProv: " + (sid -> tid))
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(RelModelProvenances(sid, tid, rtype, db(dbName).relationProvenances(sid, tid, rtype).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName).relationProvenances(sid, tid, rtype)))
  }


}