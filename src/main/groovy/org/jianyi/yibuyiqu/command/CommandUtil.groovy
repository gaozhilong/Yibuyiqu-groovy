package org.jianyi.yibuyiqu.command

import io.vertx.core.json.JsonObject

class CommandUtil {
	
	static final String COMMAND_TYPE = "command"
	
	static final String CMD_SESSIONID = "sessionID"
	
	static final String CMD_PROXYNAME = "proxy"
	
	static final String CMD_RESULT = "result"
	
	static final String CMD_MESSAGE = "message"
	
	def static getCommands(message) {
		JsonObject cmd = new JsonObject(message)
		return cmd
	}
	
	def static getCommands(message, sessionId) {
		JsonObject cmd = new JsonObject(message)
		cmd.put("sessionID", sessionId)
		return cmd
	}	

}
