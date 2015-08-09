package org.jianyi.yibuyiqu.servers.command

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.command.Result

class LoginCommandService extends CommandService {
	
	def execute(message) {
		// TODO Auto-generated method stub
		log.info("execute Login Command")
		vertx.eventBus().send("server.author.login", message.body(), {
					reply -> 
						if (reply.result().body().asBoolean()) {
							Result result = new Result(message.body().getString(CommandUtil.CMD_SESSIONID),
									"登录成功", Result.SUCCESS)
							message.reply(result.toJsonObject())
							sendMsg(result.toJsonObject())
						} else {
							Result result = new Result(message.body().getString(CommandUtil.CMD_SESSIONID),
									"登录失败", Result.ERROR)
							message.reply(result.toJsonObject())
							sendMsg(result.toJsonObject())
						}
					}
				)

	}
}
