package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ResetLoreCommand extends AbstractCommand {
    Tregmine plugin;
    TregminePlayer player;

    public ResetLoreCommand(Tregmine instance) {
        super(instance, "resetlore");
        plugin = instance;
    }

    @Override
    public boolean handlePlayer(TregminePlayer sender, String[] args) {
        if (sender.getWorld().getName().equalsIgnoreCase("vanilla") || sender.isInVanillaWorld()) {
            player.sendMessage(ChatColor.RED + "You cannot use that command in this world!");
            return true;
        }
        if (!sender.getIsAdmin()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to reset lores.");
            return true;
        }
        player = sender;
        Inventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (ItemStack item : contents) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                List<String> lore = new ArrayList<String>();
                lore.add("Flags reset by: " + player.getName());
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        player.sendMessage(ChatColor.GOLD + "Any items that had a lore have lost their lore.");
        return true;
    }
}
