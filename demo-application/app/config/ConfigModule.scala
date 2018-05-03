package config

import com.google.inject.{Inject, Provider}
import com.redis.RedisClientPool
import com.typesafe.config.{Config, ConfigFactory}
import play.api.inject._
import play.api.{Configuration, Environment}

class ConfigModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Config].to(ConfigFactory.load()).eagerly(),
      bind[RedisClientPool].toProvider[RedisPoolProvider].eagerly()
    )
  }
}

class RedisPoolProvider @Inject()(config: Config) extends Provider[RedisClientPool] {
  private val address = config.getString("redis.address")
  private val port = config.getInt("redis.port")

  private val client = new RedisClientPool(address, port)

  override def get(): RedisClientPool = client
}