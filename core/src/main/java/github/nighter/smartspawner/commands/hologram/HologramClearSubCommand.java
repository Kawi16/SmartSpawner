package github.nighter.smartspawner.commands.hologram;

import com.mojang.brigadier.context.CommandContext;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.BaseSubCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class HologramClearSubCommand extends BaseSubCommand {

    public HologramClearSubCommand(SmartSpawner plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getPermission() {
        return "smartspawner.hologram";
    }

    @Override
    public String getDescription() {
        return "Clear all text display holograms";
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        try {
            // Execute the Minecraft command to kill all text_display entities
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:kill @e[type=text_display]");
            
            // Send success message to player
            plugin.getMessageService().sendMessage(sender, "command_hologram_cleared");

            return 1;
        } catch (Exception e) {
            plugin.getLogger().severe("Error clearing holograms: " + e.getMessage());
            plugin.getMessageService().sendMessage(sender, "command_hologram_clear_error");
            return 0;
        }
    }
}
