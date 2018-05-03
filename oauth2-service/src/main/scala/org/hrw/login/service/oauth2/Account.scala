package org.hrw.login.service.oauth2

import org.hrw.login.service.mongodb.AccountDAO


case class Account(id: Long, password: String, email: String)

object Account {
  def authenticate(userName: String, password: String)(implicit accountDAO: AccountDAO): Option[Account] = {
    accountDAO.findByUserNameAndPassword(userName, password).map(u => {
      Account(id = u.userId, password = u.password, email = u.email)
    })
  }
}