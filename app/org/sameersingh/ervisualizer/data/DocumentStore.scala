package org.sameersingh.ervisualizer.data

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

  def addTopicWords(d: nlp_serde.Document, word: String): Unit = {
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
