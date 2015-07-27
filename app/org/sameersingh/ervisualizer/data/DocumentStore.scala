package org.sameersingh.ervisualizer.data

import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.Logging
import nlp_serde.FileUtil
import nlp_serde.readers.PerLineJsonReader

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashSet, HashMap}

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
  val entities = new HashSet[String]

  def numDocs = docMap.size

  def +=(d: nlp_serde.Document): nlp_serde.Document = {
    docMap.getOrElseUpdate(d.id, d)
    // touch to instantiate maps
    d.mentions
    d.entity
    d.sentences.foreach(s => {
      s.depTree = None
      s.parseTree = None
      s.tokens.foreach(t => {
        t.pos = None
        t.ner = None
      })
    })
    d
  }

  def apply(id: Id) = docMap(id)

  def get(id: Id) = docMap.get(id)

  def addKeywords(d: nlp_serde.Document): Unit = {
    this += d
    for (s <- d.sentences; t <- s.tokens; lemma <- t.lemma; key = lemma.toLowerCase; if (keywords(key))) {
      keywordsMap.getOrElseUpdate(key, new HashSet[Id]) += d.id
    }
  }

  def addEntities(doc: nlp_serde.Document): Unit = {
    this += doc
    for (e <- doc.entities; if (!e.freebaseIds.isEmpty); key = FreebaseReader.convertFbIdToId(e.freebaseIds.maxBy(_._2)._1); if (entities(key))) {
      entitiesMap.getOrElseUpdate(key, new HashSet[Id]) += doc.id
    }
  }

  def addTopic(d: nlp_serde.Document, word: String): Unit = {
    this += d
    topicsMap.getOrElseUpdate(word.toLowerCase, new HashSet[Id]) += d.id
  }

  def query(queryString: String): Iterable[Id] = {
    var results: HashSet[Id] = null
    if(queryString.isEmpty) return docMap.keys
    for(q <- queryString.split("\\s")) {
      val ids = if(q.startsWith("topic:")) {
        topicsMap.getOrElse(q.drop(6), Set.empty[Id])
      } else if(q.startsWith("ent:")) {
        entitiesMap.getOrElse(q.drop(4).replaceAll("_", " "), Set.empty[Id])
      } else {
        keywordsMap.getOrElse(q, Set.empty[Id])
      }
      if(results == null) {
        results = new mutable.HashSet[Id]()
        results ++= ids
      } else {
        results.retain((i:Id) => ids(i))
      }
    }
    if(results == null) {
      results = new mutable.HashSet[Id]()
    }
    results
  }
}

object DocumentStore extends Logging {
  val stopWords = HashSet("a", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "b", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "c", "came", "can", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "course", "currently", "d", "definitely", "described", "despite", "did", "different", "do", "does", "doing", "done", "down", "downwards", "during", "e", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "f", "far", "few", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "g", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "h", "had", "happens", "hardly", "has", "have", "having", "he", "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "it", "its", "itself", "j", "just", "k", "keep", "keeps", "kept", "know", "knows", "known", "l", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "m", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "n", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "p", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "q", "que", "quite", "qv", "r", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "s", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "t", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "u", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "uucp", "v", "value", "various", "very", "via", "viz", "vs", "w", "want", "wants", "was", "way", "we", "welcome", "well", "went", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wonder", "would", "would", "x", "y", "yes", "yet", "you", "your", "yours", "yourself", "yourselves", "z", "zero")

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

  def readWords(file: String, min: Int = 20): Iterable[String] = {
    val s = FileUtil.inputSource(file, true)
    val result = new ArrayBuffer[String]()
    for(l <- s.getLines()) {
      val split = l.split("\t")
      assert(split.length == 2)
      val (w,c) = split(0) -> split(1).toInt
      if(c > min && !stopWords(w))
        result += w
    }
    s.close()
    result
  }

  def readDocs(store: DocumentStore, dir: String, docsFile: String): Unit = {
    logger.info("Reading counts")
    store.keywords ++= readWords(dir + "/wcounts.txt.gz", 0)
    store.entities ++= readWords(dir + "/ecounts.txt.gz", 0).map(mid => FreebaseReader.convertFbIdToId(mid))
    logger.info(" # words    : " + store.keywords.size)
    logger.info(" # entities : " + store.entities.size)
    logger.info("Reading title topics")
    //val titleTopics = readTopics(dir, "title")
    logger.info("Reading content topics")
    //val contentTopics = readTopics(dir, "content")
    logger.info("Reading documents")
    val docsPath = dir + "/" + docsFile
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    for(doc <- new PerLineJsonReader().read(docsPath)) {
      store += doc
      // add topics
      //titleTopics.get(doc.id).foreach(t => store.addTopic(doc, "title" + t))
      //contentTopics.get(doc.id).foreach(t => store.addTopic(doc, "content" + t))
      // add entities
      store.addEntities(doc)
      // add words
      store.addKeywords(doc)
      docIdx += 1
      if(docIdx % dotEvery == 0) print(".")
      if(docIdx % lineEvery == 0) println(": read " + docIdx + " docs, " + store.keywords.size + " words, "
        + store.topicsMap.size + " topics, " + store.entitiesMap.size + " entities.")
    }
    logger.info("Done.")
    logger.info("Entities: " + store.entitiesMap.take(10).map(_._1).mkString(", "))
    logger.info("Words: " + store.keywordsMap.take(10).map(_._1).mkString(", "))
    logger.info("Topics: " + store.topicsMap.take(10).map(_._1).mkString(", "))
  }
}

object WordCounts {
  def main(args: Array[String]): Unit = {
    val baseDir = ConfigFactory.load().getString("nlp.data.baseDir")
    val docsFile = ConfigFactory.load().getString("nlp.data.docsFile")
    val docsPath = baseDir + "/" + docsFile
    val wcounts = new mutable.HashMap[String, Int]()
    val ecounts = new mutable.HashMap[String, Int]()
    val dotEvery = 100
    val lineEvery = 1000
    var docIdx = 0
    for(d <- new PerLineJsonReader().read(docsPath)) {
      for (s <- d.sentences; t <- s.tokens; lemma <- t.lemma; key = lemma.toLowerCase) {
        wcounts(key) = 1 + wcounts.getOrElse(key, 0)
      }
      for (e <- d.entities; if (!e.freebaseIds.isEmpty); key = e.freebaseIds.maxBy(_._2)._1) {
        ecounts(key) = 1 + ecounts.getOrElseUpdate(key, 0)
      }
      docIdx += 1
      if(docIdx % dotEvery == 0) print(".")
      if(docIdx % lineEvery == 0) println(": read " + docIdx + " docs, " + wcounts.size + " words, " + ecounts.size + " entities.")
    }
    val ww = FileUtil.writer(baseDir + "/wcounts.txt.gz", true)
    for((word,c) <- wcounts.toSeq.sortBy(-_._2)) {
      ww.println(word + "\t" + c)
    }
    ww.flush()
    ww.close
    val ew = FileUtil.writer(baseDir + "/ecounts.txt.gz", true)
    for((ent,c) <- ecounts.toSeq.sortBy(-_._2)) {
      ew.println(ent + "\t" + c)
    }
    ew.flush()
    ew.close
  }
}