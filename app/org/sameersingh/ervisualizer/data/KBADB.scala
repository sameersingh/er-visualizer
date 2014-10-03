package org.sameersingh.ervisualizer.data

import scala.collection.mutable.{HashMap, ArrayBuffer}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable
import play.api.libs.json.Json

import java.io.File

/**
 * Created by nacho.
 */
class KBADB {

  val _entities = new ArrayBuffer[EntityKba]
  val _documentsPerEntity = new HashMap[String, List[DocumentKba]]

  def documents(entityId: String): Seq[DocumentKba] = _documentsPerEntity(entityId)
  def entities : Seq[EntityKba] = _entities

  def readDB: KBADB = {
    // read json files
    println("reading files for KBA")
    val db = new KBADB
    
    val cfg = ConfigFactory.load()
    
    // read entities files
    val entitiesFileName = cfg.getString("nlp.kba.entitiesFile")
    val entitiesFile = io.Source.fromFile(entitiesFileName, "UTF-8")
    for (line <- entitiesFile.getLines()) {
      val split = line.split("\\t")
      db._entities += EntityKba(split(0).trim(), split(1).trim())
    }
    entitiesFile.close()

    // read staleness files
    val stalenessBaseDir = cfg.getString("nlp.kba.stalenessBaseDir")
    val stalenessInputFiles = new File(stalenessBaseDir).listFiles();
    for (file <- stalenessInputFiles) {
      if (!file.isDirectory()) {
        val sf = io.Source.fromFile(file, "UTF-8")
        // TODO how to do this more efficiently? ask Sameer
        val sfAsString = sf.getLines.mkString
        import JsonReads._
        val docArray = Json.fromJson[List[DocumentKba]](Json.parse(sfAsString)).get
        val entityName = file.getName().replace(".json", "")
        db._documentsPerEntity.put(entityName, docArray)
        sf.close()
      }
    }
    db
  }
}

object KBADB extends KBADB