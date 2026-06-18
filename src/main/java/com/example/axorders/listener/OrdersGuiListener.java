package com.example.axorders.listener;

import com.example.axorders.AxOrdersAddon;
import com.example.axorders.gui.FillOrderGUI;
import com.example.axorders.gui.OrdersGUI;
import com.example.axorders.model.BuyOrder;
import com.example.axorders.manager.OrderManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class OrdersGuiListener implements Listener {

    private final AxOrdersAddon plugin;

    public OrdersGuiListener(AxOrdersAddon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTopInventory().getHolder() instanceof FillOrderGUI fillGui) {
            handleFillGuiClick(event, player, fillGui);
            return;
        }
    
        if (!(event.getView().getTopInventory().getHolder() instanceof OrdersGUI gui)) return;
    
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
    
        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
    
        if (slot < 0 || slot >= topSize) return;
    
        event.setCancelled(true);
    
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
                : null;

        if (error == null && !event.isRightClick()) {
            new FillOrderGUI(plugin, player, order).open();
            return;
        }
    
        if (error != null) player.sendMessage(AxOrdersAddon.color(error));
        player.openInventory(gui.buildInventory());
    }

    private void handleFillGuiClick(InventoryClickEvent event, Player player, FillOrderGUI gui) {
        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();

        if (clicked == null) {
            event.setCancelled(true);
            return;
        }

        if (clicked != top) {
            if (!event.isShiftClick()) return;

            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (!gui.matchesOrder(clickedItem)) {
                player.sendMessage(plugin.msg("fill-invalid-item"));
                return;
            }

            int leftover = gui.addDeposit(clickedItem);
            if (leftover <= 0) {
                event.setCurrentItem(null);
            } else {
                clickedItem.setAmount(leftover);
                event.setCurrentItem(clickedItem);
            }
            gui.refreshInfo();
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;

        if (gui.isConfirmSlot(slot)) {
            OrderManager.FillResult result = plugin.getOrderManager().fillOrder(player, gui.getOrderId(), gui.collectDepositedItems());
            if (result.error() != null) {
                player.sendMessage(AxOrdersAddon.color(result.error()));
                if (plugin.getOrderManager().getOrder(gui.getOrderId()) == null) {
                    gui.returnDeposits();
                    gui.markClosingHandled();
                    player.closeInventory();
                }
                return;
            }

            gui.clearDeposits();
            gui.returnItems(result.rejectedItems());
            gui.markClosingHandled();
            player.closeInventory();
            return;
        }

        if (gui.isCancelSlot(slot)) {
            gui.returnDeposits();
            gui.markClosingHandled();
            player.closeInventory();
            return;
        }

        if (!gui.isDepositSlot(slot)) return;

        ItemStack current = top.getItem(slot);
        ItemStack cursor = event.getCursor();

        if (cursor == null || cursor.getType().isAir()) {
            if (current == null || current.getType().isAir()) return;
            event.setCursor(current);
            top.setItem(slot, null);
            gui.refreshInfo();
            return;
        }

        if (!gui.matchesOrder(cursor)) {
            player.sendMessage(plugin.msg("fill-invalid-item"));
            return;
        }

        int leftover = gui.addDeposit(cursor);
        if (leftover <= 0) {
            event.setCursor(null);
        } else {
            cursor.setAmount(leftover);
            event.setCursor(cursor);
        }
        gui.refreshInfo();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FillOrderGUI gui) {
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlots().stream().noneMatch(slot -> slot < topSize)) return;

            event.setCancelled(true);
            if (event.getOldCursor() != null && !event.getOldCursor().getType().isAir() && !gui.matchesOrder(event.getOldCursor())) {
                event.getWhoClicked().sendMessage(plugin.msg("fill-invalid-item"));
            }
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof OrdersGUI)) return;
    
        int topSize = event.getView().getTopInventory().getSize();
    
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof FillOrderGUI gui)) return;
        if (gui.isClosingHandled()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            gui.returnDeposits();
            player.updateInventory();
        });
    }
}
