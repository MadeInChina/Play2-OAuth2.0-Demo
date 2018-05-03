package org.hrw.login.service.oauth2

import java.security.SecureRandom
import java.util.UUID

import com.redis.RedisClientPool
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._

import scala.util.Random

case class OauthAccessToken(
                             id: String = UUID.randomUUID().toString,
                             accountId: Long,
                             account: Option[Account] = None,
                             oauthClientId: Long,
                             oauthClient: Option[OauthClient] = None,
                             accessToken: String,
                             refreshToken: String,
                             createdAt: Long = System.currentTimeMillis()
                           )

object OauthAccessToken {
  implicit val formats = Serialization.formats(NoTypeHints)
  private val AFFECTED_ROW_EMPTY: Int = 0

  def refresh(account: Account, client: OauthClient)(implicit redisPool: RedisClientPool): OauthAccessToken = {
    delete(account, client)
    create(account, client)
  }

  def create(account: Account, oauthClient: OauthClient)(implicit redisPool: RedisClientPool): OauthAccessToken = {
    def randomString(length: Int) = new Random(new SecureRandom()).alphanumeric.take(length).mkString

    val accessTokenExpiresIn = 60 * 600000 // 1 hour
    val accessToken = randomString(40)
    val refreshToken = randomString(40)

    val oauthAccessToken = new OauthAccessToken(
      accountId = account.id,
      account = Some(account),
      oauthClient = Some(oauthClient),
      oauthClientId = oauthClient.id,
      accessToken = accessToken,
      refreshToken = refreshToken
    )

    redisPool.withClient { client =>
      val accessToken = oauthAccessToken.accessToken
      val refreshToken = oauthAccessToken.refreshToken
      val tokenKey: String = s"tokens:$accessToken:accessToken"
      val refreshTokenKey: String = s"tokens:$refreshToken:refreshToken"
      val tokenObj = write(oauthAccessToken)
      // Create the token
      client.set(tokenKey, tokenObj)
      client.set(refreshTokenKey, tokenObj)
      // Create a reverse index for searching
      val idx = s"${oauthClient.ownerId}#${oauthClient.clientId}"
      val tokenIndex: String = s"accessToken:$idx"
      client.set(tokenIndex, tokenObj)
      // Define expiration
      client.expire(tokenKey, accessTokenExpiresIn)
      client.expire(tokenIndex, accessTokenExpiresIn)
    }
    oauthAccessToken
  }

  def delete(account: Account, oauthClient: OauthClient)(implicit redisPool: RedisClientPool): Int = {
    redisPool.withClient { client =>
      val tokenIndex: String = s"accessToken:${oauthClient.ownerId}#${oauthClient.clientId}"
      val token = client.get(tokenIndex)
      token match {
        case None => AFFECTED_ROW_EMPTY
        case Some(t) => {
          client.del(tokenIndex).getOrElse(0L).toInt
          client.del(s"tokens:$token:accessToken").getOrElse(0L).toInt
        }
      }
    }
  }

  def findByAccessToken(accessToken: String)(implicit redisPool: RedisClientPool): Option[OauthAccessToken] = {
    redisPool.withClient { client =>
      client.get(s"tokens:$accessToken:accessToken") match {
        case None => None
        case Some(x) => Some(convert(x))
      }
    }
  }

  private def convert(str: String) = {
    parse(str).extract[OauthAccessToken]
  }

  def findByAuthorized(account: Account, clientId: String)(implicit redisPool: RedisClientPool): Option[OauthAccessToken] = {
    redisPool.withClient { client =>
      val tokenIndex: String = s"accessToken:${account.id}#${clientId}"
      client.get(tokenIndex) match {
        case None => None
        case Some(x) => {
          Some(convert(x))
        }
      }
    }
  }

  def findByRefreshToken(refreshToken: String)(implicit redisPool: RedisClientPool): Option[OauthAccessToken] = {
    val refreshTokenKey: String = s"tokens:$refreshToken:refreshToken"
    redisPool.withClient { client =>
      client.get(refreshTokenKey) match {
        case None => None
        case Some(x) => {
          println("findByRefreshToken convert:" + x)
          Some(convert(x))
        }
      }
    }
  }
}
