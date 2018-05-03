package org.hrw.login.service.oauth2

import java.util.Date

import com.google.inject.Inject
import com.redis.RedisClientPool
import org.hrw.login.service.mongodb.AccountDAO
import scalaoauth2.provider._

import scala.concurrent.Future

class OauthDataHandler @Inject()(implicit redisPool: RedisClientPool, accountDAO: AccountDAO) extends DataHandler[Account] {
  private val accessTokenExpireSeconds = 3600

  override def validateClient(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Boolean] = {
    val clientCredential = maybeCredential.get
    Future.successful(OauthClient.validate(clientCredential.clientId, clientCredential.clientSecret.getOrElse(""), request.grantType))
  }

  override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = {
    Future.successful(OauthAccessToken.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(toAccessToken))
  }

  override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = {
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val oauthClient = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.create(authInfo.user, oauthClient)
    Future.successful(toAccessToken(accessToken))
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

  // Refresh token grant

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
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
    val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
    val client = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
    val accessToken = OauthAccessToken.refresh(authInfo.user, client)
    Future.successful(toAccessToken(accessToken))
  }

  // Authorization code grant

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = {
    //      Future.successful(OauthAuthorizationCode.findByCode(code).flatMap { authorization =>
    //        for {
    //          account <- authorization.account
    //          client <- authorization.oauthClient
    //        } yield {
    //          AuthInfo(
    //            user = account,
    //            clientId = Some(client.clientId),
    //            scope = None,
    //            redirectUri = authorization.redirectUri
    //          )
    //        }
    //      })
    Future.successful(None)
  }

  override def deleteAuthCode(code: String): Future[Unit] = {
    //      Future.successful(OauthAuthorizationCode.delete(code))
    Future.successful(None)
  }

  // Protected resource

  override def findAccessToken(token: String): Future[Option[AccessToken]] = {
    Future.successful(OauthAccessToken.findByAccessToken(token).map(toAccessToken))
  }

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = {
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