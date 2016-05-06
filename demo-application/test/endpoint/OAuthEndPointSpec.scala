package endpoint

import akka.actor.ActorSystem
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import scala.collection.JavaConverters._
import com.jayway.restassured.path.json.JsonPath
import com.redis.RedisClientPool
import com.typesafe.config.ConfigFactory
import de.flapdoodle.embed.mongo.distribution.Version
import org.hrw.login.service.mongodb.{Account, AccountDAO, MongoDB}
import org.hrw.login.service.oauth2
import org.hrw.login.service.oauth2.{OauthAccessToken, OauthClient}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpec, MustMatchers}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import redis.embedded.RedisServer


class OAuthEndPointSpec extends FlatSpec with MockitoSugar with MongoEmbedDatabase with MustMatchers with BeforeAndAfter {
  implicit var accountDAO: AccountDAO = null
  val actorSystem: ActorSystem = ActorSystem.create("test")

  val clientId: String = "test-ios"
  val clientSecret: String = "test-ios-client-secret"
  val grantType: String = "client_credentials"
  val redisServer: RedisServer = RedisServer.builder().port(6378).build()
  implicit var redisPool: RedisClientPool = null
  // by default port = 12345 & version = Version.2.3.0
  // add your own port & version parameters in mongoStart method if you need it
  var mongoProps: MongodProps = null

  var mongoDB: MongoDB = null

  var oAuthEndPoint: OAuthEndPoint = null

  before {
    mongoDB = new MongoDB(ConfigFactory.load("application.test.conf"))
    accountDAO = new AccountDAO(mongoDB)
    redisPool = new RedisClientPool("localhost", 6378)
    mongoProps = mongoStart(version = Version.V2_6_1)
    redisServer.start()

    oAuthEndPoint = new OAuthEndPoint(actorSystem)
  }

  after {
    mongoStop(mongoProps)
    redisServer.stop()
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
    val account = oauth2.Account(1L, "password", "test1@11.com")
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
    OauthClient.create(oauthClient)
    //Create user
    accountDAO.save(Account(userName = "test1", password = "password", email = "test1@11.com"))
    //Create oauth access token
    val accessToken = OauthAccessToken.create(account, oauthClient)
    val request = FakeRequest().withHeaders("Authorization" -> s"Bearer ${accessToken.accessToken}")
    val result = oAuthEndPoint.test(request)
    status(result) must be(200)
    new JsonObject(contentAsString(result)).getString("content.email") must be("test1@11.com")
  }

}

class JsonObject(in: String)  {
  val json = in
  val ctx = new JsonPath(json)

  def get[T](path: String): T = {
    ctx.get[T](path)
  }

  def get[T](): T = {
    ctx.get()
  }

  def getBoolean(path: String): Boolean = {
    ctx.getBoolean(path)
  }

  def getByte(path: String): Byte = {
    ctx.getByte(path)
  }

  def getChar(path: String): Char = {
    ctx.getChar(path)
  }

  def getDouble(path: String): Double = {
    ctx.getDouble(path)
  }

  def getFloat(path: String): Float = {
    ctx.getFloat(path)
  }

  def getInt(path: String): Int = {
    ctx.getInt(path)
  }

  def getJsonObject[T](path: String): T = {
    ctx.getJsonObject[T](path)
  }

  def getList[T](path: String): List[T] = {
    ctx.getList[T](path).asScala.toList
  }

  def getList[T](path: String, clazz: Class[T]): List[T] = {
    ctx.getList[T](path, clazz).asScala.toList
  }

  def getLong(path: String): Long = {
    ctx.getLong(path)
  }

  def getMap[K, V](path: String): Map[K, V] = {
    ctx.getMap[K, V](path).asScala.toMap
  }

  def getMap[K, V](path: String, keyType: Class[K], valueType: Class[V]): Map[K, V] = {
    ctx.getMap[K, V](path, keyType, valueType).asScala.toMap
  }

  def getObject[T](path: String, clazz: Class[T]): T = {
    ctx.getObject[T](path, clazz)
  }

  def getShort(path: String): Short = {
    ctx.getShort(path)
  }

  def getString(path: String): String = {
    ctx.getString(path)
  }


  override def toString = {
    json
  }
}
