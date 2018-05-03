package org.hrw.login.service.mongodb

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._


class MongoDBSpec extends FlatSpec with Matchers {
  var conf: Config = null

  "Config" should "get correct value" in {
    conf = ConfigFactory.load("application.test.conf")
    conf.getString("mongodb.address") should be("localhost")

    conf.getString("mongodb.port") should be("12345")

    conf.getString("mongodb.db") should be("test")
  }
}
