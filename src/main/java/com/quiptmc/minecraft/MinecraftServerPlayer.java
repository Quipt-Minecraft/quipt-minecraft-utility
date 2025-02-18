package com.quiptmc.minecraft;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public class MinecraftServerPlayer {

    private JSONObject data;
    public final MinecraftServer server;

    public MinecraftServerPlayer(MinecraftServer server, UUID uuid) {
        this.server = server;
        if(server.apiManager().isEmpty()) throw new IllegalArgumentException("No api manager found. Api manager must be set up to get player data");

        MinecraftServer.ApiManager.ApiResponse response = server.data();
        JSONObject raw = response.raw();
        if(!raw.has("player_stats")) throw new IllegalArgumentException("No player stats found");
        data = new JSONObject();
        JSONArray array = raw.getJSONArray("player_stats");
        for(int i = 0; i < array.length(); i++){
            JSONObject player = array.getJSONObject(i);
            if(player.getString("uuid").equals(uuid.toString())){
                data = player;
                break;
            }
        }
    }

    public String name() {
        return data.getString("name");
    }

    public UUID uuid() {
        return UUID.fromString(data.getString("uuid"));
    }

    public JSONObject data() {
        return data;
    }

    public void sendMessage(String message) {
        if(server.apiManager().isEmpty()) throw new IllegalArgumentException("No api manager found. Api manager must be set up to send messages");
        server.apiManager().get().sendMessage(uuid(), message);
    }
}
