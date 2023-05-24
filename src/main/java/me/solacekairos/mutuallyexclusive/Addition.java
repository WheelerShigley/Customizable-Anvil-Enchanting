package me.solacekairos.mutuallyexclusive;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;

public class Addition implements Listener {
    Map<String, Integer> maximums;

    MutuallyExclusive plugin;
    public Addition(MutuallyExclusive plugin) {
        this.plugin = plugin;

        maximums = new HashMap<>();
        getMaximums(plugin);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent clicked) {
        if(clicked.getView().getType() != InventoryType.ANVIL) { return; }

        final InventoryView clicked_view = clicked.getView();
        //if they did not (click into the anvil or shift click into it), don't run further
        if( !(clicked.getClickedInventory() == clicked_view.getTopInventory() || ( clicked.getClickedInventory() == clicked_view.getBottomInventory() && clicked.isShiftClick() ) )/*!*/ ) { return; }

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            continueAfterWait(clicked);
        }, 1L);

        //Bukkit.getPluginManager().callEvent( new AnvilCraftEvent(clicked.getView(), (Player)clicked.getWhoClicked(), recipe );
        //recipe.removeIngredients(view);
        /*{

        new BukkitRunnable() {
            public void run() {
                ItemStack[] currentIngredients = {view.getItem(0), view.getItem(1) };
                for(AnvilRecipe recipe : recipes) {
                    if(recipe.matches(currentIngredients)) {
                        view.getTopInventory().setItem(2, recipe.getResult(view));
                        break;
                    }
                }
            }
        }.runTask(plugin);
        */
    }

    private void continueAfterWait(InventoryClickEvent clicked) {
        ItemStack[] anvil_inventory = clicked.getView().getTopInventory().getContents();
        ItemStack item = anvil_inventory[0]; ItemStack modifier = anvil_inventory[1];
        if(item == null || modifier == null) { return; }

        //item and modifier must match or modifier must be a book
        if( ( item.getType() != modifier.getType() ) && (modifier.getType() != Material.ENCHANTED_BOOK) ) { return; }

        //get result enchantment levels
        Map<Enchantment, Integer> result_enchants = getResult( item.getType(), item.getEnchantments(), modifier.getEnchantments() );

        //remove and add all enchants from original item, set as result
        ItemStack result_item = item.clone();
        for( Map.Entry<Enchantment, Integer> mapEntry : item.getItemMeta().getEnchants().entrySet() ) { result_item.removeEnchantment( mapEntry.getKey() ); }
        result_item.addUnsafeEnchantments( result_enchants );

        //set result to result item
        //plugin.getServer().getScheduler().runTask(plugin, () -> clicked.getInventory().setRepairCost(10));
        clicked.getClickedInventory().setItem(2, result_item);
        clicked.getViewers().forEach( (person) -> ( (Player)person ).updateInventory() );
    }

    private Map<Enchantment, Integer> getResult(Material item_type, Map<Enchantment, Integer> primary, Map<Enchantment, Integer> secondary) {
        Map<Enchantment, Integer> result_enchants = new HashMap<>();
        int value = 0, temp; boolean not_already_included;
        //add intersections
        for( Map.Entry<Enchantment, Integer> secondaryEntry : secondary.entrySet() ) {
            not_already_included = true;
            for( Map.Entry<Enchantment, Integer> primaryEntry : primary.entrySet() ) {
                if( primaryEntry.getKey() == secondaryEntry.getKey() ) {
                    value = primaryEntry.getValue();
                    if (value == secondaryEntry.getValue()) { value++; }

                    //ensure value is not too large
                    temp = plugin.getConfig().getInt( primaryEntry.getKey().getName() );
                    if(temp < value) { value = temp; }

                    System.out.println(primaryEntry.getKey() + ":" + value);
                    result_enchants.put(primaryEntry.getKey(), value);

                    not_already_included = false;
                    break;
                }
            }
        }
        //add unions
        for( Map.Entry<Enchantment, Integer> primaryEntry : primary.entrySet() ) {
            boolean not_included = true;
            for( Map.Entry<Enchantment, Integer> secondaryEntry : secondary.entrySet() ) {
                if( primaryEntry.getKey() == secondaryEntry.getKey() ) { not_included = false; }
            }
            if(not_included) {
                System.out.println(primaryEntry.getKey() + ":::" + primaryEntry.getValue() );
                result_enchants.put( primaryEntry.getKey(), primaryEntry.getValue() );
            }
        }
        for( Map.Entry<Enchantment, Integer> secondaryEntry : secondary.entrySet() ) {
            boolean not_included = true;
            for( Map.Entry<Enchantment, Integer> primaryEntry : primary.entrySet() ) {
                if( primaryEntry.getKey() == secondaryEntry.getKey() ) { not_included = false; }
            }
            if(not_included) {
                System.out.println(secondaryEntry.getKey() + "::::" + secondaryEntry.getValue() );
                result_enchants.put( secondaryEntry.getKey(), secondaryEntry.getValue() );
            }
        }

        return result_enchants;
    }

    private void getMaximums(MutuallyExclusive plugin) {
        String[] names = new String[] {
                "MENDING","DURABILITY","VANISHING_CURSE","WATER_WORKER","PROTECTION_EXPLOSIONS",
                "BINDING_CURSE","DEPTH_STRIDER","PROTECTION_FALL","PROTECTION_FIRE","SWIFT_SNEAK",
                "FROST_WALKER","PROTECTION_PROJECTILE","PROTECTION_ENVIRONMENTAL","OXYGEN",
                "SOUL_SPEED","THORNS","DAMAGE_ARTHROPODS","FIRE_ASPECT","LOOT_BONUS_MOBS",
                "DAMAGE_ALL","DAMAGE_UNDEAD","SWEEPING_EDGE","CHANNELING","ARROW_FIRE","IMPALING",
                "ARROW_INFINITE","LOYALTY","RIPTIDE","MULTISHOT","PIERCING","ARROW_DAMAGE",
                "ARROW_KNOCKBACK","QUICK_CHARGE","DIG_SPEED","LOOT_BONUS_BLOCKS","LUCK","LURE",
                "SILK_TOUCH"
        };
        FileConfiguration config_file = plugin.getConfig();
        for(int i = 0; i < names.length; i++) {
            maximums.put( names[i], config_file.getInt(names[i]) );
        }
        for( Map.Entry<String, Integer> mapEntry : maximums.entrySet() ) {
            System.out.println( mapEntry.getKey() +" "+ mapEntry.getValue() );
        }
    }

    private Integer getMaximum(String enchant_id_name) {
        for( Map.Entry<String, Integer> mapEntry : maximums.entrySet() ) {
            //System.out.println( mapEntry.getKey() +" "+ mapEntry.getValue() );
            if( mapEntry.getKey().equals(enchant_id_name) ) {
                return mapEntry.getValue();
            }
        }
        return 0;
    }
}
