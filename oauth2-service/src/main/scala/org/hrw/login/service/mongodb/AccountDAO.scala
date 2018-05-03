package org.hrw.login.service.mongodb

import com.google.inject.{Inject, Singleton}
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId

@Singleton
class AccountDAO @Inject()(db: MongoDB) extends BaseSalatDAO[Account](db("users")) {
  def findByUserNameAndPassword(userName: String, password: String): Option[Account] = {
    findOne(MongoDBObject("userName" -> userName, "password" -> password))
  }
}

case class Account
(
  id: ObjectId = new ObjectId(),
  birthday: String = "",
  description: String = "",
  gender: Long = -1L,
  gps_at: Long = 0L,
  avatar: String = "",
  latitude: Double = 0.0,
  longitude: Double = 0.0,
  likeNumber: Long = 0L,
  nickName: String = "",
  password: String = "",
  phoneNumber: String = "",
  register_at: Long = 0L,
  var userId: Long = 0L,
  userName: String = "",
  userSearch: Long = 1L,
  userShown: Long = 1L,
  recommendFriends: Long = 1L,
  hobby: String = "",
  forbidGeogInfo: List[String] = Nil,
  contacts: List[String] = Nil,
  background: String = "",
  likeUserNumber: Int = 0, email: String = "",
  signature: String = "",
  searchedByThird: Int = 1,
  hideUsers: String = "",
  unShareUsers: String = "",
  findMeByRadar: Int = 1,
  isOnline: Boolean = false,
  emp: String = ""
)