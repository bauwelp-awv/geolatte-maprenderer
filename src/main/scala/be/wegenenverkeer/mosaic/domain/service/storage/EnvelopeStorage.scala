package be.wegenenverkeer.mosaic.domain.service.storage

import be.wegenenverkeer.mosaic.domain.model.CRS
import org.geolatte.geom.{C2D, Envelope}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try

trait EnvelopeStorage extends EnvelopeWriter with EnvelopeReader

case class EnvelopeFile(envelope: Envelope[C2D], fileId: String, receiptHandle: Option[String] = None) {
  def verwijderRef: String = receiptHandle.getOrElse(fileId)
}

trait EnvelopeWriter {

  def schrijf(envelope: Envelope[C2D]): Future[Unit]
}

object EnvelopeWriter {

  def envelopeToString(envelope: Envelope[C2D]): String = {
    val min = envelope.lowerLeft()
    val max = envelope.upperRight()
    Json.stringify(Json.toJson(Seq(min.getX, min.getY, max.getX, max.getY)))
  }

}

trait EnvelopeReader {

  def lees(limit: Int, uitgezonderd: Set[String]): Future[List[EnvelopeFile]]

  def verwijder(envelopeFile: EnvelopeFile): Future[Unit]

  def aantalItemsBeschikbaar(): Future[Long]

}

object EnvelopeReader {

  def parseEnvelopeString(content: String): Try[Envelope[C2D]] = Try {
    val coords = Json.parse(content).validate[Seq[Double]].getOrElse(throw new Exception(s"Could not parse envelope $content"))

    val minX :: minY :: maxX :: maxY :: Nil = coords

    new Envelope[C2D](minX, minY, maxX, maxY, CRS.LAMBERT72)
  }
}