package org.sameersingh.ervisualizer.nlp
import com.typesafe.config.ConfigFactory


object KbaRunner extends App {
  val jsonPath = ConfigFactory.load().getString("nlp.kba.jsonPath")
  println(jsonPath)
}
