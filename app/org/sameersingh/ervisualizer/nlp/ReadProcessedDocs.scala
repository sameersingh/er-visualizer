package org.sameersingh.ervisualizer.nlp

import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.data._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.Some
import org.sameersingh.ervisualizer.data.Sentence
import org.sameersingh.ervisualizer.data.Document
import java.io.{FileOutputStream, OutputStreamWriter, PrintWriter}
import org.sameersingh.ervisualizer.freebase.MongoIO
import play.api.libs.json.Json

/**
 * @author sameer
 * @since 7/15/14.
 */
/*
class ReadProcessedDocs(val baseDir: String, val filelist: String) {

  val reader = new ReadD2DDocs(baseDir)

  def sentences(fid: String): Seq[String] = {
    val sents = io.Source.fromFile(baseDir + "/processed/%s.sent" format (fid), "UTF-8")
    val strings = new ArrayBuffer[String]
    for (s <- sents.getLines()) {
      strings += s
    }
    sents.close()
    strings
  }

  var errors = 0

  class Mention(val docId: String,
                val sentId: Int,
                val toks: ArrayBuffer[String] = new ArrayBuffer,
                var nerTag: Option[String] = None,
                var wiki: Option[(String, String, Int)] = None,
                val figerTypes: mutable.HashSet[String] = new mutable.HashSet) {
    override def toString: String =
      "%s %d:\t%s\t%s\t%s\t%s" format(docId, sentId, toks.mkString(", "), nerTag, wiki, figerTypes.mkString(", "))

    def provenance(s: Sentence): Option[Provenance] = {
      assert(s.sentId == sentId)
      assert(s.docId == docId)
      val searchString = {
        var init = toks.mkString(" ").replaceAll(" '", "'").replaceAll("\\\\/", "/")
        if(init.contains("Defense") && s.string.contains("Defence")) init = init.replaceAll("Defense", "Defence")
        if(init.contains("Defence") && s.string.contains("Defense")) init = init.replaceAll("Defence", "Defense")
        if(init.contains("defense") && s.string.contains("defence")) init = init.replaceAll("defense", "defence")
        if(init.contains("defence") && s.string.contains("defense")) init = init.replaceAll("defence", "defense")
        if(init.contains("rganization") && s.string.contains("rganisation")) init = init.replaceAll("rganization", "rganisation")
        if(init.contains("rganisation") && s.string.contains("rganization")) init = init.replaceAll("rganisation", "rganization")
        if(init.contains("Labour") && s.string.contains("Labor")) init = init.replaceAll("Labour", "Labor")
        if(init.contains("Labor") && s.string.contains("Labour")) init = init.replaceAll("Labor", "Labour")
        if(init.contains(". ") && !s.string.contains(init)) init = init.replaceAll("\\. ", ".")
        if(init.contains(" ,") && !s.string.contains(init)) init = init.replaceAll(" ,", ",")
        init
      }
      val start = s.string.indexOf(searchString)
      if (start < 0) {
        errors += 1
        println("Cannot find {%s} in {%s}" format(searchString, s.string))
        None
      } else {
        val end = start + searchString.length
        Some(Provenance(s.docId, s.sentId, Seq(start -> end)))
      }
    }
  }

  def readMentions(fid: String): Seq[Mention] = {
    val mentions = new ArrayBuffer[Mention]
    var currentSentId = 0
    var currentTokId = 0
    var currentMention: Mention = null

    def endCurrentMention() {
      if (currentMention != null) mentions += currentMention
      currentMention = null
    }

    def beginNewMention() {
      assert(currentMention == null)
      currentMention = new Mention(fid, currentSentId)
    }

    def addToCurrMention(tok: String, seg: String, figer: Seq[String], wiki: (String, String, Int), ner: String) {
      if (seg == "O") {
        endCurrentMention()
        return
      }
      if (seg.startsWith("B-")) {
        endCurrentMention()
        beginNewMention()
      }
      if (seg.startsWith("I-")) {
        assert(currentMention != null)
      }
      currentMention.toks += tok
      assert(currentMention.nerTag.forall(_ == ner))
      if (ner != "O") currentMention.nerTag = Some(ner)
      assert(currentMention.wiki.forall(_ == wiki))
      if (wiki._1 != "O") currentMention.wiki = Some(wiki)
      assert(currentMention.figerTypes.isEmpty || currentMention.figerTypes.toSet == figer.toSet, "Figer mismatch: " + currentMention.figerTypes.mkString(",") + ", new: " + figer.mkString(","))
      currentMention.figerTypes ++= figer
    }

    val figerIter = io.Source.fromFile(baseDir + "/processed/%s.figer" format (fid), "UTF-8").getLines()
    val segsIter = io.Source.fromFile(baseDir + "/processed/%s.segment" format (fid), "UTF-8").getLines()
    val wikiIter = io.Source.fromFile(baseDir + "/processed/%s.wiki" format (fid), "UTF-8").getLines()

    while (figerIter.hasNext && segsIter.hasNext && wikiIter.hasNext) {
      val figer = figerIter.next().split("\t")
      val segs = segsIter.next().split("\t")
      val wiki = wikiIter.next().split("\t")
      if (figer.length == 1) {
        // end of sentence
        assert(segs.length == 1)
        assert(wiki.length == 1)
        assert(figer(0).isEmpty)
        assert(segs(0).isEmpty)
        assert(wiki(0).isEmpty)
        // end of mention?
        endCurrentMention()
        currentTokId = 0
        currentSentId += 1
      } else {
        val tok = figer(0)
        assert(tok == segs(0) && tok == wiki(0))
        val seg = segs(1)
        val ner = segs(2)
        val figerStr = figer(1)
        val figerList: Seq[String] = if (figerStr == "O") Seq.empty else figerStr.drop(2).split(",").map(_.drop(1).replaceAll("\\/", ":")).toSeq
        val wikiTriplet = if (wiki(1) == "O") ("O", "", 0) else (wiki(1), wiki(2), wiki(3).toInt)
        addToCurrMention(tok, seg, figerList, wikiTriplet, ner)
        currentTokId += 1
      }
    }
    assert(!figerIter.hasNext && !segsIter.hasNext && !wikiIter.hasNext)
    // end of sentence
    endCurrentMention()
    mentions
  }

  class EntityInfo {
    val mentions = new mutable.HashMap[String, ArrayBuffer[Mention]]
    val sentences = new mutable.HashMap[(String, Int), mutable.HashSet[String]]

    def +=(m: Mention) {
      val mid = m.wiki.get._1.drop(1).replaceFirst("/", "_")
      mentions.getOrElseUpdate(mid, new ArrayBuffer) += m
      sentences.getOrElseUpdate(m.docId -> m.sentId, new mutable.HashSet) += mid
    }
  }

  def assimilateMentions(mentions: Seq[Mention], einfo: EntityInfo) {
    for (m <- mentions) {
      if (m.wiki.isDefined && m.wiki.get._1 != "null") {
        einfo += m
      }
    }
  }

  def entityToDB(mid: String, mentions: Seq[Mention], db: InMemoryDB, maxMentions: Double) {
    // TODO get types from mongo, and then filter mentions according to that, then continue if mentions.isNotEmpty
    db._entityIds += mid
    // header
    val name = mentions.head.wiki.get._2
    val nerTag = mentions.map(_.nerTag.get).groupBy(x => x).map(p => p._1 -> p._2.size).toSeq.sortBy(-_._2).head._1
    // normalize popularity?
    db._entityHeader(mid) = EntityHeader(mid, name, nerTag, mentions.size / maxMentions)
    // info
    // empty for now, filled in later
    db._entityInfo(mid) = EntityInfo(mid, Map.empty)
    // freebase
    // empty for now, filled in later
    db._entityFreebase(mid) = EntityFreebase(mid, Seq.empty)
    // text provenances
    val provenances = mentions.map(m => m.provenance(db.document(m.docId).sents(m.sentId)))
    val distinctProvenances = provenances.flatten.distinct
    db._entityText(mid) = EntityText(mid, distinctProvenances)
    for(p <- distinctProvenances) {
      assert(p.tokPos.length == 1)
      val map = db._docEntityProvenances.getOrElseUpdate(p.docId -> p.sentId, new mutable.HashMap)
      map(mid) = map.getOrElse(mid, Seq.empty) ++ Seq(p)
    }
    // figer provenances
    val figerTypes = new mutable.LinkedHashSet[String]
    mentions.foreach(figerTypes ++= _.figerTypes)
    db._entityTypePredictions(mid) = figerTypes.toSeq
    db._entityTypeProvenances.getOrElseUpdate(mid, new mutable.HashMap) ++= mentions
      .zip(provenances)
      .map({
      case (m, p) => {
        m.figerTypes.map(ft => ft ->(m, p)).toSeq
      }
    }).flatten
      .groupBy(_._1)
      .map({
      case (s, v) => s -> v.map(_._2)
    }).map({
      case (s, mps) => s -> TypeModelProvenances(mid, s, mps.map(_._2).flatten.distinct)
    })
  }

  def readDoc(fid: String, path: String, db: InMemoryDB, einfo: EntityInfo) {
    //println("--- doc: " + fid + " ---")
    val origName = "Nigeria/%s/stories/%s" format(path, fid)
    // read document
    val ddoc = reader.readDoc(fid, origName)
    // read sentences
    val sentStrings = sentences(fid)
    val doc = Document(fid, origName, ddoc.title, ddoc.cite, ddoc.text, sentStrings.zipWithIndex.map(si => Sentence(fid, si._2, si._1)))
    db._documents(doc.docId) = doc
    // read mentions
    val mentions = readMentions(fid)
    assimilateMentions(mentions, einfo)
  }

  def readAllDocs: (DB, EntityInfo) = {
    val db = new InMemoryDB
    val einfo = new EntityInfo
    val fileList = io.Source.fromFile(baseDir + "/" + filelist, "UTF-8")
    var numRead = 0
    for (line <- fileList.getLines()) {
      val split = line.split("\t")
      val fid = split(0).dropRight(4)
      val path = split(1)
      readDoc(fid, path, db, einfo)
      numRead += 1
      if (numRead % 79 == 0) print(".")
    }
    println()
    fileList.close()
    println(einfo.mentions.size + ": " + einfo.mentions.toSeq.sortBy(-_._2.size).take(10).map({
      case (k, v) => k + "(" + v.size + ")"
    }).mkString(","))
    // add entities to DB
    val maxMentions = einfo.mentions.map(_._2.size).max.toDouble
    for ((mid, ms) <- einfo.mentions) {
      entityToDB(mid, ms, db, maxMentions)
    }
    println("Num of errors: " + errors)
    (db, einfo)
  }
}


object ReadProcessedDocs extends App {
  val baseDir = ConfigFactory.load().getString("nlp.data.baseDir")
  val filelist = ConfigFactory.load().getString("nlp.data.filelist")
  val reader = new ReadProcessedDocs(baseDir, filelist)
  val db = reader.readAllDocs
  /*
  println("Writing mids")
  val entityIdWriter = new PrintWriter(baseDir + "/d2d.mids")
  for(mid <- db._1.entityIds) {
    entityIdWriter.println(mid)
  }
  entityIdWriter.flush()
  entityIdWriter.close()
  */
  println("Starting Mongo")
  val mongo = new MongoIO("localhost", 27017)
  mongo.updateDB(db._1.asInstanceOf[InMemoryDB])
  println("Writing Mongo")
  val entityHeaderWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(baseDir + "/d2d.ent.head"), "UTF-8"))
  val entityInfoWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(baseDir + "/d2d.ent.info"), "UTF-8"))
  val entityFreebaseWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(baseDir + "/d2d.ent.freebase"), "UTF-8"))
  import org.sameersingh.ervisualizer.data.JsonWrites._
  for(mid <- db._1.entityIds) {
    entityHeaderWriter.println(Json.toJson(db._1.entityHeader(mid)))
    entityInfoWriter.println(Json.toJson(db._1.entityInfo(mid)))
    entityFreebaseWriter.println(Json.toJson(db._1.entityFreebase(mid)))
  }
  entityHeaderWriter.flush()
  entityHeaderWriter.close()
  entityInfoWriter.flush()
  entityInfoWriter.close()
  entityFreebaseWriter.flush()
  entityFreebaseWriter.close()
  //println(db)
}
*/