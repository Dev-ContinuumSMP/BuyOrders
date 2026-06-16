package com.example.axorders.listener;

import com.example.axorders.AxOrdersAddon;
import com.example.axorders.gui.OrdersGUI;
import com.example.axorders.model.BuyOrder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public class OrdersGuiListener implements Listener {

    private final AxOrdersAddon plugin;

    public OrdersGuiListener(AxOrdersAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof OrdersGUI gui)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        if (slot == gui.getSlotPrevious()) {
            gui.previousPage();
            player.openInventory(gui.buildInventory());
            return;
        }
        if (slot == gui.getSlotSort()) {
            gui.cycleSortMode();
            player.openInventory(gui.buildInventory());
            return;
        }
        if (slot == gui.getSlotRefresh()) {
            player.openInventory(gui.buildInventory());
            return;
        }
        if (slot == gui.getSlotNext()) {
            gui.nextPage();
            player.openInventory(gui.buildInventory());
            return;
        }

        BuyOrder order = gui.getOrderAtSlot(slot);
        if (order == null) return;

        String error = event.isRightClick()
                ? plugin.getOrderManager().cancelOrder(player, order.getId())
                : plugin.getOrderManager().fillOrder(player, order.getId());

        if (error != null) player.sendMessage(error);
        player.openInventory(gui.buildInventory());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof OrdersGUI)) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
