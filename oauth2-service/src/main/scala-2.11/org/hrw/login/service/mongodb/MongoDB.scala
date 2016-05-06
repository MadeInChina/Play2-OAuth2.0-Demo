package org.hrw.login.service.mongodb

import com.google.inject.{Inject, Singleton}
import com.mongodb.ServerAddress
import com.mongodb.casbah.Imports._
import com.typesafe.config.Config

@Singleton
class MongoDB @Inject()(config: Config) {

  private val address = config.getString("mongodb.address")
  private val port = config.getInt("mongodb.port")
  private val db = config.getString("mongodb.db")

  private val client = MongoClient(List(new ServerAddress(address, port)), MongoClientOptions(autoConnectRetry = true))

  def apply(collection: String): MongoCollection = client(db)(collection)
}