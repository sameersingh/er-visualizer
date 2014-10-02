package org.sameersingh.ervisualizer.data

import scala.collection.mutable.HashMap
import com.typesafe.config.ConfigFactory
import scala.collection.mutable
import play.api.libs.json.Json

import java.io.File

/**
 * Created by nacho.
 */
class KBADB {

  val _documentsPerEntity = new HashMap[String, List[DocumentKba]]

  def documents(entityId: String): List[DocumentKba] = _documentsPerEntity(entityId)  

  def readDB: KBADB = {
    // read json files
    println("Reading json files for KBA")
    val cfg = ConfigFactory.load()
    val stalenessBaseDir = cfg.getString("nlp.kba.stalenessBaseDir")
    val stalenessInputFiles = new File(stalenessBaseDir).listFiles();
    val db = new KBADB
    for (file <- stalenessInputFiles) {
      if (!file.isDirectory()) {
        // TODO how to do this more efficiently? ask Sameer
        val sf = io.Source.fromFile(file, "UTF-8").getLines.mkString
        import JsonReads._
        val docArray = Json.fromJson[List[DocumentKba]](Json.parse(sf)).get
        db._documentsPerEntity.put("test", docArray)
      }
    }
    db
  }
}

object KBADB extends KBADB