package org.jianyi.yibuyiqu

import groovy.json.JsonSlurper
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.shareddata.LocalMap

import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.utils.ConfigUtil
import org.jianyi.yibuyiqu.utils.JsonUril

import com.hazelcast.core.IMap

def logger = LoggerFactory.getLogger("Applaction")

JsonObject  httpserverCfg, sockJsCfg, proxyCfg, serverCfg, commandCfg

hazelcastInstance = Cache.getHazelcastInstance()

def cfg = this.getClass().getResource(ConfigUtil.CONFIGFILE).text
def appCfg = new JsonSlurper().parseText(cfg)

// TODO Auto-generated method stub
vertx = Vertx.vertx()
IMap<String,Object> map = hazelcastInstance.getMap(Cache.CONFIG)
Map<String, Object> maps = JsonUril.jsonToMapObject(cfg)
maps.each { k, v ->
	map.put(k, v)
}

appCfg.server.each {
	def  vcfg = JsonUril.objectToJson(it)
	def config = vcfg.getMap()
	def options = [ 
		"config" : config,
		"worker" : vcfg.getBoolean(ConfigUtil.WORKER),
		"instances" : vcfg.getInteger(ConfigUtil.INSTANCES)
	]
	
	vertx.deployVerticle(vcfg.getString(ConfigUtil.VERTILEFILE),options)
	
	vertx.deployVerticle(vcfg.getString(ConfigUtil.VERTILEFILE), options, { asyncResult ->
		if (asyncResult.succeeded()) {
			logger.info("The "+vcfg.getString(ConfigUtil.VERTILEFILE)+" Server verticle has been deployed, deployment ID is "
					+ asyncResult.result());
		} else {
			asyncResult.cause().printStackTrace();
		}
	})
}


appCfg.proxyserver.each {
	def vcfg = JsonUril.objectToJson(it)
	def config = vcfg.getMap()
	def options = [
		"config" : config,
		"worker" : false,
		"instances" : 1
	]
	
	vertx.deployVerticle("org.jianyi.yibuyiqu.servers.socket.SockJSServer", options)
	vertx.deployVerticle("org.jianyi.yibuyiqu.servers.socket.SockJSServer", options, { asyncResult ->
		if (asyncResult.succeeded()) {
			logger.info("The Socket verticle has been deployed, deployment ID is "
					+ asyncResult.result());
		} else {
			asyncResult.cause().printStackTrace();
		}
	})
}

appCfg.groups.each {
	def vcfg = JsonUril.objectToJson(it)
	def config = vcfg.getMap()
	def options = [
		"config" : config,
		"worker" : false,
		"instances" : vcfg.getInteger(ConfigUtil.INSTANCES)
	]
	
	vertx.deployVerticle(appCfg.groupserver, options, { asyncResult ->
		if (asyncResult.succeeded()) {
			logger.info("The "+appCfg.groupserver+":"+vcfg.getString(ConfigUtil.NAME)+" Group verticle has been deployed, deployment ID is "
					+ asyncResult.result());
		} else {
			asyncResult.cause().printStackTrace();
		}
	})
}

final LocalMap<String, String> commands = vertx.sharedData().getLocalMap(ConfigUtil.COMMANDS)

appCfg.commands.each {
	def vcfg = JsonUril.objectToJson(it)
	def config = vcfg.getMap()
	def options = [
		"config" : config,
		"worker" : false,
		"instances" : vcfg.getInteger(ConfigUtil.INSTANCES)
	]
	
	vertx.deployVerticle(vcfg.getString(ConfigUtil.VERTILEFILE), options, { asyncResult ->
		if (asyncResult.succeeded()) {
			logger.info("The "+vcfg.getString(ConfigUtil.VERTILEFILE)+" Command verticle has been deployed, deployment ID is "
					+ asyncResult.result());
		} else {
			asyncResult.cause().printStackTrace();
		}
	})
	commands.put(vcfg.getString(ConfigUtil.NAME), vcfg.getString(ConfigUtil.ADDRESS))
}

def vcfg = JsonUril.objectToJson(appCfg.admin)
def config = vcfg.getMap()
def options = [
	"config" : config,
	"worker" : false,
	"instances" : 1
]

vertx.deployVerticle(vcfg.getString(ConfigUtil.VERTILEFILE), options, { asyncResult ->
	if (asyncResult.succeeded()) {
		logger.info("The "+vcfg.getString(ConfigUtil.VERTILEFILE)+" verticle has been deployed, deployment ID is "
				+ asyncResult.result());
	} else {
		asyncResult.cause().printStackTrace();
	}
})

def eb = vertx.eventBus()
eb.consumer("server.deploy", { message -> 
	logger.info("deploy new service:"+message.body().name)
	config = message.body()
	def opt = [
		"config" : config,
		"worker" : false,
		"instances" : Integer.parseInt(message.body().instances)
	]
	
	vertx.deployVerticle(message.body().verticlefile, opt, { asyncResult ->
		if (asyncResult.succeeded()) {
			logger.info("The "+message.body().verticlefile+" verticle has been deployed, deployment ID is "
					+ asyncResult.result());
		} else {
			asyncResult.cause().printStackTrace();
		}
	})
	commands.put(message.body().name, message.body().address)
})



