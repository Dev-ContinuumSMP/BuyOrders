package com.example.axorders.listener;

import com.example.axorders.AxOrdersAddon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerJoinListener implements Listener {

    private final AxOrdersAddon plugin;

    public PlayerJoinListener(AxOrdersAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> pending = plugin.getOrderManager().getPendingDeliveries(player.getUniqueId());
        if (pending.isEmpty()) return;

        plugin.getOrderManager().clearPendingDeliveries(player.getUniqueId());
        List<ItemStack> overflowItems = new ArrayList<>();
        for (ItemStack item : pending) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            overflowItems.addAll(overflow.values());
        }
        if (!overflowItems.isEmpty()) {
            for (ItemStack item : overflowItems) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(AxOrdersAddon.color("&eSome buy-order items were dropped because your inventory was full."));
        } else {
            player.sendMessage(AxOrdersAddon.color("&aYour pending buy-order items were delivered."));
        }
    }
}
