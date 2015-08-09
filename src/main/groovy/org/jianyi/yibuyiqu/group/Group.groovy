package org.jianyi.yibuyiqu.group

import io.vertx.core.json.JsonObject

import org.jianyi.yibuyiqu.utils.ConfigUtil

import com.google.common.collect.Maps
import com.google.common.collect.Sets

class Group implements Serializable {
	
	static final String SESSIONIDS = "sessions"
	
	static final String USERS = "users"
	
	String name,type
	Map<String, Set<String>> objects
	
	def Group(JsonObject groupCfg) {
		name = groupCfg.getString(ConfigUtil.NAME)
		type = groupCfg.getString(ConfigUtil.TYPE)
		objects = Maps.newConcurrentMap()
		objects.put(SESSIONIDS, Sets.newConcurrentHashSet())
		objects.put(USERS, Sets.newConcurrentHashSet())
	}

}
