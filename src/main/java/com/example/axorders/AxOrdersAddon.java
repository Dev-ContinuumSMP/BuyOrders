package com.example.axorders;

import com.example.axorders.command.BuyOrderCommand;
import com.example.axorders.command.OrdersCommand;
import com.example.axorders.listener.OrdersGuiListener;
import com.example.axorders.listener.PlayerJoinListener;
import com.example.axorders.manager.CurrencyManager;
import com.example.axorders.manager.OrderManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class AxOrdersAddon extends JavaPlugin {

    private OrderManager orderManager;
    private CurrencyManager currencyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        currencyManager = new CurrencyManager(this);
        orderManager = new OrderManager(this);
        orderManager.load();

        getServer().getPluginManager().registerEvents(new OrdersGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        OrdersCommand ordersCommand = new OrdersCommand(this);
        getCommand("orders").setExecutor(ordersCommand);
        getCommand("orders").setTabCompleter(ordersCommand);

        BuyOrderCommand buyOrderCommand = new BuyOrderCommand(this);
        getCommand("buyorder").setExecutor(buyOrderCommand);
        getCommand("buyorder").setTabCompleter(buyOrderCommand);

        getLogger().info("AxOrdersAddon enabled.");
    }

    @Override
    public void onDisable() {
        if (orderManager != null) orderManager.save();
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public String msg(String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return color(message);
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
