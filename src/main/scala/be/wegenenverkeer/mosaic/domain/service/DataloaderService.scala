package be.wegenenverkeer.mosaic.domain.service

import java.net.URL

import be.wegenenverkeer.api.dataloader.dsl.scalaplay.client.ClientConfig
import be.wegenenverkeer.api.dataloader.model.{ ConfiguredStatus, JobStatus, SyncStatus }
import be.wegenenverkeer.atomium.extension.feedconsumer.FeedPosition
import be.wegenenverkeer.mosaic.util.Logging
import be.wegenenverkeer.restfailure.{ RestException, RestFailure, _ }
import play.api.Configuration

import scala.concurrent.{ ExecutionContext, Future }

class DataloaderService(configuration: Configuration) extends Logging {

  private val baseUrl = configuration.getOptional[String]("dataloader.url").getOrElse(sys.error("Heb een waarde nodig voor dataloader.url"))

  val dataloaderApi = be.wegenenverkeer.api.dataloader.DataloaderApi(
    url    = new URL(baseUrl),
    config = ClientConfig(requestTimeout = 10000)
  )

  def getVerkeersbordenJobStatus()(implicit context: ExecutionContext): Future[Option[JobStatus]] = {
    dataloaderApi.job.jobNaam("verkeersborden").get().map {
      case response if response.status == 200 => Some(response.body.get)
      case response if response.status == 404 => None
      case response                           => throw new RestException(RestFailure.fromStatus(response.status, response.stringBody.getOrElse("").asErrorMessage))
    }
  }

  def getVerkeersbordenFeedPosition(feedUrl: String)(implicit context: ExecutionContext): Future[FeedPosition] = {
    getVerkeersbordenJobStatus.map { jobStatus =>
      jobStatus
        .collect {
          case ConfiguredStatus(feedReaders, naam, _, _) =>
            feedReaders.headOption
              .flatMap(_.lastSuccess)
              .collect {
                case SyncStatus(url, Some(entryId)) =>
                  val fixedUrl = url.split(feedUrl).last
                  FeedPosition(naam, fixedUrl, entryId)
              }
              .getOrElse(throw new Exception(s"Kon feedposition niet vinden in $jobStatus"))
        }
        .getOrElse(throw new Exception(s"Kon feedposition niet vinden in $jobStatus"))
    }
  }

}
