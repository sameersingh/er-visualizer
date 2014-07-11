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
    db._entityInfo(barackId) = EntityInfo(barackId, Map("/mid" -> "/m/02mjmr", "/people/person/place_of_birth" -> "Honululu", "/common/topic/image" -> "/m/02nqg_h"))
    db._entityFreebase(barackId) = EntityFreebase(barackId, Seq("/government/us_president", "/celebrities/celebrity"))
    val michelleId = "MichelleObama"
    db._entityIds += michelleId
    db._entityHeader(michelleId) = EntityHeader(michelleId, "Michelle Obama", "PER", 0.5)
    db._entityInfo(michelleId) = EntityInfo(michelleId, Map("/mid" -> "/m/025s5v9", "/people/person/place_of_birth" -> "Chicago", "/common/topic/image" -> "/m/04s8ccw"))
    db._entityFreebase(michelleId) = EntityFreebase(michelleId, Seq("/government/politician", "/celebrities/celebrity"))
    val georgeId = "GeorgeBush"
    db._entityIds += georgeId
    db._entityHeader(georgeId) = EntityHeader(georgeId, "George W. Bush", "PER", 0.75)
    db._entityInfo(georgeId) = EntityInfo(georgeId, Map("/mid" -> "/m/09b6zr", "/people/person/place_of_birth" -> "New Haven", "/common/topic/image" -> "/m/02bs94j"))
    db._entityFreebase(georgeId) = EntityFreebase(georgeId, Seq("/government/us_president", "/celebrities/celebrity"))
    val usaId = "USA"
    db._entityIds += usaId
    db._entityHeader(usaId) = EntityHeader(usaId, "United State of America", "LOC", 0.8)
    db._entityInfo(usaId) = EntityInfo(usaId, Map("/mid" -> "/m/09c7w0", "/common/topic/image" -> "/m/02nbh90"))
    db._entityFreebase(usaId) = EntityFreebase(usaId, Seq("/location/country", "/location/nation"))

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