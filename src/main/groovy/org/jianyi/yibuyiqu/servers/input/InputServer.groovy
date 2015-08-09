package org.jianyi.yibuyiqu.servers.input

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.LocalMap

import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.command.Result
import org.jianyi.yibuyiqu.utils.ConfigUtil

import rx.Observable
import rx.functions.Action1

class InputServer extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(InputServer.class)

	public void start() {

		JsonObject cfg = config()
		String address = cfg.getString(ConfigUtil.ADDRESS)
		boolean isAuthor = cfg.getBoolean(ConfigUtil.AUTHOR)

		def eb = vertx.eventBus()
		eb.consumer(address, { message ->
			log.info("server.input")
			JsonObject command = message.body()
			boolean author = true
			String cmdType = command.getString(CommandUtil.COMMAND_TYPE)
			if (isAuthor) {
				LocalMap<String,String> users = vertx.sharedData().getLocalMap(ConfigUtil.ALLUSER)
				if (!users.keySet().contains(command.getString(CommandUtil.CMD_SESSIONID))) {
					if (cmdType != null && cmdType != "login") {
						author = false
					}
				}
			}

			if (author) {
				LocalMap<String, String> commandMap = vertx.sharedData().getLocalMap(ConfigUtil.COMMANDS)
				Observable.just(command).subscribe(
						new Action1<JsonObject>() {
							@Override
							public void call(JsonObject commandObj) {
								String sessionId = commandObj.getString(CommandUtil.CMD_SESSIONID)
								String type = commandObj.getString(CommandUtil.COMMAND_TYPE)
								if (type != null
								&& commandMap.keySet().contains(type)) {
									logCall(commandObj)
									eb.send(
											commandMap.get(type),
											commandObj,{ reply ->
												log(reply.result().body())
											}
											)
								} else {
									String msg = "命令没有相应的处理服务"
									if (type == null) {
										msg = "命令类型为空"
									}
									Result result = new Result(
											sessionId, msg, Result.ERROR)
									log(result)
									sendMsg(result.toJsonObject())
								}
							}
						})
			} else {
				String sessionId = command.getString(CommandUtil.CMD_SESSIONID)
				Result result = new Result(
						command.getString(CommandUtil.CMD_SESSIONID), "没有登录", Result.ERROR)
				log(result)
				sendMsg(result.toJsonObject())
			}
		})
	}

	def private logCall(message) {
		JsonObject log = new JsonObject()
		String type = message.getString(CommandUtil.COMMAND_TYPE)
		String sessionId = message.getString(CommandUtil.CMD_SESSIONID)
		log.put(ConfigUtil.TYPE, type)
		//log.putString("username", message.getString("username"))
		log.put(CommandUtil.CMD_SESSIONID, sessionId)
		log.put(CommandUtil.CMD_RESULT,	"call")
		vertx.eventBus().send("server.log", log)
	}

	def private log(Result result) {
		JsonObject log = new JsonObject()
		log.put(ConfigUtil.TYPE, "用户命令-" + result.getResult())
		log.put(CommandUtil.CMD_SESSIONID, result.getSessionID())
		log.put(CommandUtil.CMD_RESULT, result.getMessage())
		vertx.eventBus().send("server.log",	log)
	}

	def private log(JsonObject result) {
		JsonObject log = new JsonObject()
		log.put(ConfigUtil.TYPE, "用户命令执行结果")
		log.put(CommandUtil.CMD_SESSIONID, result.getString(CommandUtil.CMD_SESSIONID))
		log.put(CommandUtil.CMD_RESULT, result.getString(CommandUtil.CMD_RESULT))
		log.put(CommandUtil.CMD_MESSAGE, result.getString(CommandUtil.CMD_MESSAGE))
		vertx.eventBus().send("server.log",	log)
	}

	def sendMsg(JsonObject msg) {
		LocalMap<String,String> map = vertx.sharedData().getLocalMap(ConfigUtil.ALLUSER)
		JsonObject json = new JsonObject(map.get(msg.getString(CommandUtil.CMD_SESSIONID)))
		vertx.eventBus().send("server."+json.getString(CommandUtil.CMD_PROXYNAME)+".send", msg)
	}
}
