package endpoint


import akka.actor.ActorSystem
import akka.util.Timeout
import com.redis.RedisClientPool
import endpoint.Error.UnexpectedResponseFromService
import org.hrw.login.service.mongodb.AccountDAO
import org.json4s.FieldSerializer._
import org.json4s._
import org.json4s.jackson.{JsonMethods, Serialization}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import scalaoauth2.provider._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

abstract class EndPoint(cc: ControllerComponents, system: ActorSystem)(implicit accountDAO: AccountDAO, redisPool: RedisClientPool) extends AbstractController(cc) with OAuth2Provider {

  implicit val dispatcher = system.dispatcher

  implicit val timeout = Timeout(10 seconds)

  //https://github.com/nulab/play2-oauth2-provider/blob/master/README.md You want to use which grant types are supported or to use a customized handler for a grant type, you should override the handlers map in a customized TokenEndpoint trait.
  override val tokenEndpoint = new OAuthTokenEndpoint

  implicit class future2then(future: Future[Any]) {
    def respond[T: Manifest](block: Any => Result): Future[Result] = {
      future map {
        case payload: T => block(payload)
        case error: Exception => internalServerError(error)
        case unexpected => internalServerError(UnexpectedResponseFromService(unexpected))
      }
    }
  }

  val serializer = FieldSerializer[Throwable](ignore("stackTrace") orElse ignore("cause"))

  implicit val formats = Serialization.formats(NoTypeHints) + serializer

  def successInFuture(content: Any = null)(implicit execution: ExecutionContext) = Future {
    success(content)
  }

  def success(content: Any = null) = Ok(Json.parse(writePretty(Response(Meta(200, "succeeded"), content))))

  def badRequestInFuture(ex: Throwable)(implicit execution: ExecutionContext) = Future {
    badRequest(ex)
  }

  def badRequest(ex: Throwable) = BadRequest(Json.parse(writePretty(Failed(BAD_REQUEST, ex.getMessage, ex))))

  def internalServerErrorInFuture(ex: Throwable)(implicit execution: ExecutionContext) = Future {
    internalServerError(ex)
  }

  def internalServerError(ex: Throwable) = InternalServerError(Json.parse(writePretty(Failed(INTERNAL_SERVER_ERROR, ex.getMessage, ex))))

  def writePretty[A <: AnyRef](a: A)(implicit formats: Formats): String = JsonMethods.mapper.writeValueAsString(Extraction.decompose(a)(formats).snakizeKeys)

  def toPrettyJson(anyRef: AnyRef): String = {
    writePretty(anyRef)
  }
}

case class Meta(`status_code`: Int, `service_code`: String = "", description: String = null)

case class Failed(code: Int, message: String, reason: Throwable = null) extends Exception {
  def toMeta = Meta(code, message)
}

case class Response(meta: Meta, content: Any = null)

object Error {

  case class RequestAreNotCorrect(issues: String) extends Exception {
    override def getMessage = s"request has some issues: $issues"
  }

  case class UnexpectedResponseFromService(response: Any) extends Exception {
    override def getMessage: String = s"unexpected response from service ${response.getClass.getSimpleName}"
  }

}