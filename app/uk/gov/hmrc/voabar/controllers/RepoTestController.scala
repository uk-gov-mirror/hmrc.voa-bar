/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.voabar.controllers

//Temporary class allowing us to exercise the ReportStatusRepository


import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import uk.gov.hmrc.voabar.models.{LoginDetails, ReportStatus}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.voabar.connectors.LegacyConnector
import uk.gov.hmrc.voabar.repositories.ReactiveMongoRepository

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RepoTestController @Inject()(mongo: ReactiveMongoRepository) extends BaseController {
  def executeTest0: Future[Boolean] = {
    val chars = 'A' to 'Z'
    def r = scala.util.Random.nextInt(chars.size)

    val status0 = ReportStatus("sId000", s"status-${System.currentTimeMillis()}-${chars(r)}${chars(r)}")
    mongo.insert(status0)
  }

  def test0(): Action[AnyContent] = Action.async {implicit request =>
    executeTest0 map {
      response =>
        if (response) Ok("Test 0 complete with true") else Ok("Test0 complete with false")
    }
  }

  def executeTest1(): Future[List[ReportStatus]] = {
    mongo.getAll("sId000")
  }

  def test1(): Action[AnyContent] = Action.async {implicit request =>
    executeTest1 map {
      response =>
        Ok("Test 1 complete with " + response)
    }
  }
}
