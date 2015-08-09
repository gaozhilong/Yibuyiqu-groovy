package org.jianyi.yibuyiqu.cache

import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.instance.HazelcastInstanceFactory

class Cache {
	
	private static HazelcastInstance hazelcastInstance = null
	
	static final String SESSIONS = "sessions"
	
	static final String CONFIG = "config"
	
	static final String GROUPS = "groups"
	
	private Cache() {
		Config config = new Config()
		config.setInstanceName("YibuyiquServer")
		config.getNetworkConfig().setPort(5701)
		config.getNetworkConfig().setPortAutoIncrement(true)
		
		MapConfig groupConfig = new MapConfig()
		groupConfig.setBackupCount(1)
		config.getMapConfigs().put(GROUPS, groupConfig)
		
		MapConfig sessionConfig = new MapConfig()
		sessionConfig.setBackupCount(1)
		sessionConfig.setTimeToLiveSeconds(3600)
		config.getMapConfigs().put(SESSIONS, sessionConfig)
		
		hazelcastInstance = HazelcastInstanceFactory.newHazelcastInstance(config)
	}
	
	void close() {
		Hazelcast.shutdownAll()
	}
	
	static HazelcastInstance getHazelcastInstance() {
		if (hazelcastInstance == null) {
			new Cache();
		}
		return hazelcastInstance;
	}

}
