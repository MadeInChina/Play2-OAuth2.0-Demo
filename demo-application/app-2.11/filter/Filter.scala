package filter

import com.google.inject.Inject
import play.api.Logger
import play.api.http.HttpFilters
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class AccessLog extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      Logger.info(s"${requestHeader.method} ${requestHeader.uri} took $requestTime ms and returned ${result.header.status}")
      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}

class Filters @Inject()(log: AccessLog) extends HttpFilters {
  val filters = Seq(log)
}