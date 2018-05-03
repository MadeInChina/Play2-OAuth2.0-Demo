package org.hrw.login.service

import salat.Context


package object mongodb {

  implicit val ctx = new Context {
    val name = "REMAPPED_ID"
  }

  ctx.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
}
