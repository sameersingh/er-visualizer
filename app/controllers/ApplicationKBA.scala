package controllers

import org.sameersingh.ervisualizer.kba.KBADB
import play.api._
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.{JsValue, Writes, Json}
import play.api.libs.functional.syntax._

object ApplicationKBA extends Controller {

  private var _db: KBADB = null

  def db = _db

  def init() {
    _db = KBADB.readDB
  }

  init();

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = Action {
    Ok(views.html.indexkba("UW TRECKBA - default"))
  }

  def entities = Action {
    Ok(Json.toJson(db.entities))
  }
 
  def documents(entityId: String) = Action {
    Ok(Json.toJson(db.documents(entityId)))
  }

  def clusterWordCloud(entityId: String, clusterId: Int, timestamp: Long) = Action {
    Ok(Json.toJson(db.clusterWordCloud(entityId, clusterId, timestamp)))
  }

  def documentWordCloud(entityId: String, timestamp: Long) = Action {
    Ok(Json.toJson(db.documentWordCloud(entityId, timestamp)))
  }

}