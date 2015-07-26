package org.sameersingh.ervisualizer.nlp

import org.sameersingh.ervisualizer.data._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.ling.CoreAnnotations
import java.util.Arrays
import java.util
import com.typesafe.config.ConfigFactory
import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import org.sameersingh.ervisualizer.data.Provenance

/*
import edu.washington.multirframework.featuregeneration.{FeatureGenerator, DefaultFeatureGeneratorMinusDirMinusDep}
import edu.washington.multirframework.argumentidentification.{SententialInstanceGeneration, ArgumentIdentification, DefaultSententialInstanceGeneration, NERArgumentIdentification}
import edu.washington.multirframework.multiralgorithm._
import edu.washington.multir.preprocess.CorpusPreprocessing
import edu.washington.multirframework.data.{KBArgument, Argument}

/**
 * @author sameer
 * @since 7/11/14.
 */
class MultiRRunner(val pathToMultirFiles: String,
                   val fg: FeatureGenerator = new DefaultFeatureGeneratorMinusDirMinusDep,
                   val ai: ArgumentIdentification = NERArgumentIdentification.getInstance,
                   val sig: SententialInstanceGeneration = DefaultSententialInstanceGeneration.getInstance
                    ) {
  private var mapping: Mappings = null
  private var model: Model = null
  private var params: Parameters = null
  private var scorer: Scorer = null
  private val relID2rel = new mutable.HashMap[Integer, String]

  def init() = {
    import scala.collection.JavaConversions._
    val dir = pathToMultirFiles
    try {
      mapping = new Mappings
      mapping.read(dir + "/mapping")
      model = new Model
      model.read(dir + "/model")
      params = new Parameters
      params.model = model
      params.deserialize(dir + "/params")
      scorer = new Scorer
      for (key <- mapping.getRel2RelID.keySet) {
        val id: Integer = mapping.getRel2RelID.get(key)
        relID2rel.put(id, key)
      }
    }
    catch {
      case e: Exception => {
        e.printStackTrace
      }
    }
  }

  init()

  case class RelationMention(arg1: Argument, arg2: Argument, relation: String, score: Double, senText: String) {
    def toFormattedString: String =
      "%s|||%d|||%d|||%s|||%s|||%d|||%d|||%f" format(
        arg1.getArgName, arg1.getStartOffset, arg1.getEndOffset,
        relation, //.replaceAll("|","___"),
        arg2.getArgName, arg2.getStartOffset, arg2.getEndOffset, score)
  }

  def extractFromText(text: String, name: String = "default"): Seq[RelationMention] = {
    import scala.collection.JavaConversions._

    val doc: Annotation = CorpusPreprocessing.getTestDocumentFromRawString(text, name)
    val extractions = new ArrayBuffer[RelationMention]
    val sentences = doc.get(classOf[CoreAnnotations.SentencesAnnotation])

    for (s <- sentences) {
      val senText: String = s.get(classOf[CoreAnnotations.TextAnnotation])
      val args = ai.identifyArguments(doc, s)
      val sigs = sig.generateSententialInstances(args, s)
      import scala.collection.JavaConversions._
      for (p <- sigs) {
        val arg1: Argument = p.first
        val arg2: Argument = p.second
        var arg1ID: String = null
        var arg2ID: String = null
        if (p.first.isInstanceOf[KBArgument]) {
          arg1ID = (p.first.asInstanceOf[KBArgument]).getKbId
        }
        if (p.second.isInstanceOf[KBArgument]) {
          arg2ID = (p.second.asInstanceOf[KBArgument]).getKbId
        }
        val features = fg.generateFeatures(arg1.getStartOffset, arg1.getEndOffset, arg2.getStartOffset, arg2.getEndOffset, arg1ID, arg2ID, s, doc)
        val result = getPrediction(features.toList, arg1, arg2, senText)
        if (result != null) {
          val relationScoreTriple: Triple[String, Double, Double] = getPrediction(features.toList, arg1, arg2, senText)._1
          //val extractionString: String = arg1.getArgName + " " + relationScoreTriple._1 + " " + arg2.getArgName + "\n" + senText
          if (relationScoreTriple._1 != "NA")
            extractions.add(RelationMention(arg1, arg2, relationScoreTriple._1, relationScoreTriple._3, senText))
          //extractions.add(new Pair[String, Double](extractionString, relationScoreTriple._3))
        }
      }
    }

    for (e <- extractions) {
      val extrString: String = e.arg1.getArgName + " " + e.relation + " " + e.arg2.getArgName + "\n" + e.senText
      val score: Double = e.score
      System.out.println(extrString + "\t" + score)
    }

    extractions
  }

  /**
   * Conver features and args to MILDoc
   * and run Multir sentential extraction
   * algorithm, return null if no extraction
   * was predicted.
   * @param features
   * @param arg1
   * @param arg2
   * @return
   */
  private def getPrediction(features: List[String], arg1: Argument, arg2: Argument, senText: String): Pair[Triple[String, Double, Double], util.Map[Integer, java.lang.Double]] = {
    import scala.collection.JavaConversions._

    val doc: MILDocument = new MILDocument
    doc.arg1 = arg1.getArgName
    doc.arg2 = arg2.getArgName
    doc.Y = new Array[Int](1)
    doc.numMentions = 1
    doc.setCapacity(1)
    val sv: SparseBinaryVector = new SparseBinaryVector
    doc.features(0) = sv
    val ftrset: util.SortedSet[Integer] = new util.TreeSet[Integer]
    var totalfeatures: Int = 0
    var featuresInMap: Int = 0
    for (f <- features) {
      totalfeatures += 1
      val ftrid: Int = mapping.getFeatureID(f, false)
      if (ftrid >= 0) {
        featuresInMap += 1
        ftrset.add(ftrid)
      }
    }
    sv.num = ftrset.size
    sv.ids = new Array[Int](sv.num)
    var k: Int = 0
    for (f <- ftrset) {
      sv.ids(({
        k += 1;
        k - 1
      })) = f
    }
    var relation: String = ""
    var conf: Double = 0.0
    val mentionFeatureScoreMap = new java.util.HashMap[Integer, java.util.Map[Integer, java.lang.Double]]
    val parse: Parse = FullInference.infer(doc, scorer, params, mentionFeatureScoreMap)
    val Yp: Array[Int] = parse.Y
    if (parse.Z(0) > 0) {
      relation = relID2rel(parse.Z(0))
      Arrays.sort(parse.allScores(0))
      var combinedScore: Double = parse.score
      var i: Int = 0
      while (i < parse.allScores(0).length - 1) {
        {
          val s: Double = parse.allScores(0)(i)
          if (s > 0.0) {
            combinedScore += s
          }
        }
        ({
          i += 1;
          i - 1
        })
      }
      var confidence: Double = if ((combinedScore <= 0.0 || parse.score <= 0.0)) .1 else (parse.score / combinedScore)
      if (combinedScore == parse.score && parse.score > 0.0) {
        confidence = .001
      }
      conf = confidence
    } else {
      val negMentionFeatureScoreMap = new java.util.HashMap[Integer, java.util.Map[Integer, java.lang.Double]]
      val negParse: Parse = FullInference.infer(doc, scorer, params, negMentionFeatureScoreMap, 0)
      val t = Triple("NA", conf, parse.score)
      val p = (t -> negMentionFeatureScoreMap.get(0))
      return p
    }
    val t = new Triple[String, Double, Double](relation, conf, parse.score)
    val p = (t -> mentionFeatureScoreMap.get(0))
    return p
  }

  def extractFromDoc(fid: String, baseDir: String, filterSent: Int => Boolean = s => true) {
    val dname = "%s/processed/%s.sent" format(baseDir, fid)
    val output = "%s/processed/%s.rels" format(baseDir, fid)
    val writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"))
    val input = io.Source.fromFile(dname, "UTF-8")
    var sentId = 0
    for (s <- input.getLines()) {
      val extrs = if (filterSent(sentId)) extractFromText(s) else Seq.empty[RelationMention]
      if (extrs.map(_.senText).distinct.size > 1)
        println("Multiple sentences in {%s}: %s" format(s, extrs.mkString(", ")))
      writer.println(s + "\t" + extrs.map(_.toFormattedString).mkString("\t"))
      sentId += 1
    }
    input.close()
    writer.flush()
    writer.close()
  }

}

