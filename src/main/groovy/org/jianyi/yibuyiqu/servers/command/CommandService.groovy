package org.jianyi.yibuyiqu.servers.command

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.LocalMap

import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.utils.ConfigUtil

class CommandService extends AbstractVerticle {
	
	static final Logger log = LoggerFactory.getLogger(CommandService.class)
	
	void start() {
		JsonObject cfg = config()
		String address = cfg.getString(ConfigUtil.ADDRESS)
		def eb = vertx.eventBus()
		//eb.consumer(address, { message -> execute(message) })
		eb.consumer(address, { message -> executeWithLog(message, cfg) })
	}
	
	//发送消息到制定客户端
	def sendMsg(msg) {
		LocalMap<String,String> map = vertx.sharedData().getLocalMap(ConfigUtil.ALLUSER)
		JsonObject json = new JsonObject(map.get(msg.getString(CommandUtil.CMD_SESSIONID)))
		vertx.eventBus().send("server."+json.getString(CommandUtil.CMD_PROXYNAME)+".send", msg)
	}
	
	def execute(message) {}
	
	def executeWithLog(message, cfg) {
		long startTime = System.nanoTime()
		execute(message)
		long endTime = System.nanoTime()
		long duration = endTime - startTime
		JsonObject log = new JsonObject()
		log.put(ConfigUtil.NAME, cfg.getString(ConfigUtil.NAME))
		log.put(ConfigUtil.VERTILEFILE, cfg.getString(ConfigUtil.VERTILEFILE))
		log.put(ConfigUtil.DURATION, duration)
		vertx.eventBus().send("server.log.runtime",	log)
	}
	
	

}
