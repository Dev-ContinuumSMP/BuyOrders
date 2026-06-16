package com.example.axorders.manager;

import com.example.axorders.AxOrdersAddon;
import com.example.axorders.model.BuyOrder;
import com.example.axorders.util.ItemMatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OrderManager {

    private final AxOrdersAddon plugin;
    private final File dataFile;
    private final Map<UUID, BuyOrder> orders = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> pendingDeliveries = new HashMap<>();

    public OrderManager(AxOrdersAddon plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "orders.yml");
    }

    public synchronized void load() {
        orders.clear();
        pendingDeliveries.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (config.isConfigurationSection("orders")) {
            for (String key : config.getConfigurationSection("orders").getKeys(false)) {
                try {
                    String path = "orders." + key + ".";
                    UUID id = UUID.fromString(config.getString(path + "id"));
                    UUID buyerUuid = UUID.fromString(config.getString(path + "buyer-uuid"));
                    String buyerName = config.getString(path + "buyer-name", "Unknown");
                    ItemStack itemTemplate = config.getItemStack(path + "item-template");
                    if (itemTemplate == null) {
                        Material material = Material.valueOf(config.getString(path + "material"));
                        itemTemplate = new ItemStack(material);
                    }
                    restoreSavedItemData(config, path + "item-data.", itemTemplate);
                    itemTemplate.setAmount(1);
                    int wanted = config.getInt(path + "quantity-wanted");
                    int filled = config.getInt(path + "quantity-filled");
                    double priceEach = config.getDouble(path + "price-each");
                    long createdAt = config.getLong(path + "created-at");
                    orders.put(id, new BuyOrder(id, buyerUuid, buyerName, itemTemplate, wanted, filled, priceEach, createdAt));
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load order " + key, exception);
                }
            }
        }

        if (config.isConfigurationSection("pending")) {
            for (String key : config.getConfigurationSection("pending").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<?> rawItems = config.getList("pending." + key, Collections.emptyList());
                    List<ItemStack> items = new ArrayList<>();
                    for (Object rawItem : rawItems) {
                        if (rawItem instanceof ItemStack itemStack) items.add(itemStack);
                    }
                    if (!items.isEmpty()) pendingDeliveries.put(uuid, items);
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load pending delivery " + key, exception);
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (BuyOrder order : orders.values()) {
            String path = "orders." + order.getId() + ".";
            config.set(path + "id", order.getId().toString());
            config.set(path + "buyer-uuid", order.getBuyerUuid().toString());
            config.set(path + "buyer-name", order.getBuyerName());
            config.set(path + "material", order.getMaterial().name());
            config.set(path + "item-template", order.getItemTemplate());
            saveItemData(config, path + "item-data.", order.getItemTemplate());
            config.set(path + "quantity-wanted", order.getQuantityWanted());
            config.set(path + "quantity-filled", order.getQuantityFilled());
            config.set(path + "price-each", order.getPriceEach());
            config.set(path + "created-at", order.getCreatedAt());
        }
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingDeliveries.entrySet()) {
            config.set("pending." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save orders.yml", exception);
        }
    }

    public synchronized void addOrder(BuyOrder order) {
        orders.put(order.getId(), order);
        save();
    }

    private void saveItemData(YamlConfiguration config, String path, ItemStack item) {
        config.set(path + "material", item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        config.set(path + "name", meta.hasDisplayName() ? meta.getDisplayName() : null);
        config.set(path + "lore", meta.hasLore() ? meta.getLore() : null);
        config.set(path + "custom-model-data", meta.hasCustomModelData() ? meta.getCustomModelData() : null);

        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> enchantment : meta.getEnchants().entrySet()) {
            enchantments.put(enchantment.getKey().getKey().toString(), enchantment.getValue());
        }
        config.set(path + "enchantments", enchantments.isEmpty() ? null : enchantments);

        Map<String, Object> persistentData = new LinkedHashMap<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            Object value = readPersistentDataValue(pdc, key);
            if (value != null) persistentData.put(key.toString(), value);
        }
        config.set(path + "persistent-data-container", persistentData.isEmpty() ? null : persistentData);
    }

    private void restoreSavedItemData(YamlConfiguration config, String path, ItemStack item) {
        if (!config.isConfigurationSection(path.substring(0, path.length() - 1))) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = config.getString(path + "name");
        if (name != null) meta.setDisplayName(name);

        List<String> lore = config.getStringList(path + "lore");
        if (!lore.isEmpty()) meta.setLore(lore);

        if (config.isSet(path + "custom-model-data")) {
            meta.setCustomModelData(config.getInt(path + "custom-model-data"));
        }

        if (config.isConfigurationSection(path + "enchantments")) {
            for (String keyText : config.getConfigurationSection(path + "enchantments").getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(keyText);
                Enchantment enchantment = key == null ? null : Enchantment.getByKey(key);
                if (enchantment != null) {
                    meta.addEnchant(enchantment, config.getInt(path + "enchantments." + keyText), true);
                }
            }
        }

        if (config.isConfigurationSection(path + "persistent-data-container")) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (String keyText : config.getConfigurationSection(path + "persistent-data-container").getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(keyText);
                if (key == null) continue;

                Object value = config.get(path + "persistent-data-container." + keyText);
                writePersistentDataValue(pdc, key, value);
            }
        }

        item.setItemMeta(meta);
    }

    private Object readPersistentDataValue(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.STRING)) return pdc.get(key, PersistentDataType.STRING);

        if (pdc.has(key, PersistentDataType.INTEGER)) return pdc.get(key, PersistentDataType.INTEGER);

        if (pdc.has(key, PersistentDataType.LONG)) return pdc.get(key, PersistentDataType.LONG);

        if (pdc.has(key, PersistentDataType.DOUBLE)) return pdc.get(key, PersistentDataType.DOUBLE);

        return null;
    }

    private void writePersistentDataValue(PersistentDataContainer pdc, NamespacedKey key, Object value) {
        if (value instanceof String stringValue) {
            pdc.set(key, PersistentDataType.STRING, stringValue);
        } else if (value instanceof Integer integerValue) {
            pdc.set(key, PersistentDataType.INTEGER, integerValue);
        } else if (value instanceof Long longValue) {
            pdc.set(key, PersistentDataType.LONG, longValue);
        } else if (value instanceof Double doubleValue) {
            pdc.set(key, PersistentDataType.DOUBLE, doubleValue);
        }
    }

    public synchronized List<BuyOrder> getOrders() {
        return orders.values().stream()
                .filter(order -> !order.isFulfilled())
                .sorted(Comparator.comparingDouble(BuyOrder::getPriceEach).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized List<BuyOrder> getOrdersByMaterial(Material material) {
        return getOrders().stream()
                .filter(order -> order.getMaterial() == material)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized List<BuyOrder> getOrdersByPlayer(UUID uuid) {
        return getOrders().stream()
                .filter(order -> order.getBuyerUuid().equals(uuid))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized BuyOrder getOrderByShortId(String shortId) {
        return orders.values().stream()
                .filter(order -> order.getShortId().equalsIgnoreCase(shortId))
                .findFirst()
                .orElse(null);
    }

    public synchronized BuyOrder getOrder(UUID id) {
        return orders.get(id);
    }

    public String cancelOrder(Player requester, UUID orderId) {
        BuyOrder order;
        synchronized (this) {
            order = orders.get(orderId);
            if (order == null) return plugin.msg("order-not-found");
            if (!order.getBuyerUuid().equals(requester.getUniqueId()) && !requester.hasPermission("axorders.admin")) {
                return plugin.msg("no-permission");
            }
            orders.remove(orderId);
            save();
        }

        plugin.getCurrencyManager().give(order.getBuyerUuid(), order.getRemaining() * order.getPriceEach());
        return null;
    }

    public String fillOrder(Player seller, UUID orderId) {
        BuyOrder order;
        int amount;
        synchronized (this) {
            order = orders.get(orderId);
            if (order == null) return plugin.msg("order-not-found");
            if (order.getBuyerUuid().equals(seller.getUniqueId())) return plugin.msg("own-order");

            int available = countMatchingItems(seller, order.getItemTemplate());
            if (available <= 0) {
                return plugin.msg("no-items", "{material}", orderDisplayName(order));
            }

            amount = Math.min(available, order.getRemaining());
        }

        List<ItemStack> removedItems = removeMatchingItems(seller, order.getItemTemplate(), amount);
        double payment = amount * order.getPriceEach();
        if (!plugin.getCurrencyManager().give(seller.getUniqueId(), payment)) {
            for (ItemStack item : removedItems) seller.getInventory().addItem(item);
            return plugin.msg("no-currency-hook");
        }

        Player buyer = Bukkit.getPlayer(order.getBuyerUuid());
        if (buyer != null && buyer.isOnline()) {
            Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(removedItems.toArray(ItemStack[]::new));
            if (!overflow.isEmpty()) addPendingDelivery(order.getBuyerUuid(), new ArrayList<>(overflow.values()));
            buyer.sendMessage(AxOrdersAddon.color("&aYour buy order received &e" + amount + "x " + orderDisplayName(order) + "&a."));
        } else {
            addPendingDelivery(order.getBuyerUuid(), removedItems);
        }

        synchronized (this) {
            order.setQuantityFilled(order.getQuantityFilled() + amount);
            if (order.isFulfilled()) orders.remove(order.getId());
            save();
        }

        seller.sendMessage(plugin.msg("order-filled",
                "{amount}", String.valueOf(amount),
                "{material}", orderDisplayName(order),
                "{payment}", plugin.getCurrencyManager().format(payment)));
        return null;
    }

    private synchronized void addPendingDelivery(UUID uuid, List<ItemStack> items) {
        pendingDeliveries.computeIfAbsent(uuid, ignored -> new ArrayList<>()).addAll(items);
        save();
    }

    public synchronized List<ItemStack> getPendingDeliveries(UUID uuid) {
        return new ArrayList<>(pendingDeliveries.getOrDefault(uuid, Collections.emptyList()));
    }

    public synchronized void clearPendingDeliveries(UUID uuid) {
        pendingDeliveries.remove(uuid);
        save();
    }

    private int countMatchingItems(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ItemMatcher.matchesCustomItem(item, template)) count += item.getAmount();
        }
        return count;
    }

    private List<ItemStack> removeMatchingItems(Player player, ItemStack template, int amount) {
        int remaining = amount;
        List<ItemStack> removed = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !ItemMatcher.matchesCustomItem(item, template)) continue;

            int take = Math.min(item.getAmount(), remaining);
            ItemStack removedStack = item.clone();
            removedStack.setAmount(take);
            removed.add(removedStack);

            if (item.getAmount() <= take) {
                player.getInventory().setItem(slot, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
            remaining -= take;
        }
        return removed;
    }

    public static Material parseMaterial(String input) {
        if (input == null) return null;
        return Material.matchMaterial(input.replace('-', '_').replace(' ', '_'));
    }

    public static String materialName(Material material) {
        if (material == null) return "Unknown";
        String text = material.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static String itemName(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return materialName(item.getType());
    }

    public static String orderDisplayName(BuyOrder order) {
        return itemName(order.getItemTemplate());
    }
}