object TestMultiRRunner extends App {
  val modelPath = ConfigFactory.load().getString("nlp.multir.modelPath")
  println(modelPath)
  val multir = new MultiRRunner(modelPath)
  println(multir.extractFromText("Barack is married to Michelle.").mkString("\n"))
}

object RunMultiRRunner extends App {
  val modelPath = ConfigFactory.load().getString("nlp.multir.modelPath")
  val baseDir = ConfigFactory.load().getString("nlp.data.baseDir")
  val filelist = ConfigFactory.load().getString("nlp.data.filelist")
  println(modelPath)
  val multir = new MultiRRunner(modelPath)

  println("Reading processed documents")
  val reader = new ReadProcessedDocs(baseDir, filelist)
  val (db, einfo) = reader.readAllDocs

  println("Running relation extraction")
  val fileList = io.Source.fromFile(baseDir + "/" +  filelist, "UTF-8")
  for (line <- fileList.getLines();
       fid = line.split("\t")(0).dropRight(4)) {
    println("doc: " + fid)
    multir.extractFromDoc(fid, baseDir, (s: Int) => einfo.sentences.getOrElse(fid -> s, Seq.empty).size >= 2)
  }
  fileList.close()
}

class ReadMultiROutput(val baseDir: String, val filelist: String, val minScore: Double = Double.NegativeInfinity) {

