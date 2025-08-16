package github.nighter.smartspawner.hooks.protections.api;

import github.nighter.smartspawner.SmartSpawner;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.api.EnchantKeys;
import su.nightexpress.excellentenchants.api.EnchantRegistry;
import su.nightexpress.nightcore.util.bridge.RegistryType;

import java.util.Map;

public class ExcellentEnchantsHook {
    public static boolean canBreakSpawner(@NotNull Player player, @NotNull Location location) {
        if(!SmartSpawner.getInstance().getConfig().getBoolean("spawner_break.excellent-enchants.enabled", false)) return true;
        Map<Enchantment, Integer> enchants = player.getInventory().getItemInMainHand().getEnchantments();
        boolean canBreak = true;
        for(int i = 0; i < SmartSpawner.getInstance().getConfig().getStringList("spawner_break.excellent-enchants.enchants").size() && canBreak; i++) {
            String id = SmartSpawner.getInstance().getConfig().getStringList("spawner_break.excellent-enchants.enchants").get(i);
            boolean exists = EnchantRegistry.isRegistered(id.toLowerCase());
            if(!exists) continue;
            NamespacedKey key = EnchantKeys.custom(id.toLowerCase());
            Enchantment enchantment = RegistryType.ENCHANTMENT.getRegistry().get(key);
            if(!enchants.containsKey(enchantment)) canBreak = false;
        }
        return canBreak;
    }
}
