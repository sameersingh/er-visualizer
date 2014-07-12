package org.sameersingh.ervisualizer.nlp

import org.sameersingh.ervisualizer.data.{Document, Sentence}
import scala.collection.mutable.ArrayBuffer
import edu.washington.multirframework.featuregeneration.{FeatureGenerator, DefaultFeatureGeneratorMinusDirMinusDep}
import edu.washington.multirframework.argumentidentification.{SententialInstanceGeneration, ArgumentIdentification, DefaultSententialInstanceGeneration, NERArgumentIdentification}
import edu.washington.multirframework.multiralgorithm._
import scala.collection.mutable
import edu.stanford.nlp.pipeline.Annotation
import edu.washington.multir.preprocess.CorpusPreprocessing
import edu.washington.multirframework.data.{KBArgument, Argument}
import edu.stanford.nlp.ling.CoreAnnotations
import org.sameersingh.ervisualizer.data.Sentence
import org.sameersingh.ervisualizer.data.Document
import java.util.Arrays
import java.util

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

  case class Relation(s: Sentence)

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

  def extractFromText(text: String, name: String) {
    import scala.collection.JavaConversions._

    val doc: Annotation = CorpusPreprocessing.getTestDocumentFromRawString(text, name)
    val extractions = new ArrayBuffer[Pair[String, Double]]
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
          val extractionString: String = arg1.getArgName + " " + relationScoreTriple._2 + " " + arg2.getArgName + "\n" + senText
          extractions.add(new Pair[String, Double](extractionString, relationScoreTriple._3))
        }
      }
    }

    for (extr <- extractions) {
      val extrString: String = extr._1
      val score: Double = extr._2
      System.out.println(extrString + "\t" + score)
    }
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

  def extract(doc: Document): Seq[Relation] = {
    extractFromText(doc.text, doc.path)
    Seq.empty
  }

}
