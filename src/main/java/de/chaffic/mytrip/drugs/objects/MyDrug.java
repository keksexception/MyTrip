package de.chaffic.mytrip.drugs.objects;

import com.google.gson.reflect.TypeToken;
import de.chaffic.mytrip.MyTrip;
import io.github.chafficui.CrucialAPI.Utils.customItems.CrucialItem;
import io.github.chafficui.CrucialAPI.exceptions.CrucialException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.*;

import static de.chaffic.mytrip.utils.ConfigStrings.PREFIX;

public class MyDrug extends CrucialItem {
    private ArrayList<String[]> effects = new ArrayList<>();
    private ArrayList<String> commands = new ArrayList<>();
    private long duration;
    private long effectDelay;
    private int overdose;
    private String particle;
    private int addict;

    public MyDrug(String name, String head) {
        super("drug");
        this.isHead = true;
        this.name = name;
        this.material = head;
        unregisteredDrugs.add(this);
    }

    public MyDrug(String name, Material material) {
        super("drug");
        this.name = name;
        this.material = material.name();
        unregisteredDrugs.add(this);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = super.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (isRegistered) {
                meta.setDisplayName(ChatColor.WHITE + meta.getDisplayName());
            } else {
                meta.setDisplayName(ChatColor.RED + meta.getDisplayName());
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void register() throws CrucialException {
        super.register();
        unregisteredDrugs.remove(this);
    }

    public ArrayList<String> getCommands() {
        return commands;
    }

    public void setCommands(ArrayList<String> commands) {
        this.commands = commands;
    }

    public void addCommand(String command) {
        commands.add(command);
    }

    public ArrayList<String[]> getEffects() {
        return effects;
    }

    public void addEffect(String[] effect) {
        this.effects.add(effect);
    }

    public void setEffects(ArrayList<String[]> effects) {
        this.effects = effects;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        if(duration < 0) return;
        this.duration = duration;
    }

    public long getEffectDelay() {
        return effectDelay;
    }

    public void setEffectDelay(long effectDelay) {
        if(effectDelay < 0) return;
        this.effectDelay = effectDelay;
    }

    public int getOverdose() {
        return overdose;
    }

    public void setOverdose(int overdose) {
        if(overdose < 0 || overdose > 99) return;
        this.overdose = overdose;
    }

    public String getParticle() {
        return particle;
    }

    public void setParticle(String particle) {
        this.particle = particle;
    }

    public int getAddictionProbability() {
        return addict;
    }

    public void setAddictionProbability(int addict) {
        if(addict < 0 || addict > 100) return;
        this.addict = addict;
    }

    // Static
    private static final Set<MyDrug> unregisteredDrugs = new HashSet<>();
    private static final MyTrip plugin = MyTrip.getPlugin(MyTrip.class);

    public static MyDrug getUnregisteredDrugById(UUID id) {
        for (MyDrug drug : unregisteredDrugs) {
            if (drug.id.equals(id)) {
                return drug;
            }
        }
        return null;
    }

    public static MyDrug getByName(String name) {
        for (CrucialItem item : CRUCIAL_ITEMS) {
            if (item instanceof MyDrug && item.getName().equalsIgnoreCase(name)) {
                return (MyDrug) item;
            }
        }
        return null;
    }

    public static void clearUnregisteredDrugs() {
        unregisteredDrugs.clear();
    }

    public static void doDrug(Player p, ItemStack stack) {
        MyDrug drug = (MyDrug) getByStack(stack);
        long duration = drug.duration * 20;
        long delay = drug.effectDelay * 20;

        //instant visuals
        int amount = stack.getAmount();
        if (amount > 1) {
            stack.setAmount(amount - 1);
            p.getInventory().setItemInMainHand(stack);
        }
        if (amount == 1) {
            p.getInventory().getItemInMainHand().setAmount(0);
        }
        p.playSound(p.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 10, 29);
        DrugPlayer dp = DrugPlayer.getPlayer(p.getUniqueId());
        if(dp == null) {
            dp = new DrugPlayer(p);
            DrugPlayer.addPlayer(dp);
        }
        if(dp.consume(drug)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100*20, 0));
        }
        doEffects(p, drug, duration, delay);
    }

    public static void deleteDrug(MyDrug drug, CommandSender sender) {

        if (CrucialItem.getByStack(drug.getItemStack()) != null) {
            drug.delete();
            sender.sendMessage(PREFIX + drug.name + " was deleted."); //TODO: Localization
            return;
        }
        sender.sendMessage(PREFIX + drug.name + " is no existing drug!"); //TODO: Localization
    }

    private static void doEffects(Player p, MyDrug drug, long duration, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 10, 29);
            long currentDuration;

            //effects
            for (String[] effect : drug.effects) {
                try {
                    PotionEffectType type = PotionEffectType.getByName(effect[0]);
                    int strength = Integer.parseInt(effect[1]);

                    if (p.hasPotionEffect(Objects.requireNonNull(type))) {
                        currentDuration = Objects.requireNonNull(p.getPotionEffect(type)).getDuration();
                    } else {
                        currentDuration = 0L;
                    }
                    p.removePotionEffect(type);
                    p.addPotionEffect(new PotionEffect(type, (int) (currentDuration + duration), strength - 1));
                } catch (Exception ex) {
                    plugin.error("Error 012: Tryied to run drug " + drug.name +
                            " but failed. Is PotionEffect " + effect[0] + " legal?"); //TODO: Localization
                }
            }
            Objects.requireNonNull(DrugPlayer.getPlayer(p.getUniqueId())).subDose(drug);
        }, delay);
    }

    public static void saveAll() throws IOException {
        ArrayList<MyDrug> drugs = new ArrayList<>();
        for (CrucialItem item : CRUCIAL_ITEMS) {
            if (item instanceof MyDrug) {
                drugs.add((MyDrug) item);
            }
        }
        plugin.fileManager.saveToJson("drugs.json", drugs);
    }

    public static void loadAll() throws IOException, CrucialException {
        for (Object drug : plugin.fileManager.loadFromJson("drugs.json", new TypeToken<ArrayList<MyDrug>>(){}.getType())) {
            if (drug instanceof MyDrug) {
                ((MyDrug) drug).unregister();
                ((MyDrug) drug).register();
            }
        }
    }
}