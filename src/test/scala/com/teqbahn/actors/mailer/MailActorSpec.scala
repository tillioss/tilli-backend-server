package com.teqbahn.actors.mailer

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike

class MailActorSpec extends TestKit(ActorSystem("MailActorSpec"))
  with ImplicitSender
  with AnyFlatSpecLike
  with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  // Your test cases for MailActor can be written here
}
