package com.teqbahn.caseclasses

import com.teqbahn.global.{GlobalConstants}

case class GameLevel(id: String, name: String, image: GameFileObject, color: String, sortOrder: Integer)

case class Theme(id: String, name: String, image: GameFileObject, themeType: Option[String] = Option(GlobalConstants.STATIC))

case class GameFileObject(id: String, title: String, fileName: String, fileType: String)
