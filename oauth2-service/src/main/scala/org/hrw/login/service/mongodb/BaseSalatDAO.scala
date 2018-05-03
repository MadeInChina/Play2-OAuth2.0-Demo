package org.hrw.login.service.mongodb

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import salat.dao.{SalatDAO, SalatMongoCursor}

abstract class BaseSalatDAO[ObjectType <: AnyRef : Manifest](collection: MongoCollection) extends SalatDAO[ObjectType, ObjectId](collection) {
  def find(): SalatMongoCursor[ObjectType] = find("_id" $exists true)

  def findById(id: ObjectId) = findOneById(id)

  def findByIds(ids: Set[ObjectId]) = if (ids.isEmpty) Nil else find("_id" $in ids)

  /** Update the documents matching a query with the given update and return the update result */
  def updateOne(query: MongoDBObject, updateObject: MongoDBObject, upSert: Boolean = false): WriteResult = {
    update(query, updateObject, upSert, false)
  }

  def updateOne(query: MongoDBObject, updateObject: ObjectType, upSert: Boolean): WriteResult = {
    update(query, updateObject, upSert, false, new WriteConcern)

  }
}

