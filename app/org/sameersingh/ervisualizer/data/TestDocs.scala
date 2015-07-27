package org.sameersingh.ervisualizer.data

import nlp_serde.annotators.{AnnotatorPipeline, Annotator, StanfordAnnotator}
import nlp_serde.immutable.Relation
import nlp_serde.writers.PerLineJsonWriter
import nlp_serde.{Document => Doc}

import scala.collection.mutable

/**
 * @author sameer
 * @since 7/27/15.
 */
object TestDocs {

  // Two documents, with two sentences each
  val docTexts = Seq(
    """
      | Barack Obama was born in Honolulu.
      | He was married to Michelle before he became the president of USA.
    """.stripMargin,
  """
    | Barack Obama went to Columbia University.
    | It was at Columbia that Barack met his wife-to-be, Michelle.
  """.stripMargin).iterator

  val nlpAnnotator = new StanfordAnnotator()

  val linker = new Annotator {
    override def process(doc: Doc): Doc = {
      for (e <- doc.entities) {
        e.representativeString match {
          case "Barack Obama" => e.freebaseIds("/m/02mjmr") = 1.0
          case "USA" => e.freebaseIds("/m/09c7w0") = 1.0
          case "Michelle" => e.freebaseIds("/m/025s5v9") = 1.0
          case "Columbia University" => e.freebaseIds("/m/01w5m") = 1.0
          case "Columbia" => e.freebaseIds("/m/01w5m") = 1.0
          case "Honolulu" => e.freebaseIds("/m/02hrh0_") = 1.0
          case _ => println("unlinked: " + e.representativeString)
        }
      }
      doc
    }
  }

  val relExtractor = new Annotator {
    override def process(doc: Doc): Doc = {
      for (s <- doc.sentences) {
        for(m1 <- s.mentions;
            e1id <- m1.entityId;
            e1 = doc.entity(e1id);
            if (!e1.freebaseIds.isEmpty);
            m2 <- s.mentions;
            e2id <- m2.entityId;
            e2 = doc.entity(e2id);
            if (!e2.freebaseIds.isEmpty);
            if (m1 != m2)) {
          val rels: mutable.Set[String] = (e1.freebaseIds.maxBy(_._2)._1, e2.freebaseIds.maxBy(_._2)._1) match {
            case ("/m/02mjmr", "/m/025s5v9") => mutable.Set("per:spouse")
            case ("/m/025s5v9","/m/02mjmr") => mutable.Set("per:spouse")
            case ("/m/02mjmr", "/m/01w5m") => mutable.Set("per:school_attended")
            case ("/m/02mjmr", "/m/02hrh0_") => mutable.Set("per:born_in")
            case ("/m/02mjmr", "/m/09c7w0") => mutable.Set("per:president_of", "per:lives_in")
            case _ => mutable.Set.empty
          }
          if(!rels.isEmpty) s.relations += {
            val r = new nlp_serde.Relation
            r.m1Id = m1.id
            r.m2Id = m2.id
            r.relations = rels
            r
          }
        }
      }
      doc
    }
  }
  
  val pipeline = new AnnotatorPipeline(Seq(nlpAnnotator, linker, relExtractor))

  def main(args: Array[String]): Unit = {
    val outputFile = "data/test/docs.json.gz"
    val docs = docTexts.zipWithIndex.map(ti => {
      val d = new Doc()
      d.id = "doc" + ti._2
      d.text = ti._1
      d
    })
    val w = new PerLineJsonWriter(true)
    w.write(outputFile, pipeline.process(docs))
  }

}
