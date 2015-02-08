package org.sameersingh.ervisualizer.data

import java.io.PrintWriter

import com.typesafe.scalalogging.slf4j.Logging
import nlp_serde.FileUtil
import nlp_serde.readers.PerLineJsonReader

import scala.collection.mutable
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

/**
 * @author sameer
 * @since 1/25/15.
 */
class DocumentStore {
  type Id = String
  val docMap = new HashMap[Id, nlp_serde.Document]
  val keywordsMap = new HashMap[String, HashSet[Id]]
  val topicsMap = new HashMap[String, HashSet[Id]]
  val entitiesMap = new HashMap[String, HashSet[Id]]

  val keywords = new HashSet[String]

  def +=(d: nlp_serde.Document): nlp_serde.Document = {
    docMap.getOrElseUpdate(d.id, d)
  }

  def apply(id: Id) = docMap(id)

  def get(id: Id) = docMap.get(id)

  def addKeywords(d: nlp_serde.Document): Unit = {
    this += d
    for (s <- d.sentences; t <- s.tokens; lemma <- t.lemma; if (keywords(lemma))) {
      keywordsMap.getOrElseUpdate(lemma.toLowerCase, new HashSet[Id]) += d.id
    }
  }

  def addEntities(doc: nlp_serde.Document): Unit = {
    this += doc
    for (e <- doc.entities; if (!e.freebaseIds.isEmpty)) {
      entitiesMap.getOrElseUpdate(e.representativeString.toLowerCase, new HashSet[Id]) += doc.id
    }
  }

  def addTopic(d: nlp_serde.Document, word: String): Unit = {
    this += d
    topicsMap.getOrElseUpdate(word.toLowerCase, new HashSet[Id]) += d.id
  }

  def query(queryString: String): Iterable[Id] = {
    var results: HashSet[Id] = null
    for(q <- queryString.split("\\s")) {
      val ids = if(q.startsWith("topic:")) {
        topicsMap.getOrElse(q.drop(6), Set.empty)
      } else if(q.startsWith("ent:")) {
        entitiesMap.getOrElse(q.drop(4).replaceAll("_", " "), Set.empty)
      } else {
        keywordsMap.getOrElse(q, Set.empty)
      }
      if(results == null) {
        results = new mutable.HashSet[Id]()
        results ++= ids
      } else {
        results --= ids
      }
    }
    if(results == null) {
      results = new mutable.HashSet[Id]()
    }
    results
  }
}

object DocumentStore extends Logging {
  def readTopics(dir: String, typ: String): Map[String, Int] = {
    val result = new mutable.HashMap[String, Int]()
    val file = dir + "iai/" + typ + "/id_topics.tsv"
    for(l <- io.Source.fromFile(file).getLines()) {
      val split = l.split("\t")
      assert(split.length == 2)
      result(split(0)) = split(1).toInt
    }
    result.toMap
  }

  def readDocs(store: DocumentStore, dir: String = "data/d2d/"): Unit = {
    logger.info("Reading title topics")
    val titleTopics = Map.empty[String, Int] // readTopics(dir, "title")
    logger.info("Reading content topics")
    val contentTopics = readTopics(dir, "content")
    logger.info("Reading documents")
    val docsFile = dir +"docs.nlp.flr.json.gz"
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    for(doc <- new PerLineJsonReader().read(docsFile)) {
      store += doc
      // add topics
      titleTopics.get(doc.id).foreach(t => store.addTopic(doc, "title" + t))
      contentTopics.get(doc.id).foreach(t => store.addTopic(doc, "content" + t))
      // add entities
      store.addEntities(doc)
      // add words
      store.addKeywords(doc)
      docIdx += 1
      if(docIdx % dotEvery == 0) print(".")
      if(docIdx % lineEvery == 0) println(": read " + docIdx + " docs, " + store.keywords.size + " words, "
        + store.topicsMap.size + " topics, " + store.entitiesMap.size + " entities.")
    }
  }
}

object WordCounts {
  def main(args: Array[String]): Unit = {
    val docsFile = "data/d2d/docs.nlp.flr.json.gz"
    val wcounts = new mutable.HashMap[String, Int]()
    val ecounts = new mutable.HashMap[String, Int]()
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    for(d <- new PerLineJsonReader().read(docsFile)) {
      for (s <- d.sentences; t <- s.tokens; lemma <- t.lemma; key = lemma.toLowerCase) {
        wcounts(key) = 1 + wcounts.getOrElse(key, 0)
      }
      for (e <- d.entities; if (!e.freebaseIds.isEmpty); key = e.representativeString.toLowerCase) {
        ecounts(key) = 1 + ecounts.getOrElseUpdate(key, 0)
      }
      docIdx += 1
      if(docIdx % dotEvery == 0) print(".")
      if(docIdx % lineEvery == 0) println(": read " + docIdx + " docs, " + wcounts.size + " words, " + ecounts.size + " entities.")
    }
    val ww = FileUtil.writer("data/d2d/wcounts.txt.gz", true)
    for((word,c) <- wcounts.toSeq.sortBy(-_._2)) {
      ww.println(word + "\t" + c)
    }
    ww.flush()
    ww.close
    val ew = FileUtil.writer("data/d2d/ecounts.txt.gz", true)
    for((ent,c) <- ecounts.toSeq.sortBy(-_._2)) {
      ew.println(ent + "\t" + c)
    }
    ew.flush()
    ew.close
  }
}