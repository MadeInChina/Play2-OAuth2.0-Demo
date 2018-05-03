package endpoint

import java.util.Date

import com.redis.RedisClientPool
import org.hrw.login.service.mongodb.AccountDAO
import org.hrw.login.service.oauth2.{Account, OauthAccessToken, OauthAuthorizationCode, OauthClient}
import play.api.Logger
import play.api.mvc.Action
import scalaoauth2.provider._

import scala.concurrent.{ExecutionContext, Future}

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

  private val accessTokenExpireSeconds = 3600



  override def validateClient(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Boolean] = {
    Logger.info(s"validate Client start.")
    val clientCredential = maybeCredential.get
    Logger.info(s"validate Client clientCredential:$clientCredential,grantType:${request.grantType}")
    val isClientValid = OauthClient.validate(clientCredential.clientId, clientCredential.clientSecret.getOrElse(""), request.grantType)
    Logger.info(s"validate Client is valid:$isClientValid")
    Future.successful(isClientValid)
  }

  override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = {
    println(s"get stored access token")
    Future.successful(OauthAccessToken.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(toAccessToken))
  }

  private def toAccessToken(accessToken: OauthAccessToken) = {
    AccessToken(
      accessToken.accessToken,
      Some(accessToken.refreshToken),
      None,
      Some(accessTokenExpireSeconds),
      new Date(accessToken.createdAt)
    )
  }

  override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = {
    println(s"create new access token")
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val oauthClient = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.create(authInfo.user, oauthClient)
    Future.successful(toAccessToken(accessToken))
  }

  override def findUser(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Option[Account]] = {

    request match {
      case request: PasswordRequest =>
        println(s"find user by username and password")
        Future.successful(Account.authenticate(request.username, request.password))
      case _: ClientCredentialsRequest =>
        println(s"find user by client credential")
        val clientCredential = maybeCredential.get
        Future.successful(OauthClient.findClientCredentials(clientCredential.clientId, clientCredential.clientSecret.getOrElse("")))
      case _ =>
        Future.successful(None)

    }
  }

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
    println(s"find authInfo by refresh token")
    Future.successful(OauthAccessToken.findByRefreshToken(refreshToken).flatMap { accessToken =>
      for {
        account <- accessToken.account
        client <- accessToken.oauthClient
      } yield {
        println("account:" + account)
        println("client:" + client)
        AuthInfo(
          user = account,
          clientId = Some(client.clientId),
          scope = None,
          redirectUri = None
        )
      }
    })
  }

  // Refresh token grant
  override def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = {
    println(s"refresh access token authInfo:" + authInfo)
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val client = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.refresh(authInfo.user, client)
    println(s"refresh access token end")
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