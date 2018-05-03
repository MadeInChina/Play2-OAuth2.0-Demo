package org.hrw.login.service.oauth2

import com.redis.RedisClientPool
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._


case class OauthClient(
                        id: Long,
                        ownerId: Long,
                        owner: Option[Account] = None,
                        grantType: String,
                        clientId: String,
                        clientSecret: String,
                        redirectUri: Option[String],
                        createdAt: Long
                      )


object OauthClient {
  implicit val formats = Serialization.formats(NoTypeHints)

  def create(oauthClient: OauthClient)(implicit redisPool: RedisClientPool) = {
    redisPool.withClient { client =>
      val key: String = s"${oauthClient.clientId}#${oauthClient.clientSecret}"
      client.set(key, write(oauthClient))
    }
  }

  def validate(clientId: String, clientSecret: String, grantType: String)(implicit redisPool: RedisClientPool): Boolean = {
    println(s"Oauth validate")
    redisPool.withClient { client =>
      val key: String = s"${clientId}#${clientSecret}"
      println(s"Oauth $key")
      client.get(key) match {
        case None => {
          println(s"Oauth $key is invalid")
          false
        }
        case Some(x) => convert(x).grantType == grantType || grantType == "refresh_token"
      }
    }
  }

  def findByClientId(clientId: String)(implicit redisPool: RedisClientPool): Option[OauthClient] = {
    redisPool.withClient { client =>
      val key: String = s"${clientId}"
      client.get(key) match {
        case None => None
        case Some(x) => Some(convert(x))
      }
    }
  }

  private def convert(str: String) = {
    parse(str).extract[OauthClient]
  }

  def findClientCredentials(clientId: String, clientSecret: String)(implicit redisPool: RedisClientPool): Option[Account] = {
    redisPool.withClient { client =>
      val key: String = s"${clientId}#${clientSecret}"
      client.get(key) match {
        case None => None
        case Some(x) => val oauthClient = convert(x)
          if (oauthClient.grantType == "client_credentials") {
            oauthClient.owner
          }
          else {
            None
          }
      }
    }
  }
}