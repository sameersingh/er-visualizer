package org.sameersingh.ervisualizer.data

import java.io.PrintWriter

import com.typesafe.config.ConfigFactory
import org.sameersingh.ervisualizer.freebase.MongoIO
import scala.collection.mutable
import play.api.libs.json.Json

import scala.collection.mutable.ArrayBuffer

/**
 * Created by sameer on 7/20/14.
 */
class SummaTextToHTML(text: String) {

  case class TreeNode(text: String, children: ArrayBuffer[TreeNode]) {
    def toString(prefix: Int): String = {
      (0 until prefix).map(i => "-").foldLeft("")(_ + _) + text + "\n" + children.map(_.toString(prefix + 1)).mkString("")
    }

    def html(prefix: Int): String = {
      val prefixStr = (0 until prefix + 1).map(i => "\t").foldLeft("")(_ + _)
      prefixStr +
        "<span>" + text + "</span>\n" +
        (if (children.size > 0) {
          prefixStr + "<ul class=\"list-group\"%s>\n".format(if (prefix == 0) " id=\"root\"" else "") +
            children.map(c => prefixStr + "<li class=\"list-group-item\">\n" + c.html(prefix + 1) + prefixStr + "</li>\n").mkString("") +
            prefixStr + "</ul>\n"
        } else "")
    }
  }

  def tree: TreeNode = {
    val root = TreeNode("", new ArrayBuffer)
    val maxTabs = 3
    val currNodes = new mutable.HashMap[Int, TreeNode]
    currNodes(0) = root
    for (line <- text.split("\n+")) {
      val numTabs = if (line.startsWith("\t\t\t")) 3 else if (line.startsWith("\t\t")) 2 else if (line.startsWith("\t")) 1 else 0
      val str = line.trim
      val node = TreeNode(str, new ArrayBuffer)
      assert(currNodes.contains(numTabs))
      currNodes(numTabs).children += node
      currNodes(numTabs + 1) = node
      ((numTabs + 2) to maxTabs).map(i => currNodes.remove(i))
    }
    root
  }

  def html: String =
    """
      |<html>
      |<head>
      |  <title>UW Summa</title>
      |  <script src="/assets/javascripts/listCollapse.js" type="text/javascript" language="javascript1.2"></script>
      |  <link href="/assets/javascripts/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
      |  <script src="/assets/javascripts/bootstrap/js/bootstrap.min.js" type="text/javascript"></script>
      |</head>
      |<body onload="compactMenu('root',false,'');">
      |<div class="container">
      |<h1><a href="index.html">Summa <img id="logo" width="30" src="/assets/images/summa_logo.png"></a></h1>
      |%s
      |</div>
      |<body>
      |</html>
    """.stripMargin format (tree.html(0))
}

object SummaTextToHTML {
  def main(args: Array[String]): Unit = {
    val baseDir = ConfigFactory.load().getString("nlp.data.baseDir") + "/summa/"
    val fileName = if (args.isEmpty) "janara" else args(0)
    val text = io.Source.fromFile(baseDir + fileName + "/summa.txt").getLines().mkString("\n")
    val summa = new SummaTextToHTML(text)
    val output = "public/html/summa/" + fileName + ".html"
    val writer = new PrintWriter(output)
    writer.println(summa.html)
    writer.flush()
    writer.close()
  }
}