  case class Mention(string: String, start: Int, end: Int)

  case class RelationMention(relation: String, arg1: Mention, arg2: Mention, score: Double)

  var numDirectErrors = 0
  var numSearchErrors = 0
  var numTotalRequests = 0

  private def findClosestProvenance(db: DB, m: Mention, ps: Seq[(String, Provenance)]): Option[(String, Provenance)] = {
    numTotalRequests += 1
    def dist(m: Mention, p: Provenance): Double = math.pow(m.start - p.tokPos(0)._1, 2) + math.pow(m.end - p.tokPos(0)._2, 2)
    val sp = ps.minBy(sp => dist(m, sp._2))
    val d = dist(m, sp._2)
    if (d > 0.0) {
      val sent = db.sentence(sp._2.docId, sp._2.sentId)
      numDirectErrors += 1
      //println(m + "\t" + sent + "\n\t" + ps.map(_._2.tokPos(0)).map(se => se + ":" + sent.substring(se._1, se._2)).mkString("\t"))
      // now try searching
      val result = ps.find(sp => m.string == sent.string.substring(sp._2.tokPos(0)._1,sp._2.tokPos(0)._2))
      if(result.isEmpty) numSearchErrors += 1
      result
    } else Some(sp)
  }

  def multirRelation(r: String): String = if(r.contains("/")) {
    val init = r.drop(1)
    val firstSlash = init.indexOf("/")
    val lastSlash = init.lastIndexOf("/")
    init.substring(0, firstSlash) + "_" + init.substring(lastSlash + 1)
  } else r

  def updateFromDoc(fid: String, db: InMemoryDB) {
    val dname = "%s/processed/%s.rels" format(baseDir, fid)
    if (!new File(dname).exists()) return
    val input = io.Source.fromFile(dname, "UTF-8")
    var sentId = 0
    for (s <- input.getLines()) {
      val split = s.split("\\t").drop(1) // drop the sentence text
      val rms = for (rmstr <- split) yield {
          // NDLEA|||58|||63|||/organization/organization/headquarters|/location/mailing_address/citytown|||Lagos|||117|||122|||138884194.000000
          val rmSplit = rmstr.split("\\|\\|\\|")
          assert(rmSplit.length == 8)
          RelationMention(multirRelation(rmSplit(3)),
            Mention(rmSplit(0), rmSplit(1).toInt, rmSplit(2).toInt),
            Mention(rmSplit(4), rmSplit(5).toInt, rmSplit(6).toInt), rmSplit(7).toDouble)
        }
      val trueProvenances = db.docEntityProvenances(fid, sentId).map({
        case (mid, ps) => ps.map(mid -> _)
      }).flatten.toSeq
      for (rm <- rms) {
        val a1 = findClosestProvenance(db, rm.arg1, trueProvenances)
        val a2 = findClosestProvenance(db, rm.arg2, trueProvenances)
        for (arg1P <- a1; arg2P <- a2) {
          val p = Provenance(fid, sentId, arg1P._2.tokPos ++ arg2P._2.tokPos, rm.score)
          // add to db
          val rid = if(arg1P._1 < arg2P._1) arg1P._1 -> arg2P._1 else arg2P._1 -> arg1P._1
          val rel = rm.relation
          db._relationPredictions(rid) = db._relationPredictions.getOrElse(rid, Set.empty) ++ Seq(rel)
          val rt = db._relationText.getOrElse(rid, RelationText(rid._1, rid._2, Seq.empty))
          db._relationText(rid) = RelationText(rt.sourceId, rt.targetId, rt.provenances ++ Seq(p))
          val rmp = db._relationProvenances.getOrElseUpdate(rid, new mutable.HashMap).getOrElseUpdate(rel, RelModelProvenances(rid._1, rid._2, rel, Seq.empty))
          db._relationProvenances(rid)(rel) = RelModelProvenances(rmp.sourceId, rmp.targetId, rmp.relType, rmp.provenances ++ Seq(p))
        }
      }
      sentId += 1
    }
    input.close()
  }

  def updateFromAllDocs(db: InMemoryDB) {
    println("Running relation extraction")
    val fileList = io.Source.fromFile(baseDir + "/" + filelist, "UTF-8")
    var numRead = 0
    for (line <- fileList.getLines();
         fid = line.split("\t")(0).dropRight(4)) {
      // println("doc: " + fid)
      updateFromDoc(fid, db)
      if (numRead % 79 == 0) print(".")
      numRead += 1
    }
    fileList.close()
    println
    println(s"numDirect: $numDirectErrors, numSearch: $numSearchErrors, total: $numTotalRequests")
  }

}
*/