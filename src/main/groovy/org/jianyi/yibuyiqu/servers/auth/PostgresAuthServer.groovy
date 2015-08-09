package org.jianyi.yibuyiqu.servers.auth

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

import java.sql.ResultSet
import java.sql.Statement

import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.db.PostgresDataSource
import org.jianyi.yibuyiqu.utils.ConfigUtil
import org.jianyi.yibuyiqu.utils.JsonUril

class PostgresAuthServer extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(PostgresAuthServer.class)

	private PostgresDataSource datasource

	void start() {
		datasource = PostgresDataSource.getInstance()
		def eb = vertx.eventBus()

		eb.consumer("server.author.login", { message -> login(message) })
		eb.consumer("server.author.logout", { message -> logout(message) })
		eb.consumer("server.author.authorise", { message -> authorise(message) })
	}

	void stop() {
	}

	def private findUser(username, password) {
		Map<String, Object> usr = null
		StringBuffer sql=new StringBuffer("SELECT jdoc FROM \"User\" WHERE jdoc @> '{\"username\": \"")
		sql.append(username)
		sql.append("\"}' and jdoc @> '{\"password\": \"")
		sql.append(password)
		sql.append("\"}'")
		Statement stmt=null
		ResultSet rs=null
		stmt=datasource.getConnection().createStatement()
		rs=stmt.executeQuery(sql.toString())
		while(rs.next()){
			usr = JsonUril.jsonToMapObject(rs.getString(1))
		}
		return usr
	}

	def private void login(message) {
		String sessionID = message.body().getString(CommandUtil.CMD_SESSIONID)
		String username = message.body().getString(ConfigUtil.USERNAME)
		String password = message.body().getString(ConfigUtil.PASSWORD)
		String proxy = message.body().getString(CommandUtil.CMD_PROXYNAME)
		if (username != null || password != null) {
			Map<String, Object> user = findUser(username, password)
			if (user != null) {
				// 判断是否已经登录
				JsonObject userinfo = new JsonObject()
				userinfo.put(ConfigUtil.USERNAME, username)
				/*vertx.eventBus().send("server.session.getbyusername", userinfo,
						{ reply ->
							// TODO Auto-generated method stub
							if (reply != null && reply.succeeded() && reply.result().body().getString(
							"sessionID") != null && !reply.result().body().getString(
							"sessionID").equals("null")) {
								// 清除原有登录信息
								JsonObject session = new JsonObject()
								session.put("sessionID", reply.result().body().getString("sessionID"))
								vertx.eventBus().send("server.session.destroy", session,
										{ ry ->
											// TODO Auto-generated method stub
											if (ry.result().body().asBoolean()) {
												log.info("Session信息销毁成功")
											} else {
												log.info("Session信息销毁失败！")
											}
										}
										)
							}
						}
						)*/

				JsonObject session = new JsonObject()
				session.put(CommandUtil.CMD_SESSIONID, sessionID)
				session.put(ConfigUtil.USERNAME, username)
				JsonObject sessionVal = new JsonObject()
				sessionVal.put(ConfigUtil.USERNAME, username)
				session.put(ConfigUtil.SESSION_VAL, sessionVal.toString())
				session.put(ConfigUtil.CREATE_TIME, String.valueOf(System.nanoTime()))
				session.put(CommandUtil.CMD_PROXYNAME, proxy)
				
				vertx.eventBus().send("server.session.create", session,
						{ reply ->
							// TODO Auto-generated method stub
							if (reply.succeeded() && reply.result().body().asBoolean()) {
								log.info("Session信息存储成功")
								message.reply(true)
							} else {
								log.info("Session信息存储失败！")
								message.reply(false)
							}
						}
						)

			}
		}
	}

	def private logout(message) {
		JsonObject session = new JsonObject()
		session.put(CommandUtil.CMD_SESSIONID, message.body().getString(CommandUtil.CMD_SESSIONID))
		vertx.eventBus().send("server.session.destroy", session,
				{ reply ->
					// TODO Auto-generated method stub
					if (reply.succeeded() && reply.result().body().asBoolean()) {
						message.reply(true)
						log.info("Session信息销毁成功")
					} else {
						message.reply(false)
						log.info("Session信息销毁失败！")
					}
				}
				)
	}

	def private authorise(message) {
		JsonObject session = new JsonObject()
		session.put(CommandUtil.CMD_SESSIONID, message.body().getString(CommandUtil.CMD_SESSIONID))
		vertx.eventBus().send("server.session.get", session,
				{ reply ->
					// TODO Auto-generated method stub
					if (reply.result().body().getString(CommandUtil.CMD_SESSIONID) != null && !reply.result().body().getString(CommandUtil.CMD_SESSIONID).equals("null")) {
						message.reply(true)
					} else {
						message.reply(false)
					}
				}
				)
	}

}
