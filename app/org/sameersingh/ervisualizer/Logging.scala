package org.sameersingh.ervisualizer

import play.api.Logger

/**
 * @author sameer
 * @since 7/26/15.
 */
trait Logging {
  val logger: Logger = Logger(this.getClass())
}
