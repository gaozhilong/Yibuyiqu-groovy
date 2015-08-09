package org.jianyi.yibuyiqu.servers.group

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.command.Result
import org.jianyi.yibuyiqu.group.Group
import org.jianyi.yibuyiqu.utils.ConfigUtil

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap

class GroupServer extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(GroupServer.class)

	private HazelcastInstance hazelcastInstance

	String name

	void start() {

		hazelcastInstance = Cache.getHazelcastInstance()

		JsonObject groupCfg = config()

		Group group = new Group(groupCfg)
		name = group.name

		IMap<String,Group> map = hazelcastInstance.getMap(Cache.GROUPS)
		if (!map.containsKey(name)) {
			map.put(name, group)
		}

		def eb = vertx.eventBus()

		eb.consumer("group." + name + ".broadcast", { message ->
			if (message.body().size() > 0) {
				Result result = sendMsg(msg)
				message.reply(result.toJsonObject())
			}
		})

		eb.consumer("group." + name + ".regist", { message ->
			if (message.body().size() > 0) {
				Result result = regist(message)
				message.reply(result.toJsonObject())
			}
		})

		eb.consumer("group." + name + ".unregist", { message ->
			if (message.body().size() > 0) {
				Result result = unregist(message)
				message.reply(result.toJsonObject())
			}
		})
	}

	private regist(message) {
		def sessionID = message.body().getString(CommandUtil.CMD_SESSIONID)
		Map<String, String> session = hazelcastInstance.getMap(Cache.SESSIONS).get(sessionID)
		def username = session.get(ConfigUtil.USERNAME)
		Group group = hazelcastInstance.getMap(Cache.GROUPS).get(name)
		Set<String> sessions = group.objects.get(Group.SESSIONIDS)
		sessions.add(sessionID)
		group.objects.put(Group.SESSIONIDS, sessions)

		Set<String> users = group.objects.get(Group.USERS)
		users.add(username)
		hazelcastInstance.getMap(Cache.GROUPS).get(name).objects.put(Group.USERS, users)
		Result result = new Result(sessionID, "Group注册成功", Result.SUCCESS)
		return result
	}


	private unregist(message) {
		def sessionID = message.body().getString(CommandUtil.CMD_SESSIONID)
		Map<String, String> session = hazelcastInstance.getMap(Cache.SESSIONS).get(sessionID)
		def username = session.get(ConfigUtil.USERNAME)
		Group group = hazelcastInstance.getMap(Cache.GROUPS).get(name)
		Set<String> sessions = group.objects.get(Group.SESSIONIDS)
		Result result
		if (sessions.contains(sessionID)) {
			sessions.remove(sessionID)
			group.objects.put(Group.SESSIONIDS, sessions)
			Set<String> users = group.objects.get(Group.USERS)
			users.remove(username)
			group.objects.put(Group.USERS, users)
			result = new Result(sessionID, "Group注销成功", Result.SUCCESS)
		} else {
			result = new Result(sessionID, "Group:"+name+"中没有注册当前用户", Result.SUCCESS)
		}
		return result
	}

	public sendMsg(msg) {
		def sessionID = message.body().getString(CommandUtil.CMD_SESSIONID)
		Set<String> sessions = hazelcastInstance.getMap(Cache.GROUPS).get(name).objects.get(Group.SESSIONIDS)
		if (sessions != null) {
			sessions.each {
				vertx.eventBus().send("server."+hazelcastInstance.getMap(Cache.SESSIONS).get(it).get(CommandUtil.CMD_PROXYNAME)+".send", msg)
			}
		}
		Result result = new Result(sessionID, "Group:"+name+",消息广播成功", Result.SUCCESS)
		return result
	}

}
