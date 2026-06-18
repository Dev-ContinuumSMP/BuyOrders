package com.example.buyorders.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMatcherRegressionTest {

    private static final NamespacedKey SEA_TOKEN_KEY = new NamespacedKey("pyrofishing", "pyro_item");
    private static final NamespacedKey UNRELATED_KEY = new NamespacedKey("otherplugin", "runtime_data");

    @Test
    void rejectsRenamedVanillaItemWhenRequiredItemHasPersistentCustomId() {
        ItemStack required = item(Material.NAUTILUS_SHELL, "Sea Token", null, Map.of(SEA_TOKEN_KEY, "sea_token"));
        ItemStack submitted = item(Material.NAUTILUS_SHELL, "Sea Token", null, Map.of());

        assertFalse(ItemMatcher.matchesCustomItem(submitted, required));
    }

    @Test
    void acceptsPlayerRenamedItemsWhenRequiredItemHasNoPersistentCustomId() {
        ItemStack required = item(Material.DIAMOND_CHESTPLATE, "Johnny Custom Set", List.of("Owner: Johnny"), Map.of());
        ItemStack submitted = item(Material.DIAMOND_CHESTPLATE, "Johnny Custom Set", List.of("Owner: Johnny"), Map.of());

        assertTrue(ItemMatcher.matchesCustomItem(submitted, required));
    }

    @Test
    void rejectsPlayerRenamedItemsWhenNormalMetaDiffers() {
        ItemStack required = item(Material.DIAMOND_CHESTPLATE, "Johnny Custom Set", List.of("Owner: Johnny"), Map.of());
        ItemStack submitted = item(Material.DIAMOND_CHESTPLATE, "Someone Else Set", List.of("Owner: Johnny"), Map.of());

        assertFalse(ItemMatcher.matchesCustomItem(submitted, required));
    }

    @Test
    void ignoresUnrelatedPersistentDataWhenRequiredItemHasCustomIdentity() {
        ItemStack required = item(Material.NAUTILUS_SHELL, "Sea Token", null, Map.of(
                SEA_TOKEN_KEY, "sea_token",
                UNRELATED_KEY, "required-runtime-value"
        ));
        ItemStack submitted = item(Material.NAUTILUS_SHELL, "Sea Token", null, Map.of(
                SEA_TOKEN_KEY, "sea_token",
                UNRELATED_KEY, "submitted-runtime-value"
        ));

        assertTrue(ItemMatcher.matchesCustomItem(submitted, required));
    }

    private static ItemStack item(Material material, String name, List<String> lore, Map<NamespacedKey, String> pdc) {
        return new StubItemStack(material, name, lore, pdc);
    }

    private static final class StubItemStack extends ItemStack {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final Map<NamespacedKey, String> pdc;
        private final ItemMeta meta;

        private StubItemStack(Material material, String name, List<String> lore, Map<NamespacedKey, String> pdc) {
            super(material);
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.pdc = new LinkedHashMap<>(pdc);
            this.meta = meta(name, lore, this.pdc);
        }

        @Override
        public Material getType() {
            return material;
        }

        @Override
        public ItemMeta getItemMeta() {
            return meta;
        }

        @Override
        public boolean isSimilar(ItemStack stack) {
            if (!(stack instanceof StubItemStack other)) return false;
            return material == other.material
                    && Objects.equals(name, other.name)
                    && Objects.equals(lore, other.lore)
                    && Objects.equals(pdc, other.pdc);
        }
    }

    private static ItemMeta meta(String name, List<String> lore, Map<NamespacedKey, String> pdc) {
        PersistentDataContainer container = persistentDataContainer(pdc);
        return (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasDisplayName" -> name != null;
                    case "getDisplayName" -> name;
                    case "hasLore" -> lore != null && !lore.isEmpty();
                    case "getLore" -> lore;
                    case "hasCustomModelData" -> false;
                    case "getCustomModelData" -> 0;
                    case "getPersistentDataContainer" -> container;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static PersistentDataContainer persistentDataContainer(Map<NamespacedKey, String> pdc) {
        return (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKeys" -> Set.copyOf(pdc.keySet());
                    case "has" -> args[1] == PersistentDataType.STRING && pdc.containsKey(args[0]);
                    case "get" -> args[1] == PersistentDataType.STRING ? pdc.get(args[0]) : null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0F;
        if (returnType == double.class) return 0D;
        if (returnType == char.class) return '\0';
        return null;
    }
}
