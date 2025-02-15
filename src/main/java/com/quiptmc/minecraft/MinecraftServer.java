package com.quiptmc.minecraft;

import com.quiptmc.core.utils.NetworkUtils;
import com.quiptmc.core.utils.TaskScheduler;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MinecraftServer {

    public final JSONObject data;

    Cache cache = new Cache();

    public MinecraftServer(String ip, String secret, String apiEndpoint) {
        JSONObject post = new JSONObject();
        post.put("secret", secret);
        this.data = new JSONObject(NetworkUtils.post(apiEndpoint +ip, post)).getJSONObject("quipt_data");
    }

    public MinecraftServerPlayer getPlayer(UUID uuid){
        if(cache.contains("players." + uuid.toString())){
            return cache.get("players." + uuid, MinecraftServerPlayer.class);
        }
        MinecraftServerPlayer player = new MinecraftServerPlayer(this, uuid);
        cache.add("players." + uuid, player);
        return player;
    }


    public static class Cache {

        Map<String, Long> lastUpdated = new HashMap<>();
        Map<String, Object> cache = new HashMap<>();
        Map<String, Object> add = new HashMap<>();
        List<String> remove = new ArrayList<>();

        public Cache(){
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    for(Map.Entry<String, Long> entry : lastUpdated.entrySet()){
                        if(now - entry.getValue() > TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS)){
                            //TODO change delay and remove logs
                            System.getLogger("Cache").log(System.Logger.Level.ALL, "Removing " + entry.getKey() + " from cache");
                            remove.add(entry.getKey());
                        }
                    }
                    for(String key : remove){
                        cache.remove(key);
                        lastUpdated.remove(key);
                    }
                    remove.clear();
                    for(Map.Entry<String, Object> entry : add.entrySet()){
                        cache.put(entry.getKey(), entry.getValue());
                        lastUpdated.put(entry.getKey(), now);
                    }
                    TaskScheduler.scheduleAsyncTask(this, 5, TimeUnit.SECONDS);
                }
            };
            TaskScheduler.scheduleAsyncTask(task, 5, TimeUnit.SECONDS);
        }

        public void add(String key, Object value){
            add.put(key, value);
        }

        public void remove(String key){
            remove.add(key);
        }

        public Object get(String key){
            lastUpdated.put(key, System.currentTimeMillis());
            return cache.getOrDefault(key, null);
        }

        public <T> T get(String key, Class<T> type){
            return type.cast(get(key));
        }

        public boolean contains(String key){
            return cache.containsKey(key);
        }

        public void clear(){
            remove.addAll(cache.keySet());
        }



    }
}
