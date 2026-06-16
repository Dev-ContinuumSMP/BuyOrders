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
    private CurrencyHook hook;

    public CurrencyManager(AxOrdersAddon plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        Map<String, CurrencyHook> registry = AxAuctionsAPI.getRegistry();
        String configured = plugin.getConfig().getString("currency", "vault");
    
        CurrencyHook found = registry.get(configured);
    
        if (found == null) {
            for (Map.Entry<String, CurrencyHook> entry : registry.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(configured)) {
                    found = entry.getValue();
                    break;
                }
            }
        }
    
        if (found == null && !registry.isEmpty()) {
            found = registry.values().iterator().next();
        }
    
        this.hook = found;
    }

    public boolean has(UUID player, double amount) {
        try {
            return hook != null && hook.getBalance(player) >= amount;
        } catch (Exception e) {
            plugin.getLogger().warning("Currency check failed: " + e.getMessage());
            return false;
        }
    }
    public boolean take(UUID player, double amount) {
        try {
            return hook != null && hook.takeBalance(player, amount).join();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to take AxAuctions currency balance: " + exception.getMessage());
            return false;
        }
    }

    public boolean give(UUID player, double amount) {
        try {
            return hook != null && hook.giveBalance(player, amount).join();
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to give AxAuctions currency balance: " + exception.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        String currency = hook == null)
            ? plugin.getConfig().getString("currency", "money") : hook.getName();
            ? hook.getName();
        
        return FORMAT.format(amount) + " " + currency;
    }
}
