package org.sameersingh.ervisualizer.nlp

import edu.stanford.nlp.semgraph.SemanticGraph
import play.api.libs.json.Json
import org.sameersingh.ervisualizer.data.Document

/**
 * @author sameer
 * @since 7/11/14.
 */
class ReadD2DDocs(val baseDir: String) {

  def path(baseDir: String, name: String, format: String): String = {
    "%s/allafrica.com_07-2013-to-05-2014_%s/%s.%s" format(baseDir, format, name, format)
  }
  
  def readD2DNLP(name: String) {
    val source = io.Source.fromFile(path(baseDir, name, "nlp"), "UTF-8")
    for(s <- source.getLines().drop(8)) {
      println(s)
      val sg = SemanticGraph.valueOf(s)
      println(sg.toFormattedString)
    } //.mkString("\n")//.replaceAll("\\]\\[", "]\n[")

    source.close()
  }

  case class D2DDoc(title: Array[String], cite: Array[String], h1: Array[String], div: Array[String])
  implicit val d2dDocWrites = Json.writes[D2DDoc]
  implicit val d2dDocReads = Json.reads[D2DDoc]

  def readOriginalDoc(name: String): D2DDoc = {
    val source = io.Source.fromFile(path(baseDir, name, "json"), "UTF-8")
    val s = source.getLines().mkString("\n")//.replaceAll("\\]\\[", "]\n[")
    // println(s)
    val d = Json.fromJson[D2DDoc](Json.parse(s)).get
    // println(d)
    d
  }

  def readDoc(id: String, name: String): Document = {
    val d = readOriginalDoc(name)
    Document(id, name, d.title.mkString("___SEP___"), d.cite.mkString("___SEP___"), d.div.mkString("\n"), Seq.empty)
  }
}

object ReadD2DDocs extends ReadD2DDocs("/Users/sameer/Work/data/d2d") {
  def main(args: Array[String]) {
    readD2DNLP("Nigeria/piracy/stories/201307010505")
    // readOriginalDoc("Nigeria/piracy/stories/201307010505")
  }
}