package com.example.axorders.manager;

import com.example.axorders.AxOrdersAddon;
import com.example.axorders.model.BuyOrder;
import com.example.axorders.util.ItemMatcher;
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
import java.util.*;
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

                    ItemStack template = config.getItemStack(path + "item-template");
                    if (template == null) {
                        Material mat = Material.valueOf(config.getString(path + "material"));
                        template = new ItemStack(mat);
                    }

                    restoreItem(config, path + "item-data.", template);
                    template.setAmount(1);

                    int wanted = config.getInt(path + "quantity-wanted");
                    int filled = config.getInt(path + "quantity-filled");
                    double price = config.getDouble(path + "price-each");
                    long created = config.getLong(path + "created-at");

                    orders.put(id, new BuyOrder(id, buyerUuid, buyerName, template, wanted, filled, price, created));
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load order " + key, ex);
                }
            }
        }

        if (config.isConfigurationSection("pending")) {
            for (String key : config.getConfigurationSection("pending").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<?> raw = config.getList("pending." + key, Collections.emptyList());

                    List<ItemStack> items = new ArrayList<>();
                    for (Object o : raw) {
                        if (o instanceof ItemStack item) items.add(item);
                    }

                    if (!items.isEmpty()) {
                        pendingDeliveries.put(uuid, items);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load pending " + key, ex);
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
            saveItem(config, path + "item-data.", order.getItemTemplate());
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
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed saving orders.yml", ex);
        }
    }

    public synchronized void addOrder(BuyOrder order) {
        orders.put(order.getId(), order);
        save();
    }

    public synchronized List<BuyOrder> getOrders() {
        return orders.values().stream()
                .filter(o -> !o.isFulfilled())
                .sorted(Comparator.comparingDouble(BuyOrder::getPriceEach).reversed())
                .collect(Collectors.toList());
    }

    public synchronized BuyOrder getOrder(UUID id) {
        return orders.get(id);
    }

    public String cancelOrder(Player player, UUID orderId) {
        BuyOrder order;

        synchronized (this) {
            order = orders.get(orderId);

            if (order == null) return plugin.msg("order-not-found");

            if (!order.getBuyerUuid().equals(player.getUniqueId())
                    && !player.hasPermission("axorders.admin")) {
                return plugin.msg("no-permission");
            }

            orders.remove(orderId);
            save();
        }

        plugin.getCurrencyManager()
                .give(order.getBuyerUuid(), order.getRemaining() * order.getPriceEach());

        return null;
    }

    public String fillOrder(Player seller, UUID orderId) {

        BuyOrder order;

        synchronized (this) {
            order = orders.get(orderId);

            if (order == null) return plugin.msg("order-not-found");

            if (order.getBuyerUuid().equals(seller.getUniqueId()))
                return plugin.msg("own-order");

            if (order.isFulfilled())
                return plugin.msg("order-not-found");
        }

        int available = countItems(seller, order.getItemTemplate());
        if (available <= 0)
            return plugin.msg("no-items", "{material}", itemName(order.getItemTemplate()));

        int amount = Math.min(available, order.getRemaining());
        if (amount <= 0)
            return plugin.msg("no-items");

        double payment = amount * order.getPriceEach();

        List<ItemStack> toRemove = new ArrayList<>();
        int left = amount;

        for (ItemStack item : seller.getInventory().getContents()) {
            if (item == null || left <= 0) continue;
            if (!ItemMatcher.matchesCustomItem(item, order.getItemTemplate())) continue;

            int take = Math.min(item.getAmount(), left);

            ItemStack copy = item.clone();
            copy.setAmount(take);
            toRemove.add(copy);

            left -= take;
        }

        boolean paid = plugin.getCurrencyManager()
                .give(seller.getUniqueId(), payment);

        if (!paid) return plugin.msg("no-currency-hook");

        int remaining = amount;

        for (int i = 0; i < seller.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = seller.getInventory().getItem(i);
            if (item == null) continue;
            if (!ItemMatcher.matchesCustomItem(item, order.getItemTemplate())) continue;

            int take = Math.min(item.getAmount(), remaining);

            if (item.getAmount() <= take) {
                seller.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }

            remaining -= take;
        }

        synchronized (this) {
            BuyOrder current = orders.get(orderId);

            if (current == null) {
                seller.getInventory().addItem(toRemove.toArray(ItemStack[]::new));
                plugin.getCurrencyManager().give(seller.getUniqueId(), -payment);
                return plugin.msg("order-not-found");
            }

            current.setQuantityFilled(current.getQuantityFilled() + amount);

            if (current.isFulfilled()) {
                orders.remove(orderId);
            }

            save();
        }

        seller.sendMessage(plugin.msg("order-filled",
                "{amount}", String.valueOf(amount),
                "{material}", itemName(order.getItemTemplate()),
                "{payment}", plugin.getCurrencyManager().format(payment)));

        return null;
    }

    private int countItems(Player player, ItemStack template) {
        int count = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ItemMatcher.matchesCustomItem(item, template)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    public synchronized void addPending(UUID uuid, List<ItemStack> items) {
        pendingDeliveries.computeIfAbsent(uuid, k -> new ArrayList<>()).addAll(items);
        save();
    }

    public synchronized List<ItemStack> getPending(UUID uuid) {
        return new ArrayList<>(pendingDeliveries.getOrDefault(uuid, Collections.emptyList()));
    }

    public synchronized void clearPending(UUID uuid) {
        pendingDeliveries.remove(uuid);
        save();
    }

    private void saveItem(YamlConfiguration config, String path, ItemStack item) {
        config.set(path + "material", item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        config.set(path + "name", meta.hasDisplayName() ? meta.getDisplayName() : null);
        config.set(path + "lore", meta.hasLore() ? meta.getLore() : null);

        if (meta.hasCustomModelData()) {
            config.set(path + "custom-model-data", meta.getCustomModelData());
        }

        Map<String, Integer> ench = new HashMap<>();
        meta.getEnchants().forEach((e, lvl) -> ench.put(e.getKey().toString(), lvl));

        if (!ench.isEmpty()) {
            config.set(path + "enchantments", ench);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Map<String, Object> data = new HashMap<>();

        for (NamespacedKey key : pdc.getKeys()) {
            Object val = readPdc(pdc, key);
            if (val != null) data.put(key.toString(), val);
        }

        if (!data.isEmpty()) {
            config.set(path + "pdc", data);
        }
    }

    private void restoreItem(YamlConfiguration config, String path, ItemStack item) {
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
            for (String k : config.getConfigurationSection(path + "enchantments").getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(k);
                Enchantment ench = key != null ? Enchantment.getByKey(key) : null;
                if (ench != null) {
                    meta.addEnchant(ench, config.getInt(path + "enchantments." + k), true);
                }
            }
        }

        if (config.isConfigurationSection(path + "pdc")) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            for (String k : config.getConfigurationSection(path + "pdc").getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(k);
                if (key == null) continue;

                Object val = config.get(path + "pdc." + k);
                writePdc(pdc, key, val);
            }
        }

        item.setItemMeta(meta);
    }

    private Object readPdc(PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc.has(key, PersistentDataType.STRING)) return pdc.get(key, PersistentDataType.STRING);
        if (pdc.has(key, PersistentDataType.INTEGER)) return pdc.get(key, PersistentDataType.INTEGER);
        if (pdc.has(key, PersistentDataType.LONG)) return pdc.get(key, PersistentDataType.LONG);
        if (pdc.has(key, PersistentDataType.DOUBLE)) return pdc.get(key, PersistentDataType.DOUBLE);
        return null;
    }

    private void writePdc(PersistentDataContainer pdc, NamespacedKey key, Object val) {
        if (val instanceof String s) pdc.set(key, PersistentDataType.STRING, s);
        else if (val instanceof Integer i) pdc.set(key, PersistentDataType.INTEGER, i);
        else if (val instanceof Long l) pdc.set(key, PersistentDataType.LONG, l);
        else if (val instanceof Double d) pdc.set(key, PersistentDataType.DOUBLE, d);
    }

    public static String itemName(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        return item.getType().name().toLowerCase().replace("_", " ");
    }
}
