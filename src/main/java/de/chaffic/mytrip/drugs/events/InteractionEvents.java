package de.chaffic.mytrip.drugs.events;

import de.chaffic.mytrip.MyTrip;
import de.chaffic.mytrip.drugs.objects.DrugTool;
import de.chaffic.mytrip.drugs.objects.MyDrug;
import de.chaffic.mytrip.utils.PlayerUtils;
import io.github.chafficui.CrucialAPI.Utils.customItems.CrucialItem;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import static de.chaffic.mytrip.utils.ConfigStrings.*;

public class InteractionEvents implements Listener {
    private static final MyTrip plugin = MyTrip.getPlugin(MyTrip.class);

    @EventHandler
    public void onDrugConsume(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> {
                if (item != null && item.getType() != Material.AIR) {
                    CrucialItem crucialItem = CrucialItem.getByStack(item);
                    if (crucialItem instanceof MyDrug && crucialItem.isRegistered()) {
                        e.setCancelled(true);
                        if (PlayerUtils.hasPermissions(p, PERM_USE_ANY, PERM_USE_ + crucialItem.getName())) {
                            MyDrug.doDrug(p, item);
                        } else {
                            p.sendMessage(PREFIX + "§cYou do not have the permission to do this!"); //TODO: Localization
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrugCraft(PrepareItemCraftEvent e) {
        for (LivingEntity entity : e.getViewers()) {
            Recipe recipe = e.getRecipe();
            if (recipe != null) {
                CrucialItem crucialItem = CrucialItem.getByStack(recipe.getResult());
                BlockState state = entity.getTargetBlock(null, 5).getState();
                if (DrugTool.isDrugSet(state)) {
                    if (!(crucialItem instanceof MyDrug) || !crucialItem.isRegistered()) {
                        e.getInventory().setResult(new ItemStack(Material.AIR));
                    } else if (!PlayerUtils.hasPermissions(entity, PERM_CRAFT_ANY, PERM_CRAFT_ + crucialItem.getName())) {
                        e.getInventory().setResult(new ItemStack(Material.AIR));
                    } else {
                        e.getInventory().setResult(crucialItem.getItemStack());
                    }
                } else {
                    if (crucialItem instanceof MyDrug) {
                        e.getInventory().setResult(new ItemStack(Material.AIR));
                    } else if (crucialItem instanceof DrugTool) {
                        e.getInventory().setResult(crucialItem.getItemStack());
                        if (!PlayerUtils.hasPermissions((entity), PERM_CRAFT_ANY, PERM_CRAFT_ + crucialItem.getName())) {
                            e.getInventory().setResult(new ItemStack(Material.AIR));
                        }
                    }
                }
            }
        }
    }
}
