package org.jianyi.yibuyiqu.servers.socket

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.impl.BufferImpl
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.LocalMap
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions

import org.jianyi.yibuyiqu.command.CommandUtil
import org.jianyi.yibuyiqu.utils.ConfigUtil
import org.jianyi.yibuyiqu.utils.JsonUril

class SockJSServer extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(SockJSServer.class)

	void start() {
		// TODO Auto-generated method stub

		JsonObject cfg = config()
		String servername = cfg.getString(ConfigUtil.NAME)

		def server = vertx.createHttpServer()

		def router = Router.router(vertx)

		def options = new SockJSHandlerOptions().setHeartbeatInterval(2000)

		def sockJSHandler = SockJSHandler.create(vertx, options)
		
		def eb = vertx.eventBus()
		
		sockJSHandler.socketHandler({ sockJSSocket ->
			log.info("A client has connected! SID:"	+ sockJSSocket.writeHandlerID())
			JsonObject log = new JsonObject()
			log.put("type", "客户端连接！")
			log.put("ip",sockJSSocket.remoteAddress().host())
			eb.send("server.log", log)
			LocalMap<String,String> map = vertx.sharedData().getLocalMap(ConfigUtil.ALLUSER)
			Map<String, String> clientMap = new HashMap<String, String>()
			clientMap.put(CommandUtil.CMD_PROXYNAME, servername)
			map.put(sockJSSocket.writeHandlerID(), JsonUril.objectToJsonStr(clientMap))
			sockJSSocket.handler({data ->
				JsonObject message = null
				try {
					message = new JsonObject(data.toString())
					message.put(CommandUtil.CMD_SESSIONID, sockJSSocket.writeHandlerID())
					message.put(CommandUtil.CMD_PROXYNAME, servername)
					eb.send("server.input", message)
				} catch (Exception e) {
					log = new JsonObject()
					log.put(ConfigUtil.VALUE,data.toString())
					log.put(ConfigUtil.TYPE,
							"用户命令-命令格式错误，不是有效的JSON字符串")
					log.put(CommandUtil.CMD_RESULT,
							"用户命令-命令格式错误，不是有效的JSON字符串")
					eb.send("server.log", log)
					eb.send(sockJSSocket.writeHandlerID(), new BufferImpl("用户命令-命令格式错误，不是有效的JSON字符串"))
				}
			})
		})

		router.route(config().getString(ConfigUtil.PREFIX)+"/*").handler(sockJSHandler)

		server.requestHandler(router.&accept)

		server.listen(config().getInteger(ConfigUtil.PORT))

		eb.consumer("server." + servername + ".send", { message ->
			if (message.body().size() > 0) {
				JsonObject msg = message.body()
				String sessionId = msg.getString(CommandUtil.CMD_SESSIONID)
				msg.remove(CommandUtil.CMD_SESSIONID)
				eb.send(sessionId, new BufferImpl(msg.toString()))
			}
		})

	}

}
