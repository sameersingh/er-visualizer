package controllers

import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.Logging
import org.sameersingh.ervisualizer.kba.{EntityKBAReader, KBAStore}
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.Json

import scala.collection.mutable

object Application extends Controller with Logging {

  val config = ConfigFactory.load()
  val defaultDBName = config.getString("nlp.data.defaultDB")

  val _docs = new DocumentStore()
  val _dbStore = new DBStore(_docs)

  private val _db: mutable.Map[String, DB] = new mutable.HashMap[String, DB]
  private var _entKBA: KBAStore = null

  def db(id: String) = _dbStore.id(id)
  def dbQueryId(query: String) = _dbStore.query(query)._1

  def entKBA = _entKBA

  def init() {
    _entKBA = EntityKBAReader.read()
    val docDir = config.getString("nlp.data.baseDir")
    val docsFile = config.getString("nlp.data.docsFile")
    DocumentStore.readDocs(_docs, docDir, docsFile)
  }

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = search // reset(defaultDBName)

  def search = Action {
    Ok(views.html.search("UW Visualizer"))
  }

  def page(query: Option[String]) = Action {
    logger.info("Query : \"" + query.get + "\"")
    if(_docs.numDocs == 0) init()
    val (dbId, _) = _dbStore.query(query.getOrElse(""))
    logger.info("  Id  : " + dbId)
    Ok(views.html.main("UW Visualizer - " + query.get, dbId))
  }

  def pageId(dbId: String) = Action {
    if(_docs.numDocs == 0) init()
    val query = _dbStore.queryIdMap(dbId)
    logger.info("Request Id  : " + dbId + ", saved query: \"" + query + "\"")
    Ok(views.html.main("UW Visualizer - " + query, dbId))
  }

  def entityKBA(id: String) = Action {
    println("eKBA: " + id)
    Ok(Json.toJson(entKBA.entityKBA(id)))
  }

  def relationKBA(sid: String, tid: String) = Action {
    println("rKBA: " + sid -> tid)
    Ok(Json.toJson(entKBA.relationKBA(sid, tid)))
  }

  def document(docId: String, dbId: Option[String]) = Action {
    println("doc: " + docId)
    //SeeOther("http://allafrica.com/stories/%s.html?viewall=1" format(docId.take(12)))
    Ok(Json.prettyPrint(Json.toJson(db(dbId.get).document(docId))))
  }

  def sentence(docId: String, sid: Int, dbName: Option[String]) = Action {
    // println("sen: " + docId + ", " + sid)
    Ok(Json.toJson(db(dbName.get).document(docId).sents(sid)))
  }

  def entityHeaders(dbName: Option[String]) = Action {
    println("Entity Headers: " + dbName.get)
    Ok(Json.toJson(db(dbName.get).entityIds.map(id => db(dbName.get).entityHeader(id)).toSeq))
  }

  def entityInfo(id: String, dbName: Option[String]) = Action {
    println("eInfo: " + id)
    Ok(Json.toJson(db(dbName.get).entityInfo(id)))
  }

  def entityFreebase(id: String, dbName: Option[String]) = Action {
    println("eFb: " + id)
    Ok(Json.toJson(db(dbName.get).entityFreebase(id)))
  }

  def entityText(id: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("eTxt: " + id)
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(EntityText(id, db(dbName.get).entityText(id).provenances.take(limit.get))))
    else Ok (Json.toJson(db(dbName.get).entityText(id)))
  }

  def entityProvs(id: String, dbName: Option[String]) = Action {
    println("eTxt: " + id)
    Ok(views.html.provs("Entity " + id, Seq(id), dbName.get))
    //Ok(Json.toJson(db(dbName).entityText(id)))
  }

  def entityRelations(id: String, dbName: Option[String]) = Action {
    println("eRels: " + id)
    Ok(Json.toJson(db(dbName.get).relations(id)))
  }

  def entityTypes(id: String, dbName: Option[String]) = Action {
    println("eT: " + id + ": " + db(dbName.get).entityTypePredictions(id).mkString(", "))
    Ok(Json.toJson(db(dbName.get).entityTypePredictions(id)))
  }

  def entityTypeProv(id: String, etype: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("eTP: " + id + ", " + etype)
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(TypeModelProvenances(id, etype, db(dbName.get).entityTypeProvenances(id, etype).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName.get).entityTypeProvenances(id, etype)))
  }

  def relationHeaders(dbName: Option[String]) = Action {
    println("Relation Headers: " + dbName.get)
    Ok(Json.toJson(db(dbName.get).relationIds.map(id => db(dbName.get).relationHeader(id._1, id._2)).toSeq))
  }

  def relationFreebase(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db(dbName.get).relationFreebase(sid, tid)))
  }

  def relationText(sid: String, tid: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("RelText: " + (sid -> tid))
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(RelationText(sid, tid, db(dbName.get).relationText(sid, tid).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName.get).relationText(sid, tid)))
  }

  def relationProvs(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelText: " + (sid -> tid))
    Ok(views.html.provs("Relation: %s -> %s ".format(sid, tid), Seq(sid, tid), dbName.get))
    //Ok(Json.toJson(db(dbName).relationText(sid, tid)))
  }

  def relationPredictions(sid: String, tid: String, dbName: Option[String]) = Action {
    println("RelPred: " + (sid -> tid))
    Ok(Json.toJson(db(dbName.get).relationPredictions(sid, tid)))
  }

  def relationProvenances(sid: String, tid: String, rtype: String, dbName: Option[String], limit: Option[Int]) = Action {
    println("RelProv: " + (sid -> tid))
    if (limit.isDefined && limit.get > 0)
      Ok(Json.toJson(RelModelProvenances(sid, tid, rtype, db(dbName.get).relationProvenances(sid, tid, rtype).provenances.take(limit.get))))
    else Ok(Json.toJson(db(dbName.get).relationProvenances(sid, tid, rtype)))
  }


}