package org.sameersingh.ervisualizer.kba

import com.typesafe.config.ConfigFactory


object KbaRunner extends App {
  val jsonPath = ConfigFactory.load().getString("nlp.kba.jsonPath")
  println(jsonPath)
}
