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
    db._documents(doc1Id) = Document(doc1Id, "Obama is awesome. Yes, he is married to Michelle. He is president of USA, same as George W.",
      Seq(Sentence(doc1Id, 0, "Obama is awesome."),
        Sentence(doc1Id, 1, "Yes, he is married to Michelle."),
        Sentence(doc1Id, 2, "He is president of USA, same as George W.")))

    // entities
    val ent1Id = "ENT_00"
    db._entityIds += ent1Id
    db._entityHeader(ent1Id) = EntityHeader(ent1Id, "Barack Obama", "PER", 1.0)
    db._entityInfo(ent1Id) = EntityInfo(ent1Id, Map("/mid" -> "/m/02mjmr", "/people/person/place_of_birth" -> "Honululu", "/common/topic/image" -> "/m/02nqg_h"))
    db._entityFreebase(ent1Id) = EntityFreebase(ent1Id, Seq("/government/us_president", "/celebrities/celebrity"))
    val ent2Id = "ENT_01"
    db._entityIds += ent2Id
    db._entityHeader(ent2Id) = EntityHeader(ent2Id, "Michelle Obama", "PER", 0.5)
    db._entityInfo(ent2Id) = EntityInfo(ent2Id, Map("/mid" -> "/m/025s5v9", "/people/person/place_of_birth" -> "Chicago", "/common/topic/image" -> "/m/04s8ccw"))
    db._entityFreebase(ent2Id) = EntityFreebase(ent2Id, Seq("/government/politician", "/celebrities/celebrity"))
    val ent3Id = "ENT_02"
    db._entityIds += ent3Id
    db._entityHeader(ent3Id) = EntityHeader(ent3Id, "George W. Bush", "PER", 0.75)
    db._entityInfo(ent3Id) = EntityInfo(ent3Id, Map("/mid" -> "/m/09b6zr", "/people/person/place_of_birth" -> "New Haven", "/common/topic/image" -> "/m/02bs94j"))
    db._entityFreebase(ent3Id) = EntityFreebase(ent3Id, Seq("/government/us_president", "/celebrities/celebrity"))
    val ent4Id = "ENT_03"
    db._entityIds += ent4Id
    db._entityHeader(ent4Id) = EntityHeader(ent4Id, "United State of America", "LOC", 0.8)
    db._entityInfo(ent4Id) = EntityInfo(ent4Id, Map("/mid" -> "/m/09c7w0", "/common/topic/image" -> "/m/02bs94j"))
    db._entityFreebase(ent4Id) = EntityFreebase(ent4Id, Seq("/location/country", "/location/nation"))

    // entity text provenances
    db._entityText(ent1Id) = EntityText(ent1Id, Seq(Provenance(doc1Id, 0, Seq(0 -> 5)), Provenance(doc1Id, 1, Seq(5 -> 7)), Provenance(doc1Id, 2, Seq(0 -> 2))))
    db._entityText(ent2Id) = EntityText(ent2Id, Seq(Provenance(doc1Id, 1, Seq(22 -> 30))))
    db._entityText(ent3Id) = EntityText(ent3Id, Seq(Provenance(doc1Id, 2, Seq(32 -> 41))))
    db._entityText(ent4Id) = EntityText(ent4Id, Seq(Provenance(doc1Id, 2, Seq(19 -> 22))))

    // entity type provenances
    db._entityTypePredictions(ent1Id) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(ent1Id, new mutable.HashMap)("person") =
      TypeModelProvenances(ent1Id, "person", Seq(Provenance(doc1Id, 1, Seq(5 -> 7))))
    db._entityTypePredictions(ent2Id) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(ent2Id, new mutable.HashMap)("person") =
      TypeModelProvenances(ent2Id, "person", Seq(Provenance(doc1Id, 1, Seq(22 -> 30))))
    db._entityTypePredictions(ent3Id) = Seq("person")
    db._entityTypeProvenances.getOrElseUpdate(ent3Id, new mutable.HashMap)("person") =
      TypeModelProvenances(ent3Id, "person", Seq(Provenance(doc1Id, 2, Seq(32 -> 41))))
    db._entityTypePredictions(ent4Id) = Seq("country")
    db._entityTypeProvenances.getOrElseUpdate(ent4Id, new mutable.HashMap)("country") =
      TypeModelProvenances(ent4Id, "country", Seq(Provenance(doc1Id, 2, Seq(19 -> 22))))

    // relations
    val rel1Id = ent1Id -> ent2Id
    db._relationIds += rel1Id
    db._relationHeader(rel1Id) = RelationHeader(rel1Id._1, rel1Id._2, 0.25)
    db._relationFreebase(rel1Id) = RelationFreebase(rel1Id._1, rel1Id._2, Seq("/people/person/spouse"))
    val rel2Id = ent3Id -> ent4Id
    db._relationIds += rel2Id
    db._relationHeader(rel2Id) = RelationHeader(rel2Id._1, rel2Id._2, 1.0)
    db._relationFreebase(rel2Id) = RelationFreebase(rel2Id._1, rel2Id._2, Seq("/location/president"))
    val rel3Id = ent1Id -> ent4Id
    db._relationIds += rel3Id
    db._relationHeader(rel3Id) = RelationHeader(rel3Id._1, rel3Id._2, 0.75)
    db._relationFreebase(rel3Id) = RelationFreebase(rel3Id._1, rel3Id._2, Seq("/location/president"))

    // relations text provenances
    db._relationText(rel1Id) = RelationText(rel1Id._1, rel1Id._2, Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))
    db._relationText(rel2Id) = RelationText(rel1Id._1, rel1Id._2, Seq(Provenance(doc1Id, 2, Seq(32 -> 41, 19 -> 22))))
    db._relationText(rel3Id) = RelationText(rel1Id._1, rel1Id._2, Seq(Provenance(doc1Id, 2, Seq(0 -> 2, 19 -> 22))))

    // relations type provenances
    db._relationPredictions(rel1Id) = Seq("per:spouse")
    db._relationProvenances.getOrElseUpdate(rel1Id, new mutable.HashMap)("per:spouse") =
      RelModelProvenances(rel1Id._1, rel1Id._2, "per:spouse", Seq(Provenance(doc1Id, 1, Seq(5 -> 7, 22 -> 30))))
    db._relationPredictions(rel2Id) = Seq("loc:president")
    db._relationProvenances.getOrElseUpdate(rel2Id, new mutable.HashMap)("loc:president") =
      RelModelProvenances(rel2Id._1, rel2Id._2, "loc:president", Seq(Provenance(doc1Id, 2, Seq(32 -> 41, 19 -> 22))))
    db._relationPredictions(rel3Id) = Seq("loc:president")
    db._relationProvenances.getOrElseUpdate(rel3Id, new mutable.HashMap)("loc:president") =
      RelModelProvenances(rel3Id._1, rel2Id._2, "loc:president", Seq(Provenance(doc1Id, 2, Seq(0 -> 2, 19 -> 22))))

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