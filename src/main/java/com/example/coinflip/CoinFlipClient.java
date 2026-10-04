package com.example.coinflip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.regex.*;

public class CoinFlipClient implements ClientModInitializer {

    // ===== CONFIG =====
    static final String ADVERT =
        "Coinflip! Pay me any amount with /pay and you have a 45% chance to get double back. Odds are 45%, play at your own risk.";
    static final int MSG_DELAY_TICKS = 20 * 8;
    static final double WIN_CHANCE = 0.45;
    static final double MAX_BET = 1_000_000;
    static final Pattern PAID_PATTERN =
        Pattern.compile("^(\\w{3,16}) paid you \\$([\\d.,]+)\\s*([KkMmBb]?)");
    // ==================

    private static boolean enabled = false;
    private static KeyBinding toggleKey;
    private final Set<String> messaged = new HashSet<>();
    private final Random random = new Random();
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.coinflipbot.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.coinflipbot"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!enabled || overlay) return;
            handlePayment(MinecraftClient.getInstance(), message.getString());
        });
    }

    private void onTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (client.player != null)
                client.player.sendMessage(Text.literal("CoinFlip bot: " + (enabled ? "ON" : "OFF")), true);
        }
        if (!enabled || client.player == null || client.getNetworkHandler() == null) return;

        if (++tickCounter < MSG_DELAY_TICKS) return;
        tickCounter = 0;

        String me = client.getSession().getUsername();
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (name.equals(me) || messaged.contains(name)) continue;
            messaged.add(name);
            client.getNetworkHandler().sendChatCommand("msg " + name + " " + ADVERT);
            break;
        }
    }

    private void handlePayment(MinecraftClient client, String chat) {
        Matcher m = PAID_PATTERN.matcher(chat);
        if (!m.find() || client.getNetworkHandler() == null) return;

        String payer = m.group(1);
        double amount = parseAmount(m.group(2), m.group(3));
        if (amount <= 0) return;

        if (amount > MAX_BET) {
            client.getNetworkHandler().sendChatCommand("pay " + payer + " " + (long) amount);
            client.getNetworkHandler().sendChatCommand("msg " + payer + " Bet too high, refunded. Max is " + (long) MAX_BET);
            return;
        }

        if (random.nextDouble() < WIN_CHANCE) {
            client.getNetworkHandler().sendChatCommand("pay " + payer + " " + (long) (amount * 2));
            client.getNetworkHandler().sendChatCommand("msg " + payer + " You won! Paid x2. GG");
        } else {
            client.getNetworkHandler().sendChatCommand("msg " + payer + " You lost this one, better luck next time!");
        }
    }

    private double parseAmount(String num, String suffix) {
        try {
            double v = Double.parseDouble(num.replace(",", ""));
            return switch (suffix.toUpperCase()) {
                case "K" -> v * 1_000;
                case "M" -> v * 1_000_000;
                case "B" -> v * 1_000_000_000;
                default -> v;
            };
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
