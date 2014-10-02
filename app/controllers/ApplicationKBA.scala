package controllers

import play.api._
import play.api.mvc._
import org.sameersingh.ervisualizer.data._
import play.api.libs.json.{JsValue, Writes, Json}
import play.api.libs.functional.syntax._

object ApplicationKBA extends Controller {

  private var _db: DB = null

  def db = _db

  def init() {
    // _db = InMemoryDB.readFromTSV("public/data/test/")
    //_db = D2DDB.readDB()
  }

  init();

  import org.sameersingh.ervisualizer.data.JsonWrites._

  def index = Action {
    Ok(views.html.indexkba("UW TRECKBA - default"))
  }

  def reset(name: String) = Action {
    //_db = D2DDB.readDB(Some(name))
    Ok(views.html.index("UW TRECKBA - " + name))
  }

  def document() = Action {
    Ok("successful received call")
  }

}