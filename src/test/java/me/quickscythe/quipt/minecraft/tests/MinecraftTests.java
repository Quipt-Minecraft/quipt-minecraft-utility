package me.quickscythe.quipt.minecraft.tests;

import me.quickscythe.quipt.minecraft.MinecraftServer;
import me.quickscythe.quipt.minecraft.MinecraftServerPlayer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class MinecraftTests {

    @Test
    void testMinecraftServerPlayer() {
        MinecraftServer server = new MinecraftServer("127.0.0.1", "secret", "http://localhost:8080/api/server_status/");
        MinecraftServerPlayer player = server.getPlayer(UUID.fromString("60191757-427b-421e-bee0-399465d7e852"));
        System.out.println(player.name());
    }
}
