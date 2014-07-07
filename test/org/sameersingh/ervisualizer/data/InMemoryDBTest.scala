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
    db._documents(doc1Id) = Document(doc1Id, "Obama is awesome. Yes, he is married to Michelle.",
      Seq(Sentence(doc1Id, 0, "Obama is awesome."), Sentence(doc1Id, 1, "Yes, he is married to Michelle.")))

    // entities
    val ent1Id = "ENT_00"
    db._entityIds += ent1Id
    db._entityHeader(ent1Id) = EntityHeader(ent1Id, "Barack Obama", 1.0)
    db._entityInfo(ent1Id) = EntityInfo(ent1Id, Map("/mid" -> "/m/02mjmr", "/people/person/place_of_birth" -> "Honululu", "/common/topic/image" -> "/m/02nqg_h"))
    db._entityFreebase(ent1Id) = EntityFreebase(ent1Id, Seq("/government/us_president", "/celebrities/celebrity"))
    val ent2Id = "ENT_01"
    db._entityIds += ent2Id
    db._entityHeader(ent2Id) = EntityHeader(ent2Id, "Michelle Obama", 0.5)
    db._entityInfo(ent2Id) = EntityInfo(ent2Id, Map("/mid" -> "/m/025s5v9", "/people/person/place_of_birth" -> "Chicago", "/common/topic/image" -> "/m/04s8ccw"))
    db._entityFreebase(ent2Id) = EntityFreebase(ent2Id, Seq("/government/politician", "/celebrities/celebrity"))

    // entity text provenances
    db._entityText(ent1Id) = EntityText(ent1Id, Seq(Provenance(doc1Id, 0, Seq(0 -> 5)), Provenance(doc1Id, 1, Seq(5 -> 7))))
    db._entityText(ent2Id) = EntityText(ent2Id, Seq(Provenance(doc1Id, 1, Seq(22 -> 30))))

    // entity type provenances
    db._entityTypePredictions(ent1Id) = Seq("/people/person")
    db._entityTypeProvenances.getOrElseUpdate(ent1Id, new mutable.HashMap)("/people/person") =
      TypeModelProvenances(ent1Id, "/people/person", Seq(Provenance(doc1Id, 1, Seq(5 -> 7))))
    db._entityTypePredictions(ent2Id) = Seq("/people/person")
    db._entityTypeProvenances.getOrElseUpdate(ent2Id, new mutable.HashMap)("/people/person") =
      TypeModelProvenances(ent2Id, "/people/person", Seq(Provenance(doc1Id, 1, Seq(22 -> 30))))

    // relations
    val rel1Id = ent1Id -> ent2Id
    db._relationIds += rel1Id
    db._relationHeader(rel1Id) = RelationHeader(rel1Id._1, rel1Id._2, 1.0)
    db._relationFreebase(rel1Id) = RelationFreebase(rel1Id._1, rel1Id._2, Seq("/people/person/spouse"))

    // relations text provenances
    db._relationText(rel1Id) = RelationText(rel1Id._1, rel1Id._2, Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))

    // relations type provenances
    db._relationPredictions(rel1Id) = Seq("per:spouse")
    db._relationProvenances.getOrElseUpdate(rel1Id, new mutable.HashMap)("per:spouse") =
      RelModelProvenances(rel1Id._1, rel1Id._2, "per:spouse", Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))

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