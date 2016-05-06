package endpoint

import java.util.Date

import com.redis.RedisClientPool
import org.hrw.login.service.mongodb.AccountDAO
import org.hrw.login.service.oauth2.{Account, OauthAccessToken, OauthAuthorizationCode, OauthClient}
import play.api.mvc.Action

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider._

class AuthorizeAction {

}

object AuthorizeAction extends OAuth2Provider {
  def async(block: Account => scala.concurrent.Future[play.api.mvc.Result])(implicit ec: ExecutionContext, redisPool: RedisClientPool, accountDAO: AccountDAO) = Action.async { implicit request =>
    authorize(new AuthorizeDataHandler()) { authInfo =>
      val user = authInfo.user // User is defined on your system
      // access resource for the user
      block(user)
    }
  }

}

class AuthorizeDataHandler(implicit redisPool: RedisClientPool, accountDAO: AccountDAO) extends DataHandler[Account] {

  override def validateClient(clientCredential: ClientCredential, grantType: String): Future[Boolean] = {
    println(s"validating client")
    Future.successful(OauthClient.validate(clientCredential.clientId, clientCredential.clientSecret.getOrElse(""), grantType))
  }

  override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = {
    println(s"get stored access token")
    Future.successful(OauthAccessToken.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(toAccessToken))
  }

  override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = {
    println(s"create new access token")
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val oauthClient = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.create(authInfo.user, oauthClient)
    Future.successful(toAccessToken(accessToken))
  }

  private val accessTokenExpireSeconds = 3600

  private def toAccessToken(accessToken: OauthAccessToken) = {
    AccessToken(
      accessToken.accessToken,
      Some(accessToken.refreshToken),
      None,
      Some(accessTokenExpireSeconds),
      new Date(accessToken.createdAt)
    )
  }

  // Password grant

  override def findUser(username: String, password: String): Future[Option[Account]] = {
    println(s"find user by username and password")
    Future.successful(Account.authenticate(username, password))
  }

  // Client credentials grant

  override def findClientUser(clientCredential: ClientCredential, scope: Option[String]): Future[Option[Account]] = {
    println(s"find user by client credential")
    Future.successful(OauthClient.findClientCredentials(clientCredential.clientId, clientCredential.clientSecret.getOrElse("")))
  }

  // Refresh token grant

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
    println(s"find authInfo by refresh token")
    Future.successful(OauthAccessToken.findByRefreshToken(refreshToken).flatMap { accessToken =>
      for {
        account <- accessToken.account
        client <- accessToken.oauthClient
      } yield {
        AuthInfo(
          user = account,
          clientId = Some(client.clientId),
          scope = None,
          redirectUri = None
        )
      }
    })
  }

  override def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = {
    println(s"refresh access token")
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val client = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.refresh(authInfo.user, client)
    Future.successful(toAccessToken(accessToken))
  }

  // Authorization code grant

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = {
    println(s"find authInfo by authorization code")
    Future.successful(OauthAuthorizationCode.findByCode(code).flatMap { authorization =>
      for {
        account <- authorization.account
        client <- authorization.oauthClient
      } yield {
        AuthInfo(
          user = account,
          clientId = Some(client.clientId),
          scope = None,
          redirectUri = authorization.redirectUri
        )
      }
    })
  }

  override def deleteAuthCode(code: String): Future[Unit] = {
    println(s"delete authorization code")
    Future.successful(OauthAuthorizationCode.delete(code))
  }

  // Protected resource

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    println(s"find accesss token")
    Future.successful(OauthAccessToken.findByAccessToken(token).map(toAccessToken))
  }

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = {
    println(s"find authInfo by access token")
    Future.successful(OauthAccessToken.findByAccessToken(accessToken.token).flatMap { case accessToken =>
      for {
        account <- accessToken.account
        client <- accessToken.oauthClient
      } yield {
        AuthInfo(
          user = account,
          clientId = Some(client.clientId),
          scope = None,
          redirectUri = None
        )
      }
    })
  }
}