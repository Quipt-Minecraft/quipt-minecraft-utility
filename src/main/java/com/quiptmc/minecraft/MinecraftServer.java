package com.quiptmc.minecraft;

import com.quiptmc.core.annotations.Nullable;
import com.quiptmc.core.utils.NetworkUtils;
import com.quiptmc.core.utils.TaskScheduler;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MinecraftServer {

//    public final JSONObject data;
//    private final String secret;
//    public final String apiEndpoint;
//    public final String ip;

    @Nullable
    private ApiManager apiManager = null;

    private final List<String> aliases = new ArrayList<>();
    private final String ip;
    private final JSONObject secrets;

    private final Cache cache = new Cache();

    public MinecraftServer(String ip, JSONObject secrets, String... aliases) {
        this.ip = ip;
        this.secrets = secrets;
        this.aliases.addAll(Arrays.asList(aliases));
    }

    public void setupApi(String url) throws MalformedURLException {
        this.apiManager = new ApiManager(url);
    }


    public Optional<ApiManager> apiManager() {
        return Optional.ofNullable(apiManager);
    }

    public String ip() {
        return ip;
    }

    public List<String> aliases() {
        return aliases;
    }

    public JSONObject secrets() {
        return secrets;
    }

    public ApiManager.ApiResponse data(){
        if(apiManager().isEmpty()) throw new IllegalArgumentException("No api manager found. Api manager must be set up to get player data");
        return apiManager().get().api("/server_status/" + ip, secrets());
    }

    public class Builder {

        private final String ip;
        private final JSONObject secrets;
        private final List<String> aliases = new ArrayList<>();

        public Builder(String ip, JSONObject secrets) {
            this.ip = ip;
            this.secrets = secrets;
        }

        public Builder addAlias(String alias) {
            aliases.add(alias);
            return this;
        }

        public Builder addAliases(String... aliases) {
            this.aliases.addAll(Arrays.asList(aliases));
            return this;
        }

        public MinecraftServer build() {
            return new MinecraftServer(ip, secrets, aliases.toArray(new String[0]));
        }
    }


    public static class Cache {

        Map<String, Long> lastUpdated = new HashMap<>();
        Map<String, Object> cache = new HashMap<>();
        Map<String, Object> add = new HashMap<>();
        List<String> remove = new ArrayList<>();

        public Cache() {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<String, Long> entry : lastUpdated.entrySet()) {
                        if (now - entry.getValue() > TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS)) {
                            //TODO change delay and remove logs
                            System.getLogger("Cache").log(System.Logger.Level.ALL, "Removing " + entry.getKey() + " from cache");
                            remove.add(entry.getKey());
                        }
                    }
                    for (String key : remove) {
                        cache.remove(key);
                        lastUpdated.remove(key);
                    }
                    remove.clear();
                    for (Map.Entry<String, Object> entry : add.entrySet()) {
                        cache.put(entry.getKey(), entry.getValue());
                        lastUpdated.put(entry.getKey(), now);
                    }
                    TaskScheduler.scheduleAsyncTask(this, 5, TimeUnit.SECONDS);
                }
            };
            TaskScheduler.scheduleAsyncTask(task, 5, TimeUnit.SECONDS);
        }

        public void add(String key, Object value) {
            add.put(key, value);
        }

        public void remove(String key) {
            remove.add(key);
        }

        public Object get(String key) {
            lastUpdated.put(key, System.currentTimeMillis());
            return cache.getOrDefault(key, null);
        }

        public <T> T get(String key, Class<T> type) {
            return type.cast(get(key));
        }

        public boolean contains(String key) {
            return cache.containsKey(key);
        }

        public void clear() {
            remove.addAll(cache.keySet());
        }


    }

    public class ApiManager {

        private final URL endpoint;

        public ApiManager(String apiEndpoint) throws MalformedURLException {
            endpoint = URI.create(apiEndpoint).toURL();
        }

        public MinecraftServerPlayer getPlayer(UUID uuid) {
            if (MinecraftServer.this.cache.contains("players." + uuid.toString())) {
                return MinecraftServer.this.cache.get("players." + uuid, MinecraftServerPlayer.class);
            }
            MinecraftServerPlayer player = new MinecraftServerPlayer(MinecraftServer.this, uuid);
            MinecraftServer.this.cache.add("players." + uuid, player);
            return player;
        }

        public ApiResponse sendMessage(UUID uuid, String message) {
            JSONObject postRequest = new JSONObject();
            postRequest.put("secret", secrets.getString("secret"));
            postRequest.put("action", "send_message");
            JSONObject action = new JSONObject();
            action.put("message", message);
            action.put("to", uuid.toString());
            postRequest.put("action", action);

            return api("/action/" + ip, postRequest);
        }

        public ApiResponse api(String path, JSONObject data) throws JSONException {
            JSONObject raw = new JSONObject(NetworkUtils.post(endpoint + path, data));
            return new ApiResponse(raw.optEnum(ApiResponse.RequestResult.class, "result", ApiResponse.RequestResult.NO_ACTION), raw);
        }

        public record ApiResponse(RequestResult result, JSONObject raw) {

            public enum RequestResult {
                SUCCESS, FAILURE, NO_ACTION
            }

        }


    }
}
