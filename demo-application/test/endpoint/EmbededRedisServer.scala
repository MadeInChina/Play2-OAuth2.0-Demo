package endpoint

import redis.embedded.RedisServer

trait EmbededRedisServer {

 }

object EmbededRedisServer{
  val redisServer: RedisServer = RedisServer.builder().port(6378).build()
}