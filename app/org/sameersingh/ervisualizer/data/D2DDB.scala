package org.sameersingh.ervisualizer.data

import org.sameersingh.ervisualizer.nlp.ReadProcessedDocs
import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.freebase.MongoIO

/**
 * Created by sameer on 7/20/14.
 */
class D2DDB {

  def readDB: DB = {
    // read raw documents and entity links
    val baseDir = ConfigFactory.load().getString("nlp.data.baseDir")
    val processedDocReader = new ReadProcessedDocs(baseDir)
    val (db, einfo) = processedDocReader.readAllDocs

    // fill entity info with freebase info
    val mongo = new MongoIO("localhost", 27017)
    mongo.updateDB(db.asInstanceOf[InMemoryDB])

    // TODO read relations and convert that to provenances

    // TODO read relation info from freebase?
    db
  }
}
