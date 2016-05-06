package org.hrw.login.service.oauth2

import com.redis.RedisClientPool
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.native.JsonMethods._


case class OauthAuthorizationCode(
                                   id: Long,
                                   accountId: Long,
                                   account: Option[Account] = None,
                                   oauthClientId: Long,
                                   oauthClient: Option[OauthClient] = None,
                                   code: String,
                                   redirectUri: Option[String],
                                   createdAt: Long)

object OauthAuthorizationCode {
  implicit val formats = Serialization.formats(NoTypeHints)

  private def convert(str: String) = {
    parse(str).extract[OauthAuthorizationCode]
  }


  def findByCode(code: String)(implicit redisPool: RedisClientPool): Option[OauthAuthorizationCode] = {
    redisPool.withClient { client => client.get(s"codes:$code:authorizationCode") match {
      case None => None
      case Some(x) => Some(convert(x))
    }
    }
  }

  def delete(code: String)(implicit redisPool: RedisClientPool): Unit = {
    redisPool.withClient { client => client.del(s"codes:$code:authorizationCode")
    }
  }
}