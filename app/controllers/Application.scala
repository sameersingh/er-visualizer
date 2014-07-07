package controllers

import play.api._
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.Json
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

  implicit val entityHeaderWrites = Json.writes[EntityHeader]
  implicit val entityInfoWrites = Json.writes[EntityInfo]
  implicit val relationHeaderWrites = Json.writes[RelationHeader]
  implicit val relationFreebaseoWrites = Json.writes[RelationFreebase]

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def entityHeaders = Action {
    println("Entity Headers")
    Ok(Json.toJson(db.entityIds.map(id => db.entityHeader(id))))
  }

  def entityInfo(id: String) = Action {
    println("Info: " + id)
    Ok(Json.toJson(db.entityInfo(id)))
  }

  def relationHeaders = Action {
    println("Relation Headers")
    Ok(Json.toJson(db.relationIds.map(id => db.relationHeader(id._1, id._2))))
  }

  def relationFreebase(sid: String, tid: String) = Action {
    println("RelFreebase: " + (sid -> tid))
    Ok(Json.toJson(db.relationFreebase(sid, tid)))
  }

}