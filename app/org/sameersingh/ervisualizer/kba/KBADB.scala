package org.sameersingh.ervisualizer.kba

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import scala.collection.mutable.{ArrayBuffer, HashMap}

import JsonReads._

/**
 * Created by nacho.
 */
class KBADB {

  val _entities = new ArrayBuffer[EntityKba]
  val _documentsPerEntity = new HashMap[String, Seq[DocumentKba]]
  val _wordCloudPerEntityPerCluster = new HashMap[String, HashMap[(Int, Long), Seq[WordKba]]]
  val _wordCloudPerEntity = new HashMap[String, HashMap[Long, Seq[WordKba]]]

  def documents(entityId: String): Seq[DocumentKba] = _documentsPerEntity(entityId)
  def entities : Seq[EntityKba] = _entities
  def clusterWordCloud(entityId: String, clusterId: Int, timestamp: Long): Seq[WordKba] = _wordCloudPerEntityPerCluster(entityId)((clusterId, timestamp))
  def documentWordCloud(entityId: String, timestamp: Long): Seq[WordKba] = _wordCloudPerEntity(entityId)(timestamp)

  def readDB: KBADB = {
    // read json files
    println("reading files for KBA")
    val db = new KBADB
    
    val cfg = ConfigFactory.load()
    
    // read entities files
    println("reading entities file")
    val entitiesFileName = cfg.getString("nlp.kba.entitiesFile")
    val entitiesFile = io.Source.fromFile(entitiesFileName, "UTF-8")
    for (line <- entitiesFile.getLines()) {
      val split = line.split("\\t")
      //println(split)
      db._entities += EntityKba(split(0).trim(), split(1).trim())
    }
    entitiesFile.close()
    println("read entities file")

    // read staleness files
    val stalenessBaseDir = cfg.getString("nlp.kba.stalenessBaseDir")
    val stalenessInputFiles = new File(stalenessBaseDir).listFiles();
    println("reading staleness files")
    for (file <- stalenessInputFiles) {
      if (!file.isDirectory()) {
        println("reading file " + file.getName())
        val sf = io.Source.fromFile(file, "UTF-8")
        // for(l <- sf.getLines.filter(l => random.nextDouble > 0.1))
        val docArray = new ArrayBuffer[DocumentKba]
        for (l <- sf.getLines) {
          val doc = Json.fromJson[DocumentKba](Json.parse(l)).get
          docArray += doc
        }
        val entityName = file.getName().replace(".json", "")
        db._documentsPerEntity.put(entityName, docArray)
        sf.close()
        println("read file " + file.getName())
      }
    }
    println("read staleness files")


    // read embedding files
    val embeddingBaseDir = cfg.getString("nlp.kba.embeddingBaseDir")
    val embeddingInputFiles = new File(embeddingBaseDir).listFiles();
    println("reading embedding files")
    for (file <- embeddingInputFiles) {
      if (!file.isDirectory()) {
        println("reading file " + file.getName())
        val sf = io.Source.fromFile(file, "UTF-8")
        val embeddings = new ArrayBuffer[EmbeddingKba]
        for (l <- sf.getLines) {
          val embedding = Json.fromJson[EmbeddingKba](Json.parse(l)).get
          embeddings += embedding
        }
        val entityName = file.getName().replace(".json", "")
        val wordCloudPerEntity = new HashMap[Long, Seq[WordKba]]
        val wordCloudPerEntityPerCluster = new HashMap[(Int, Long), Seq[WordKba]]
        for (embedding <- embeddings) {
          wordCloudPerEntity.put(embedding.timestamp, embedding.di)
          for (cluster <- embedding.clusters) {
            wordCloudPerEntityPerCluster.put((cluster.cj, embedding.timestamp), cluster.cj_emb)            
          }
        }
        db._wordCloudPerEntity.put(entityName, wordCloudPerEntity)
        db._wordCloudPerEntityPerCluster.put(entityName, wordCloudPerEntityPerCluster)
        sf.close()
        println("read file " + file.getName())
      }
    }
    println("read embedding files")
    db
  }
}

object KBADB extends KBADB