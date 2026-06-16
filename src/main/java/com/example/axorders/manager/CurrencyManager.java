package com.example.axorders.manager;

import com.artillexstudios.axauctions.api.AxAuctionsAPI;
import com.artillexstudios.axauctions.hooks.currency.CurrencyHook;
import com.example.axorders.AxOrdersAddon;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;

public class CurrencyManager {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##");

    private final AxOrdersAddon plugin;

    public CurrencyManager(AxOrdersAddon plugin) {
        this.plugin = plugin;
    }

    public CurrencyHook getHook() {
        Map<String, CurrencyHook> registry = AxAuctionsAPI.getRegistry();
        String configured = plugin.getConfig().getString("currency", "vault");

        CurrencyHook hook = registry.get(configured);
        if (hook != null) return hook;

        for (Map.Entry<String, CurrencyHook> entry : registry.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(configured)) return entry.getValue();
        }
        return registry.values().stream().findFirst().orElse(null);
    }

    public boolean has(UUID player, double amount) {
        CurrencyHook hook = getHook();
        try {
            return hook != null && hook.getBalance(player) >= amount;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to check AxAuctions currency balance: " + exception.getMessage());
            return false;
        }
    }

    public boolean take(UUID player, double amount) {
        CurrencyHook hook = getHook();
        try {
            return hook != null && hook.takeBalance(player, amount).join();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to take AxAuctions currency balance: " + exception.getMessage());
            return false;
        }
    }

    public boolean give(UUID player, double amount) {
        CurrencyHook hook = getHook();
        try {
            return hook != null && hook.giveBalance(player, amount).join();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to give AxAuctions currency balance: " + exception.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        CurrencyHook hook = getHook();
        String currency = hook == null ? plugin.getConfig().getString("currency", "money") : hook.getName();
        return FORMAT.format(amount) + " " + currency;
    }
}
