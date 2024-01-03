package com.github.merelin.util.finance.bb

import java.net.URLEncoder
import java.nio.charset.Charset
import sttp.client4.quick._
import sttp.client4.Response
import com.github.merelin.util.json.JsonConverter

import java.io.FileReader
import java.util.Properties
import scala.io.StdIn


case class PhoneNumberRequest(clientId: String, phoneNumber: String)

case class PhoneNumberResponseValuePayload(codeTtlSeconds: Int)
case class PhoneNumberResponseValue(confirmToken: String, payload: PhoneNumberResponseValuePayload)
case class PhoneNumberResponse(value: PhoneNumberResponseValue, isSuccess: Boolean, error: String)

case class VerifyCodeRequest(clientId: String, confirmToken: String, code: String)

case class VerifyCodeResponseValue(confirmToken: String)
case class VerifyCodeResponse(value: VerifyCodeResponseValue, isSuccess: Boolean, error: String)

case class VerifyPasswordRequest(clientId: String, confirmToken: String, password: String)

case class VerifyPasswordResponseValue(exchangeToken: String)
case class VerifyPasswordResponse(value: VerifyPasswordResponseValue, isSuccess: Boolean, error: String)

case class TokensResponse(accessToken: String, expiresIn: Int, tokenType: String, refreshToken: String, scope: String)

case class TokensErrorResponse(error: String)

object Authenticator {
  val props: Properties = new Properties
  props.load(new FileReader("props.conf"))

  val identityHost: String = props.getProperty("IdentityHost")
  val apiBaseUrl: String = s"https://${identityHost}/api"

  val authBaseUrl: String = s"${apiBaseUrl}/sso/v2/open-banking/auth"
  val authLoginUrl: String = s"${authBaseUrl}/third-party/login"
  val authVerifyCodeUrl: String = s"${authBaseUrl}/third-party/verifyCode"
  val authVerifyPasswordUrl: String = s"${authBaseUrl}/third-party/2fa"

  val connectTokenUrl: String = s"${apiBaseUrl}/sso/connect/token"

  val clientId: String = props.getProperty("ClientID")

  val scope: String = "offline_access openid open-banking:payments open-banking:accounts"
  val grantTypeThirdParty: String = "open_banking:third_party"

  def urlEncode(s: String): String = URLEncoder.encode(s, Charset.forName("UTF-8"))

  def request(contentType: String, url: String, data: String): Response[String] = {
    quickRequest
      .header("Content-Type", contentType)
      .post(uri"${url}")
      .body(data)
      .send()
  }

  def enterPhoneNumber(): PhoneNumberResponse = {
    val phoneNumber = props.getProperty("PhoneNumber")
    val json = JsonConverter.writeTo(PhoneNumberRequest(clientId, phoneNumber))
    val response = request("application/json", authLoginUrl, json)
    JsonConverter.readFrom[PhoneNumberResponse](response.body)
  }

  def verifyCode(confirmToken: String): VerifyCodeResponse = {
    val code = StdIn.readLine("Code: ")
    val json = JsonConverter.writeTo(VerifyCodeRequest(clientId, confirmToken, code))
    val response = request("application/json", authVerifyCodeUrl, json)
    JsonConverter.readFrom[VerifyCodeResponse](response.body)
  }

  def verifyPassword(confirmToken: String): VerifyPasswordResponse = {
    val password = props.getProperty("Password")
    val json = JsonConverter.writeTo(VerifyPasswordRequest(clientId, confirmToken, password))
    val response = request("application/json", authVerifyPasswordUrl, json)
    JsonConverter.readFrom[VerifyPasswordResponse](response.body)
  }

  def requestTokens(exchangeToken: String): Either[TokensErrorResponse, TokensResponse] = {
    val scopeEncoded = urlEncode(scope)
    val grantTypeEncoded = urlEncode(grantTypeThirdParty)
    val data = s"client_id=${clientId}&scope=${scopeEncoded}&grant_type=${grantTypeEncoded}&exchange_token=${exchangeToken}"
    val response = request("application/x-www-form-urlencoded", connectTokenUrl, data)
    response.code.code match {
      case c if c >= 400 =>
        Left(JsonConverter.readFrom[TokensErrorResponse](response.body))
      case _ =>
        Right(JsonConverter.readFrom[TokensResponse](response.body))
    }
  }

  def authenticate(): Unit = {
    val phoneNumber = enterPhoneNumber()
    if (phoneNumber.isSuccess) {
      val code = verifyCode(phoneNumber.value.confirmToken)
      if (code.isSuccess) {
        val password = verifyPassword(code.value.confirmToken)
        if (password.isSuccess) {
          val tokens = requestTokens(password.value.exchangeToken)
          tokens match {
            case Left(error) =>
              println(s"error: ${error.error}")
            case Right(tokens) =>
              println(s"tokens: ${tokens}")
          }
        }
      }
    }
  }
}
