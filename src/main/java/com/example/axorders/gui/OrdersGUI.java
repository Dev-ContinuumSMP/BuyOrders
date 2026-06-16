package com.example.axorders.gui;

import com.example.axorders.AxOrdersAddon;
import com.example.axorders.manager.OrderManager;
import com.example.axorders.model.BuyOrder;
import com.example.axorders.util.ItemMatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrdersGUI implements InventoryHolder {

    public enum SortMode {
        PRICE_DESC, PRICE_ASC, NEWEST, OLDEST
    }

    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_SORT = 46;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_REFRESH = 50;
    private static final int SLOT_NEXT = 53;

    private final AxOrdersAddon plugin;
    private final Player player;
    private final Material filter;
    private int page;
    private SortMode sortMode = SortMode.PRICE_DESC;
    private Inventory inventory;

    public OrdersGUI(AxOrdersAddon plugin, Player player, Material filter) {
        this.plugin = plugin;
        this.player = player;
        this.filter = filter;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        String baseTitle = plugin.getConfig().getString("gui-title", "&6&lBuy Orders");
        String title = filter == null ? baseTitle : baseTitle + " &8- &b" + OrderManager.materialName(filter);
        Inventory inv = Bukkit.createInventory(this, 54, AxOrdersAddon.color(title));
        this.inventory = inv;

        List<BuyOrder> orders = getOrders();
        int totalPages = Math.max(1, (int) Math.ceil(orders.size() / (double) ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, orders.size());
        for (int index = start; index < end; index++) {
            inv.setItem(index - start, buildOrderItem(orders.get(index)));
        }

        ItemStack filler = makeFiller();
        for (int slot = 45; slot < 54; slot++) {
            if (slot == SLOT_PREVIOUS ||
                slot == SLOT_SORT ||
                slot == SLOT_INFO ||
                slot == SLOT_REFRESH ||
                slot == SLOT_NEXT) continue;
        
            inv.setItem(slot, filler);
        }

        if (page > 0) inv.setItem(SLOT_PREVIOUS, makeControl(Material.ARROW, "&ePrevious Page"));
        inv.setItem(SLOT_SORT, makeControl(Material.HOPPER, "&eSort: &b" + sortLabel(), "&7Click to cycle"));
        inv.setItem(SLOT_INFO, makeControl(Material.PAPER,
                "&fPage &e" + (page + 1) + "&f/&e" + totalPages,
                "&7" + orders.size() + " open order(s)"));
        inv.setItem(SLOT_REFRESH, makeControl(Material.SUNFLOWER, "&aRefresh"));
        if (page < totalPages - 1) inv.setItem(SLOT_NEXT, makeControl(Material.ARROW, "&eNext Page"));

        return inv;
    }

    private ItemStack buildOrderItem(BuyOrder order) {
        int amount = Math.min(order.getRemaining(), 64);
        ItemStack item = order.getItemTemplate().clone();
        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        if (!meta.hasDisplayName()) {
            meta.setDisplayName(AxOrdersAddon.color(
                    "&b" + OrderManager.materialName(order.getMaterial()) +
                    " &8#" + order.getShortId()
            ));
        }

        item.setItemMeta(meta);

        int available = countMatchingItems(order.getItemTemplate());
        int fillAmount = Math.min(available, order.getRemaining());
        double payout = fillAmount * order.getPriceEach();

        List<String> lore = new ArrayList<>();
        lore.add(AxOrdersAddon.color("&eBuyer: &f" + order.getBuyerName()));
        lore.add(AxOrdersAddon.color("&eRemaining: &f" + order.getRemaining() + "&7/&f" + order.getQuantityWanted()));
        lore.add(AxOrdersAddon.color("&ePrice Each: &a" + plugin.getCurrencyManager().format(order.getPriceEach())));
        lore.add(AxOrdersAddon.color("&eYour Items: &f" + available));
        lore.add(AxOrdersAddon.color("&ePayout Now: &a" + plugin.getCurrencyManager().format(payout)));
        lore.add("");
        if (order.getBuyerUuid().equals(player.getUniqueId())) {
            lore.add(AxOrdersAddon.color("&7Right-click to &ccancel"));
        } else if (fillAmount > 0) {
            lore.add(AxOrdersAddon.color("&7Left-click to &afill " + fillAmount));
        } else {
            lore.add(AxOrdersAddon.color("&cYou do not have this item"));
        }
        if (player.hasPermission("axorders.admin") && !order.getBuyerUuid().equals(player.getUniqueId())) {
            lore.add(AxOrdersAddon.color("&7Right-click to &ccancel as admin"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<BuyOrder> getOrders() {
        List<BuyOrder> orders = filter == null
                ? plugin.getOrderManager().getOrders()
                : plugin.getOrderManager().getOrdersByMaterial(filter);

        switch (sortMode) {
            case PRICE_DESC -> orders.sort(Comparator.comparingDouble(BuyOrder::getPriceEach).reversed());
            case PRICE_ASC -> orders.sort(Comparator.comparingDouble(BuyOrder::getPriceEach));
            case NEWEST -> orders.sort(Comparator.comparingLong(BuyOrder::getCreatedAt).reversed());
            case OLDEST -> orders.sort(Comparator.comparingLong(BuyOrder::getCreatedAt));
        }
        return orders;
    }

    private int countMatchingItems(ItemStack template) {
        int count = 0;
    
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ItemMatcher.matchesCustomItem(item, template)) {
                count += item.getAmount();
            }
        }
    
        return count;
    }

    private ItemStack makeControl(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
    
        meta.setDisplayName(AxOrdersAddon.color(name));
    
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(AxOrdersAddon.color(line));
    
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private String sortLabel() {
        return switch (sortMode) {
            case PRICE_DESC -> "Highest Price";
            case PRICE_ASC -> "Lowest Price";
            case NEWEST -> "Newest";
            case OLDEST -> "Oldest";
        };
    }

    public BuyOrder getOrderAtSlot(int slot) {
        if (slot < 0 || slot >= ITEMS_PER_PAGE) return null;
        int index = page * ITEMS_PER_PAGE + slot;
        List<BuyOrder> orders = getOrders();
        return index >= orders.size() ? null : orders.get(index);
    }

    public void previousPage() {
        if (page > 0) page--;
    }

    public void nextPage() {
        int totalPages = Math.max(1, (int) Math.ceil(getOrders().size() / (double) ITEMS_PER_PAGE));
        if (page < totalPages - 1) page++;
    }

    public void cycleSortMode() {
        SortMode[] modes = SortMode.values();
        sortMode = modes[(sortMode.ordinal() + 1) % modes.length];
        page = 0;
    }

    public int getSlotPrevious() { return SLOT_PREVIOUS; }
    public int getSlotSort() { return SLOT_SORT; }
    public int getSlotRefresh() { return SLOT_REFRESH; }
    public int getSlotNext() { return SLOT_NEXT; }
}
