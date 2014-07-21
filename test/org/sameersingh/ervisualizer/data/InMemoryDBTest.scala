package org.sameersingh.ervisualizer.data

import org.junit.Test
import scala.collection.mutable
import java.io.File

/**
 * @author sameer
 * @since 6/12/14.
 */
class InMemoryDBTest {

  def initDB: DB = {
    val db = new InMemoryDB
    // docs
    val doc1Id = "DOC_000"
    db._documents(doc1Id) = Document(doc1Id, "", "", "", "Obama is awesome. Yes, he is married to Michelle. He is a resident of USA, same as George W. and Michelle.",
      Seq(Sentence(doc1Id, 0, "Obama is awesome."),
        Sentence(doc1Id, 1, "Yes, he is married to Michelle."),
        Sentence(doc1Id, 2, "He is a resident of USA, same as George W. and Michelle.")))

    // entities
    val barackId = "BarackObama"
    db._entityIds += barackId
    db._entityHeader(barackId) = EntityHeader(barackId, "Barack Obama", "PER", 1.0)
    db._entityInfo(barackId) = EntityInfo(barackId, Map("/mid" -> "/m/02mjmr",
      "/common/topic/description" ->
        "Barack Hussein Obama II is the 44th and current President of the United States, and the first African American to hold the office. Born in Honolulu, Hawaii, Obama is a graduate of Columbia University and Harvard Law School, where he served as president of the Harvard Law Review. He was a community organizer in Chicago before earning his law degree. He worked as a civil rights attorney and taught constitutional law at the University of Chicago Law School from 1992 to 2004. He served three terms representing the 13th District in the Illinois Senate from 1997 to 2004, running unsuccessfully for the United States House of Representatives in 2000. In 2004, Obama received national attention during his campaign to represent Illinois in the United States Senate with his victory in the March Democratic Party primary, his keynote address at the Democratic National Convention in July, and his election to the Senate in November. He began his presidential campaign in 2007 and, after a close primary campaign against Hillary Rodham Clinton in 2008, he won sufficient delegates in the Democratic Party primaries to receive the presidential nomination.",
      "/common/topic/image" -> "/m/02nqg_h"))
    db._entityFreebase(barackId) = EntityFreebase(barackId, Seq("US President"))
    val michelleId = "MichelleObama"
    db._entityIds += michelleId
    db._entityHeader(michelleId) = EntityHeader(michelleId, "Michelle Obama", "PER", 0.5)
    db._entityInfo(michelleId) = EntityInfo(michelleId, Map("/mid" -> "/m/025s5v9",
      "/common/topic/description" ->
        "Michelle LaVaughn Robinson Obama, an American lawyer and writer, is the wife of the 44th and current President of the United States, Barack Obama, and the first African-American First Lady of the United States. Raised on the South Side of Chicago, she is a graduate of Princeton University and Harvard Law School, and spent the early part of her legal career working at the law firm Sidley Austin, where she met her future husband. Subsequently, she worked as part of the staff of Chicago mayor Richard M. Daley, and for the University of Chicago Medical Center. Throughout 2007 and 2008, she helped campaign for her husband's presidential bid. She delivered a keynote address at the 2008 Democratic National Convention and also spoke at the 2012 Democratic National Convention. She is the mother of daughters Malia and Natasha. As the wife of a Senator, and later the First Lady, she has become a fashion icon and role model for women, and an advocate for poverty awareness, nutrition, and healthy eating.",
      "/common/topic/image" -> "/m/04s8ccw"))
    db._entityFreebase(michelleId) = EntityFreebase(michelleId, Seq("Celebrity"))
    val georgeId = "GeorgeBush"
    db._entityIds += georgeId
    db._entityHeader(georgeId) = EntityHeader(georgeId, "George W. Bush", "PER", 0.75)
    db._entityInfo(georgeId) = EntityInfo(georgeId, Map("/mid" -> "/m/09b6zr",
      "/common/topic/description" ->
        "George Walker Bush is an American politician and businessman who served as the 43rd President of the United States from 2001 to 2009, and the 46th Governor of Texas from 1995 to 2000. The eldest son of Barbara and George H. W. Bush, he was born in New Haven, Connecticut. After graduating from Yale University in 1968 and Harvard Business School in 1975, Bush worked in oil businesses. He married Laura Welch in 1977 and ran unsuccessfully for the House of Representatives shortly thereafter. He later co-owned the Texas Rangers baseball team before defeating Ann Richards in the 1994 Texas gubernatorial election. Bush was elected president in 2000 after a close and controversial election, becoming the fourth president to be elected while receiving fewer popular votes nationwide than his opponent. Bush is the second president to have been the son of a former president, the first being John Quincy Adams. He is also the brother of Jeb Bush, former Governor of Florida. Eight months into Bush's first term as president, the September 11, 2001 terrorist attacks occurred.",
      "/common/topic/image" -> "/m/02bs94j"))
    db._entityFreebase(georgeId) = EntityFreebase(georgeId, Seq("US President"))
    val usaId = "USA"
    db._entityIds += usaId
    db._entityHeader(usaId) = EntityHeader(usaId, "United State of America", "LOC", 0.8)
    db._entityInfo(usaId) = EntityInfo(usaId, Map("/mid" -> "/m/09c7w0",
      "/common/topic/description" ->
        "The United States of America, commonly referred to as the United States, America, and sometimes the States, is a federal republic consisting of 50 states and a federal district. The 48 contiguous states and Washington, D.C., are in central North America between Canada and Mexico. The state of Alaska is the northwestern part of North America and the state of Hawaii is an archipelago in the mid-Pacific. The country also has five populated and nine unpopulated territories in the Pacific and the Caribbean. At 3.71 million square miles and with around 318 million people, the United States is the world's third or fourth-largest country by total area and third-largest by population. It is one of the world's most ethnically diverse and multicultural nations, the product of large-scale immigration from many countries. The geography and climate of the United States is also extremely diverse, and it is home to a wide variety of wildlife. Paleo-Indians migrated from Eurasia to what is now the U.S. mainland around 15,000 years ago, with European colonization beginning in the 16th century. The United States emerged from 13 British colonies located along the Atlantic seaboard.",
      "/common/topic/image" -> "/m/02nbh90"))
    db._entityFreebase(usaId) = EntityFreebase(usaId, Seq("Country"))

    // entity text provenances
    db._entityText(barackId) = EntityText(barackId, Seq(Provenance(doc1Id, 0, Seq(0 -> 5)), Provenance(doc1Id, 1, Seq(5 -> 7)), Provenance(doc1Id, 2, Seq(0 -> 2))))
    db._entityText(michelleId) = EntityText(michelleId, Seq(Provenance(doc1Id, 1, Seq(22 -> 30)), Provenance(doc1Id, 2, Seq(47 -> 55))))
    db._entityText(georgeId) = EntityText(georgeId, Seq(Provenance(doc1Id, 2, Seq(33 -> 42))))
    db._entityText(usaId) = EntityText(usaId, Seq(Provenance(doc1Id, 2, Seq(20 -> 23))))

    // entity type provenances
    db._entityTypePredictions(barackId) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(barackId, new mutable.HashMap)("person") =
      TypeModelProvenances(barackId, "person", Seq(Provenance(doc1Id, 1, Seq(5 -> 7))))
    db._entityTypePredictions(michelleId) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(michelleId, new mutable.HashMap)("person") =
      TypeModelProvenances(michelleId, "person", Seq(Provenance(doc1Id, 1, Seq(22 -> 30)), Provenance(doc1Id, 2, Seq(47 -> 55))))
    db._entityTypePredictions(georgeId) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(georgeId, new mutable.HashMap)("person") =
      TypeModelProvenances(georgeId, "person", Seq(Provenance(doc1Id, 2, Seq(32 -> 41))))
    db._entityTypePredictions(usaId) = Seq("country")
    db._entityTypeProvenances.getOrElseUpdate(usaId, new mutable.HashMap)("country") =
      TypeModelProvenances(usaId, "country", Seq(Provenance(doc1Id, 2, Seq(20 -> 23))))

    // relations
    val barackMichelleId = barackId -> michelleId
    db._relationIds += barackMichelleId
    db._relationHeader(barackMichelleId) = RelationHeader(barackMichelleId._1, barackMichelleId._2, 0.25)
    db._relationFreebase(barackMichelleId) = RelationFreebase(barackMichelleId._1, barackMichelleId._2, Seq("/people/person/spouse"))
    val georgeUSAId = georgeId -> usaId
    db._relationIds += georgeUSAId
    db._relationHeader(georgeUSAId) = RelationHeader(georgeUSAId._1, georgeUSAId._2, 1.0)
    db._relationFreebase(georgeUSAId) = RelationFreebase(georgeUSAId._1, georgeUSAId._2, Seq("/location/president"))
    val barackUSAId = barackId -> usaId
    db._relationIds += barackUSAId
    db._relationHeader(barackUSAId) = RelationHeader(barackUSAId._1, barackUSAId._2, 0.75)
    db._relationFreebase(barackUSAId) = RelationFreebase(barackUSAId._1, barackUSAId._2, Seq("/location/president"))
    val michelleUSAId = michelleId -> usaId
    db._relationIds += michelleUSAId
    db._relationHeader(michelleUSAId) = RelationHeader(michelleUSAId._1, michelleUSAId._2, 0.25)
    db._relationFreebase(michelleUSAId) = RelationFreebase(michelleUSAId._1, michelleUSAId._2, Seq("/location/citizen"))

    // relations text provenances
    db._relationText(barackMichelleId) = RelationText(barackMichelleId._1, barackMichelleId._2, Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))
    db._relationText(georgeUSAId) = RelationText(barackMichelleId._1, barackMichelleId._2, Seq(Provenance(doc1Id, 2, Seq(33 -> 42, 20 -> 23))))
    db._relationText(barackUSAId) = RelationText(barackMichelleId._1, barackMichelleId._2, Seq(Provenance(doc1Id, 2, Seq(0 -> 2, 20 -> 23))))
    db._relationText(michelleUSAId) = RelationText(michelleUSAId._1, michelleUSAId._2, Seq(Provenance(doc1Id, 2, Seq(47 -> 55, 20 -> 23))))

