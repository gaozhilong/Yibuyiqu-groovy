package org.jianyi.yibuyiqu.servers.session

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.LocalMap

import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.utils.ConfigUtil
import org.jianyi.yibuyiqu.utils.JsonUril

import com.google.common.collect.Maps
import com.hazelcast.core.HazelcastInstance

class DefaultSessionManager extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(DefaultSessionManager.class);

	private HazelcastInstance hazelcastInstance

	void start() {
		hazelcastInstance = Cache.getHazelcastInstance()
		def address = config().getString(ConfigUtil.ADDRESS)
		def eb = vertx.eventBus()
		eb.consumer(address + "create", { message ->
			if (create(message)) {
				message.reply(true)
			} else {
				message.reply(false)
			}
		})

		eb.consumer(address + "get", { message ->
			JsonObject val = get(message)
			if (val != null) {
				message.reply(val)
			} else {
				JsonObject msg = new JsonObject()
				msg.put("sessionID", "null")
				message.reply(msg)
			}
		})

		eb.consumer(address + "clear", { message ->
			if (clear(message)) {
				message.reply(true)
			} else {
				message.reply(false)
			}
		})

		eb.consumer(address + "destroy", { message ->
			if (destroy(message)) {
				message.reply(true)
			} else {
				message.reply(false)
			}
		})
	}



	private create(message) {
		LocalMap<String, String> map = vertx.sharedData().getLocalMap(ConfigUtil.USER)
		Map<String, String> clientMap = Maps.newHashMap()
		clientMap.put(CommandUtil.CMD_PROXYNAME,
				message.body().getString(CommandUtil.CMD_PROXYNAME))
		String jsons = JsonUril.objectToJsonStr(clientMap)
		map.put(message.body().getString(CommandUtil.CMD_SESSIONID), jsons)
		
		hazelcastInstance.getMap(Cache.SESSIONS).put(
				message.body().getString(CommandUtil.CMD_SESSIONID), message.body().getMap())
		return true;
	}

	private get(message) {
		JsonObject sessionVal = new JsonObject(hazelcastInstance.getMap(Cache.SESSIONS).get(message.body().getString(CommandUtil.CMD_SESSIONID)));
		return sessionVal;
	}

	private clear(message) {
		LocalMap<String, String> map = vertx.sharedData().getLocalMap(ConfigUtil.USER);
		map.remove(message.body().getString(CommandUtil.CMD_SESSIONID));
		hazelcastInstance.getMap(Cache.SESSIONS).remove(message.body().getString(CommandUtil.CMD_SESSIONID));
		return true;
	}

	private destroy(message) {
		LocalMap<String, String> map = vertx.sharedData().getLocalMap(ConfigUtil.USER);
		map.remove(message.body().getString(CommandUtil.CMD_SESSIONID));
		hazelcastInstance.getMap(Cache.SESSIONS).remove(message.body().getString(CommandUtil.CMD_SESSIONID));
		return true;
	}
}
