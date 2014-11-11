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
          prefixStr + "<ul class=\"list-group\"%s>\n".format(if(prefix==0) " id=\"root\"" else "") +
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
      |  <script src="../../../javascripts/listCollapse.js" type="text/javascript" language="javascript1.2"></script>
      |  <link href="../../../javascripts/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
      |  <script src="../../../javascripts/bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
      |</head>
      |<body onload="compactMenu('root',false,'');">
      |<div class="container">
      |<h1><a href="index.html">Summa <img id="logo" width="30" src="http://knowitall.cs.washington.edu/summa/img/logo.png"></a></h1>
      |%s
      |</div>
      |<body>
      |</html>
    """.stripMargin format(tree.html(0))
}

object SummaTextToHTML {
  def main(args: Array[String]): Unit = {
    val text = """5-8-2014	As good as that may sound , but whichever politician refused to hijack policies in favour of the US was made to face financial espionage or '' corruption charges '' .
                 |	5-8-2014	As good as that may sound , but whichever politician refused to hijack policies in favour of the US was made to face financial espionage or '' corruption charges '' .
                 |		5-8-2014	Years , later the CIA while tactically taking advantage of growing sectarian violence in Nigeria , recruited jobless Islamic extremist through Muslim and other traditional leaders offering training indirectly to the group by use of foreign based terror groups .
                 |		5-8-2014	Today as Nigerians are reeling from the negative effects of the insurgency that has befallen our dear country and earnestly seeking answers to what all this portends for the future , the GREENWHITE COALITION a citizen 's watchdog can reveal the true nature of this silent , undeclared war of attrition waged against Nigeria by the Government of United States of America .
                 |	5-17-2014	Countries neighbouring Nigeria are ready to wage war against the Nigeria-based , al-Qaeda-linked group , Boko Haram , Chad 's president says .
                 |	5-28-2014	At least 31 security personnel have been killed following an attack on a military base in Nigeria by Boko Haram fighters , security sources and witnesses said .
                 |5-30-2014	Source 2 in Lagos claims to have heard of a '' Hosni '' through a network of associates .
                 |	5-30-2014	Source 2 in Lagos claims to have heard of a '' Hosni '' through a network of associates .
                 |		5-30-2014	The northeast of Nigeria is plagued by Boko Haram attacks and has been under a state of emergency since May 2013 .
                 |		5-30-2014	Nigeria 's president has said he has ordered '' total war '' against the armed group Boko Haram which last month abducted 276 schoolgirls in the northeastern state of Borno .
                 |		5-30-2014	Chief of Defence Staff Air Chief Marshal Alex Badeh said any potential armed rescue operation was fraught with danger as the girls could be caught in the crossfire .
                 |	5-31-2014	Violence in northeastern Nigeria no longer fits the overly simplistic early narrative of Muslims killing Christians .
                 |		5-31-2014	Today , visitors travelling to Maiduguri by road will notice an absence of uniformed military presence on the streets of the historic town .
                 |		5-31-2014	Violence in northeastern Nigeria no longer fits the overly simplistic early narrative of Muslims killing Christians .
                 |		5-31-2014	Once described as the '' home of peace '' by locals , Maiduguri - the capital of Borno state - is now better known as the epicentre of deadly attacks and abductions that have killed thousands of Nigerians in schools , churches , mosques and markets .
                 |	6-9-2014	Source 7 in Bingi reports no confirmation of the arrival of Lagos-based radical Islamists .
                 |		6-5-2014	Source 1 in Lagos has heard street talk of an impending operation to assassinate the Nigerian Prime Minister .
                 |		6-7-2014	Source 7 has heard rumors concerning the arrival in Bingi of potential radical Islamists from Lagos who may be part of a catastrophic plot to kill hundreds , if not thousands , of people in and around Lagos .
                 |		6-11-2014	Source 1 in Lagos reports that the following individuals are among the most radical members of the Khoury Habib Mosque in Lagos : Omar Assad , Hani Boutros , and Yousef Najeeb .
                 |	6-15-2014	Source 11 in Onitsha states that he has not noted any rise in anti-Nigerian sentiment among Onitsha 's small Moslem community .
                 |	6-16-2014	Source 9 in Lagos claims that two of his brother-in-law 's friends , Tawfiq Attuk and Bassam Bahran , staying in Nigeria on extended tourist visas , have expressed their support for Boko Haram activities in the Middle East .
                 |6-21-2014	Source 10 in Abuja describes Al Samarah as leader of the '' virulent anti-Western faction '' among his business associates .
                 |	6-29-2014	She and several of her associates are lobbying for separate schools for boys and girls .
                 |		6-19-2014	Source 4 in Lagos reports no apparent increase in anti-Nigerian rhetoric among the members of his Young Men 's Islamic Association in the aftermath of Operation Iraqi Freedom .
                 |		6-22-2014	Source 16 in Benin City cites Malik Mosul as a '' dangerous subversive '' operating in the city .
                 |		6-29-2014	She and several of her associates are lobbying for separate schools for boys and girls .
                 |	7-2-2014	Source 17 in Uyo reports a political meeting having taken place between Gimmel Faruk and Dimitri Yagdanich , a Bosnian immigrant .
                 |	7-5-2014	Source believes this is probable evidence of influx of '' conspiracy-mongers from Lagos . ''
                 |7-8-2014	Source 17 in Uyo reports that his friend Karmij Aziz claims to have been offered a job by one Ali Hakem because of his computer hacking skills .
                 |	7-10-2014	Source 1 in Lagos claims an association between Khaleed Kulloh and Djibouti Jones .
                 |	7-11-2014	Source 9 in Lagos claims to have seen Khaleed Kulloh and Phil Salwah together on several occasions at several mosques in the East Side .
                 |	7-27-2014	Samagu 's Islamic community is very small and it is unusual for an immigrant to show up and stay for several weeks without family or business ties .
                 |	7-28-2014	Source 18 , located in the University of Benin , reports that several of the more radical Islamic students have left resigned from the university to apparently return to Saudi Arabia , yet they are still staying at their hotel in Benin City .""".stripMargin
    val summa = new SummaTextToHTML(text)
    val output = "public/html/summa/janara/summa.html"
    val writer = new PrintWriter(output)
    writer.println(summa.html)
    writer.flush()
    writer.close()
  }
}