package org.sameersingh.ervisualizer.data

import java.io.PrintWriter

import org.sameersingh.ervisualizer.nlp.{ReadMultiROutput, ReadProcessedDocs}
import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.freebase.MongoIO
import scala.collection.mutable
import play.api.libs.json.Json

import scala.collection.mutable.ArrayBuffer

/**
 * Created by sameer on 7/20/14.
 */
class D2DDB {

  def addRelationInfo(db: InMemoryDB) {
    val maxProvenances = db._relationText.map({
      case (rid, map) => map.provenances.size
    }).max.toDouble
    for ((rid, rt) <- db._relationText) {
      db._relationIds += rid
      // TODO read from freebase
      db._relationFreebase(rid) = RelationFreebase(rid._1, rid._2, Seq.empty)
      db._relationHeader(rid) = RelationHeader(rid._1, rid._2, rt.provenances.size.toDouble / maxProvenances)
    }
    val minScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).min
    val maxScore = db._relationProvenances.values.map(_.values).flatten.map(_.provenances).flatten.map(p => math.log(p.confidence)).max
    for ((pair, relMap) <- db._relationProvenances) {
      for ((r, rmps) <- relMap) {
        relMap(r) = RelModelProvenances(rmps.sourceId, rmps.targetId, rmps.relType, rmps.provenances,
          math.sqrt(rmps.provenances.map(p => (math.log(p.confidence) - minScore) / maxScore).max)) //sum / rmps.provenances.size.toDouble)
      }
    }
  }

  def readFromMongoJson(baseDir: String, db: InMemoryDB) {
    val ehf = io.Source.fromFile(baseDir + "/d2d.ent.head", "UTF-8").getLines()
    val eif = io.Source.fromFile(baseDir + "/d2d.ent.info", "UTF-8").getLines()
    val eff = io.Source.fromFile(baseDir + "/d2d.ent.freebase", "UTF-8").getLines()
    import JsonReads._
    for (ehl <- ehf; eil = eif.next(); efl = eff.next()) {
      val eh = Json.fromJson[EntityHeader](Json.parse(ehl)).get
      val ei = Json.fromJson[EntityInfo](Json.parse(eil)).get
      val ef = Json.fromJson[EntityFreebase](Json.parse(efl)).get
      assert(eh.id == ei.id)
      assert(eh.id == ef.id)
      val mid = eh.id
      if (db._entityHeader.contains(mid)) {
        db._entityHeader(mid) = eh
        db._entityInfo(mid) = ei
        db._entityFreebase(mid) = ef
      }
    }
    assert(eif.isEmpty)
    assert(eff.isEmpty)
  }

  def readDB(filelistSuffix: Option[String] = None): DB = {
    // read raw documents and entity links
    println("Read raw docs")
    val cfg = ConfigFactory.load()
    val baseDir = cfg.getString("nlp.data.baseDir")
    val filelist = if (filelistSuffix.isDefined) "d2d.filelist." + filelistSuffix.get else cfg.getString("nlp.data.filelist")
    val processedDocReader = new ReadProcessedDocs(baseDir, filelist)
    val (db, einfo) = processedDocReader.readAllDocs

    // fill entity info with freebase info
    println("Read mongo info")
    if (cfg.getBoolean("nlp.data.mongo")) {
      val mongo = new MongoIO("localhost", 27017)
      mongo.updateDB(db.asInstanceOf[InMemoryDB])
    } else readFromMongoJson(baseDir, db.asInstanceOf[InMemoryDB])

    // read relations and convert that to provenances
    println("Read relations")
    val relReader = new ReadMultiROutput(baseDir, filelist)
    relReader.updateFromAllDocs(db.asInstanceOf[InMemoryDB])

    // aggregate info to relations from provenances
    println("Aggregate relation info")
    addRelationInfo(db.asInstanceOf[InMemoryDB])

    db
  }
}

object D2DDB extends D2DDB

class SummaTextToHTML(text: String) {

  case class TreeNode(text: String, children: ArrayBuffer[TreeNode]) {
    def toString(prefix: Int): String = {
      (0 until prefix).map(i => "-").foldLeft("")(_ + _) + text + "\n" + children.map(_.toString(prefix + 1)).mkString("")
    }

    def html(prefix: Int): String = {
      val prefixStr = (0 until prefix + 1).map(i => "\t").foldLeft("")(_ + _)
      prefixStr +
        "<span>" + text + "</span>\n" +
        (if (children.size > 0) {
          prefixStr + "<ul class=\"list-group\"%s>\n".format(if (prefix == 0) " id=\"root\"" else "") +
            children.map(c => prefixStr + "<li class=\"list-group-item\">\n" + c.html(prefix + 1) + prefixStr + "</li>\n").mkString("") +
            prefixStr + "</ul>\n"
        } else "")
    }
  }

  def tree: TreeNode = {
    val root = TreeNode("", new ArrayBuffer)
    val maxTabs = 3
    val currNodes = new mutable.HashMap[Int, TreeNode]
    currNodes(0) = root
    for (line <- text.split("\n+")) {
      val numTabs = if (line.startsWith("\t\t\t")) 3 else if (line.startsWith("\t\t")) 2 else if (line.startsWith("\t")) 1 else 0
      val str = line.trim
      val node = TreeNode(str, new ArrayBuffer)
      assert(currNodes.contains(numTabs))
      currNodes(numTabs).children += node
      currNodes(numTabs + 1) = node
      ((numTabs + 2) to maxTabs).map(i => currNodes.remove(i))
    }
    root
  }

  def html: String =
    """
      |<html>
      |<head>
      |  <title>UW Summa</title>
      |  <script src="../../javascripts/listCollapse.js" type="text/javascript" language="javascript1.2"></script>
      |  <link href="../../javascripts/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
      |  <script src="../../javascripts/bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
      |</head>
      |<body onload="compactMenu('root',false,'');">
      |<div class="container">
      |<h1><a href="index.html">Summa <img id="logo" width="30" src="http://knowitall.cs.washington.edu/summa/img/logo.png"></a></h1>
      |%s
      |</div>
      |<body>
      |</html>
    """.stripMargin format (tree.html(0))
}

object SummaTextToHTML {
  def main(args: Array[String]): Unit = {
    val baseDir = "/Users/sameer/Google Drive/UW/D2D/D2D Nov 14/summa/"
    val fileName = if (args.isEmpty) "janara" else args(0)
    val text = io.Source.fromFile(baseDir + fileName + ".summa").getLines().mkString("\n")
    val summa = new SummaTextToHTML(text)
    val output = "public/html/summa/" + fileName + ".html"
    val writer = new PrintWriter(output)
    writer.println(summa.html)
    writer.flush()
    writer.close()
  }
}