package org.sameersingh.ervisualizer.data

/**
 * @author sameer
 * @since 6/10/14.
 */
case class Provenance(docId: String, sentId: Int, tokPos: Seq[(Int, Int)], confidence: Double = 1.0)

case class Sentence(docId: String, sentId: Int, string: String)

case class Document(docId: String, path: String, title: String, cite: String, text: String, sents: Seq[Sentence])