    // relations type provenances
    db._relationPredictions(barackMichelleId) = Seq("spouse")
    db._relationProvenances.getOrElseUpdate(barackMichelleId, new mutable.HashMap)("spouse") =
      RelModelProvenances(barackMichelleId._1, barackMichelleId._2, "spouse", Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))

    db._relationPredictions(georgeUSAId) = Seq("citizen")
    db._relationProvenances.getOrElseUpdate(georgeUSAId, new mutable.HashMap)("citizen") =
      RelModelProvenances(georgeUSAId._1, georgeUSAId._2, "citizen", Seq(Provenance(doc1Id, 2, Seq(33 -> 42, 20 -> 23))))

    db._relationPredictions(barackUSAId) = Seq("citizen")
    db._relationProvenances.getOrElseUpdate(barackUSAId, new mutable.HashMap)("citizen") =
      RelModelProvenances(barackUSAId._1, barackUSAId._2, "citizen", Seq(Provenance(doc1Id, 2, Seq(0 -> 2, 20 -> 23))))

    db._relationPredictions(michelleUSAId) = Seq("citizen")
    db._relationProvenances.getOrElseUpdate(michelleUSAId, new mutable.HashMap)("citizen") =
      RelModelProvenances(michelleUSAId._1, michelleUSAId._2, "citizen", Seq(Provenance(doc1Id, 2, Seq(47 -> 55, 20 -> 23))))

    db
  }

  def writeFiles(db: DB): String = {
    val f = File.createTempFile("ervisualizer.data", (System.currentTimeMillis() % 1000).toString)
    if (f.exists() && f.isFile) f.delete()
    f.mkdirs()
    InMemoryDB.writeDB(f.getCanonicalPath, db)
    f.getCanonicalPath
  }

  @Test
  def testAll() {
    val db = initDB
    println(db)
    val dir = writeFiles(db)
    println("dir: " + dir)
    val ndb = InMemoryDB.readFromTSV(dir)
    println(ndb)
  }

}