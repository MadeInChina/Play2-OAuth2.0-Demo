package endpoint

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import play.api.Play
import play.api.http.Status._
import play.api.libs.iteratee.{Execution, Iteratee, Traversable}
import play.api.mvc.{BodyParser, RequestHeader, Result, Results}

import scala.concurrent.Future

object TypedJsonParser {
  private implicit val formats = Serialization.formats(NoTypeHints)

  import play.api.mvc.BodyParsers.parse._

  private def _parser[T: Manifest] = BodyParser("test") { request =>

    import Execution.Implicits.trampoline
    Traversable.takeUpTo[Array[Byte]](1024 * 1024 * 512)
      .transform(Iteratee.consume[Array[Byte]]().map { content =>
      val body = new String(content, request.charset.getOrElse("UTF-8"))
      Serialization.read[T](body)
    }).flatMap(Iteratee.eofOrElse(Results.EntityTooLarge))
  }

  def parser[T: Manifest] = when(
    _.contentType.exists(_.equalsIgnoreCase("application/json")),
    _parser[T],
    createBadResult("please use content-type : application/json", BAD_REQUEST)
  )

  private def createBadResult(msg: String, statusCode: Int = BAD_REQUEST): RequestHeader => Future[Result] = { request =>
    Play.maybeApplication.map(_.errorHandler.onClientError(request, statusCode, msg))
      .getOrElse(Future.successful(Results.BadRequest))
  }
}
