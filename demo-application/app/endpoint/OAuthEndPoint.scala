package endpoint


import akka.actor.ActorSystem
import com.google.inject.Inject
import com.redis.RedisClientPool
import org.hrw.login.service.mongodb.AccountDAO
import org.hrw.login.service.oauth2.Account
import play.api.libs.json.{JsObject, JsString, Json, Writes}
import play.api.mvc.ControllerComponents
import scalaoauth2.provider._


class OAuthEndPoint @Inject()(cc: ControllerComponents, system: ActorSystem)(implicit accountDAO: AccountDAO, redisPool: RedisClientPool) extends EndPoint(cc, system) with OAuth2ProviderActionBuilders {

  implicit val authInfoWrites = new Writes[AuthInfo[Account]] {
    def writes(authInfo: AuthInfo[Account]) = {
      JsObject(Seq(
        "account" -> JsObject(
          Seq("email" -> JsString(authInfo.user.email))
        ),
        "clientId" -> JsString(authInfo.clientId.getOrElse("")),
        "redirectUri" -> JsString(authInfo.redirectUri.getOrElse(""))
      ))
    }
  }


  //
  def accessToken = Action.async { implicit request =>
    issueAccessToken(new AuthorizeDataHandler())
  }

  def test = AuthorizeAction.async { account => {
    successInFuture(account)
  }
  }


  def resources = AuthorizedAction(new AuthorizeDataHandler()) { request =>
    Ok(Json.toJson(request.authInfo))
  }
}

