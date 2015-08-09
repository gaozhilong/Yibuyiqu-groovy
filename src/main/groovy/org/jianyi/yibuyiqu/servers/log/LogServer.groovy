package org.jianyi.yibuyiqu.servers.log

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

import org.bson.Document
import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.utils.ConfigUtil

import com.hazelcast.core.IMap
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase

class LogServer extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(LogServer.class)

	MongoClient mongoClient
	MongoCollection<Document> coll,cmdcol

	public void start(Future<Void> startFuture) {
		// TODO Auto-generated method stub
		def hazelcastInstance = Cache.getHazelcastInstance()
		IMap<String,Map> map = hazelcastInstance.getMap(Cache.CONFIG)
		JsonObject dbcfg = new JsonObject(map.get(ConfigUtil.MONGODB))
		mongoClient = new MongoClient(dbcfg.getString(ConfigUtil.HOST), dbcfg.getInteger(ConfigUtil.PORT))
		MongoDatabase db = mongoClient.getDatabase(dbcfg.getString(ConfigUtil.DB))
		coll = db.getCollection(dbcfg.getString(ConfigUtil.LOG))
		cmdcol = db.getCollection(dbcfg.getString(ConfigUtil.RUNTIME_LOG))
		def eb = vertx.eventBus()
		eb.consumer("server.log", { message -> saveLog(message) })
		eb.consumer("server.log.runtime", { message -> commandRunTimeLog(message) })
	}

	def saveLog(message) {
		def log = message.body()
		log.put(ConfigUtil.CREATE_TIME, new Date().format("yyyy-MM-dd'T'HH:mm:ss SSS"))
		Document doc = new Document(log.getMap())
		coll.insertOne(doc)
	}

	def commandRunTimeLog(message) {
		def log = message.body()
		log.put(ConfigUtil.CREATE_TIME, new Date().format("yyyy-MM-dd'T'HH:mm:ss SSS"))
		Document doc = new Document(log.getMap())
		cmdcol.insertOne(doc)
	}

}
