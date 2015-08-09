package org.jianyi.yibuyiqu.db

import io.vertx.core.json.JsonObject

import java.sql.Connection

import org.jianyi.yibuyiqu.cache.Cache
import org.jianyi.yibuyiqu.utils.ConfigUtil
import org.postgresql.ds.PGPoolingDataSource

import com.hazelcast.core.IMap

class PostgresDataSource {
	
	private static PostgresDataSource datasource
	
	private PGPoolingDataSource source
	
	private PostgresDataSource() {
		def hazelcastInstance = Cache.getHazelcastInstance()
		IMap<String,Map> map = hazelcastInstance.getMap(Cache.CONFIG)
		JsonObject dbcfg = new JsonObject(map.get(ConfigUtil.POSTGRES))
		source = new PGPoolingDataSource()
		//source.setDataSourceName("PGDataSource")
		source.setServerName(dbcfg.getString(ConfigUtil.HOST))
		source.setDatabaseName(dbcfg.getString(ConfigUtil.DB))
		source.setUser(dbcfg.getString(ConfigUtil.USERNAME))
		source.setPassword(dbcfg.getString(ConfigUtil.PASSWORD))
		source.setMaxConnections(Integer.parseInt(dbcfg.getString(ConfigUtil.MAX)))
	}
	
	def getConnection(){
		Connection conn = source.getConnection()
		return conn
	}
	
	public void close() {
		source.close()
	}
	
	def static PostgresDataSource getInstance()  {
		if (datasource == null) {
			datasource = new PostgresDataSource()
		}
		return datasource;
	}

}
