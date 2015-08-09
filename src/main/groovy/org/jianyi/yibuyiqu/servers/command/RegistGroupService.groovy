package org.jianyi.yibuyiqu.servers.command

import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger

import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.command.Result
import org.jianyi.yibuyiqu.group.Group

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap

class RegistGroupService extends CommandService {

	public static final String GROUP="group"
	private HazelcastInstance hazelcastInstance

	def execute(message) {
		log.info("execute RegistGroup Command")
		hazelcastInstance = Cache.getHazelcastInstance()
		JsonObject msg = message.body()
		def name = msg.getString(GROUP)
		Result result
		IMap<String,Group> map = hazelcastInstance.getMap(Cache.GROUPS)
		if (!map.containsKey(name)) {
			result = new Result(msg.getString(CommandUtil.CMD_SESSIONID), "Group名称:"+name+"不存在", Result.ERROR)
			sendMsg(result.toJsonObject())
		} else {
			vertx.eventBus().send("group." + name + ".regist", msg, { reply ->
				if (reply.result().body() != null) {
					result = new Result(msg.getString(CommandUtil.CMD_SESSIONID), "Group命令:"+msg.getString(CommandUtil.COMMAND_TYPE)+"执行成功", Result.ERROR)
					sendMsg(reply.result().body())
				} else {
					result = new Result(msg.getString(CommandUtil.CMD_SESSIONID), "Group命令:"+msg.getString(CommandUtil.COMMAND_TYPE)+"执行失败", Result.ERROR)
					sendMsg(result.toJsonObject())
				}
			})
		}
		message.reply(result.toJsonObject())
	}
}
