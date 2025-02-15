package com.quiptmc.minecraft.tests;

import com.quiptmc.minecraft.MinecraftServer;
import com.quiptmc.minecraft.MinecraftServerPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class MinecraftTests {

    @Test
    void testMinecraftServerPlayer() {
        MinecraftServer server = new MinecraftServer("127.0.0.1", "secret", "https://quipt-api.azurewebsites.net/api/server_status/");
        MinecraftServerPlayer player = server.getPlayer(UUID.fromString("60191757-427b-421e-bee0-399465d7e852"));
        System.out.println(player.name());
    }
}
