package org.hrw.login.service.oauth2

import com.redis.RedisClientPool
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpecLike}
import redis.embedded.RedisServer


class OauthClientSpec extends WordSpecLike with MustMatchers with BeforeAndAfter {
  val clientId: String = "test-ios"
  val clientSecret: String = "test-ios-client-secret"
  val grantType: String = "client_credentials"
  val redisServer: RedisServer = RedisServer.builder().port(6378).build()
  before {
    redisServer.start()
  }

  after {
    redisServer.stop()
  }

  "OauthClient" should {
    "return false when validate with invalid parameters" in {
      implicit val redisPool = new RedisClientPool("localhost", 6378)

      OauthClient.validate(clientId, clientSecret + "false", grantType) must be(false)
    }

    "return true when validate with valid parameters" in {
      implicit val redisPool = new RedisClientPool("localhost", 6378)
      OauthClient.create(OauthClient(
        id = 0L,
        ownerId = 0L,
        owner = None,
        grantType = grantType,
        clientId = clientId,
        clientSecret = clientSecret,
        redirectUri = None,
        createdAt = 0L))
      OauthClient.validate(clientId, clientSecret, grantType) must be(true)
    }
  }
}
