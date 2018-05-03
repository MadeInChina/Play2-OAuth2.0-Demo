package endpoint

import akka.actor.ActorSystem
import com.redis.RedisClientPool
import com.typesafe.config.ConfigFactory
import org.hrw.login.service.mongodb.{Account, AccountDAO, MongoDB}
import org.hrw.login.service.oauth2
import org.hrw.login.service.oauth2.{OauthAccessToken, OauthClient}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, MustMatchers}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._


class OAuthEndPointInitSpec extends FlatSpec with MockitoSugar with MustMatchers with BeforeAndAfter {
  implicit var accountDAO: AccountDAO = null
  val actorSystem: ActorSystem = ActorSystem.create("test")

  val clientId: String = "oauth2-client-id"
  val clientSecret: String = "oauth2-client-secret"
  val grantType: String = "client_credentials"
  implicit var redisPool: RedisClientPool = null

  var mongoDB: MongoDB = null

  var oAuthEndPoint: OAuthEndPoint = null

  before {
    mongoDB = new MongoDB(ConfigFactory.load("application.test.conf"))
    accountDAO = new AccountDAO(mongoDB)
    redisPool = new RedisClientPool("localhost", 6379)
    oAuthEndPoint = new OAuthEndPoint(null, actorSystem)
  }


  "OAuthEndPoint" should "return 400 without token in header" in {
    val request = FakeRequest()
    val result = oAuthEndPoint.test(request)
    status(result) must be(400)
  }

  "OAuthEndPoint" should "return 401 with invalid token" in {
    val request = FakeRequest().withHeaders("Authorization" -> "Bearer ${accessToken.accessToken}")
    val result = oAuthEndPoint.test(request)
    status(result) must be(401)
  }

  "OAuthEndPoint" should "return user info success" in {
    val account = oauth2.Account(1L, "password", "oauth2@demo.com")
    //Create ${clientId}#${clientSecret}
    val oauthClient = OauthClient(
      id = 0L,
      ownerId = 1L,
      owner = Some(account),
      grantType = grantType,
      clientId = clientId,
      clientSecret = clientSecret,
      redirectUri = None,
      createdAt = 0L)
    val oauthClientForRefreshToken = OauthClient(
      id = 0L,
      ownerId = 1L,
      owner = Some(account),
      grantType = "refresh_token",
      clientId = clientId,
      clientSecret = clientSecret,
      redirectUri = None,
      createdAt = 0L)
    OauthClient.create(oauthClient)
    OauthClient.create(oauthClientForRefreshToken)
    //Create user
    accountDAO.save(Account(userName = "oauth2", password = "password", email = "oauth2@demo.com"))
    //Create oauth access token
    val accessToken = OauthAccessToken.create(account, oauthClient)
    val request = FakeRequest().withHeaders("Authorization" -> s"Bearer ${accessToken.accessToken}")
    val result = oAuthEndPoint.test(request)
    status(result) must be(200)
    new JsonObject(contentAsString(result)).getString("content.email") must be("oauth2@demo.com")
  }

}
