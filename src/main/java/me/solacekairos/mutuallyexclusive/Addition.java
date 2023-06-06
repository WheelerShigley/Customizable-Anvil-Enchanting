package me.solacekairos.mutuallyexclusive;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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
        Map<Enchantment, Integer> result_enchants = new HashMap<>();
        if(modifier.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)modifier.getItemMeta();
            result_enchants = sortEnchants( getResult( item.getType(), item.getEnchantments(), bookmeta.getStoredEnchants() ) );
        } else {
            result_enchants = sortEnchants( getResult( item.getType(), item.getEnchantments(), modifier.getEnchantments() ) );
        }

        //remove and add all enchants from original item, set as result
        ItemStack result_item = item.clone();
        for( Map.Entry<Enchantment, Integer> mapEntry : item.getItemMeta().getEnchants().entrySet() ) { result_item.removeEnchantment( mapEntry.getKey() ); }
        result_item.addUnsafeEnchantments( result_enchants );

        //set cost CONTINUE HERE:
        //( (Repairable)result_item.getItemMeta() ).setRepairCost( ( 2*( (Repairable)item.getItemMeta() ).getRepairCost() )+1 );

        ( (AnvilInventory)clicked.getView().getTopInventory() ).setRepairCost( calculateCost( item.getEnchantments(), modifier.getEnchantments() ) );

        //set result to result item
        clicked.getView().getTopInventory().setItem(2, result_item);
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
                result_enchants.put( primaryEntry.getKey(), primaryEntry.getValue() );
            }
        }
        for( Map.Entry<Enchantment, Integer> secondaryEntry : secondary.entrySet() ) {
            boolean not_included = true;
            for( Map.Entry<Enchantment, Integer> primaryEntry : primary.entrySet() ) {
                if( primaryEntry.getKey() == secondaryEntry.getKey() ) { not_included = false; }
            }
            if(not_included) {
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

    private int calculateCost( Map<Enchantment, Integer> primary, Map<Enchantment,Integer> secondary ) {
        int cost = 0;
        for( Map.Entry<Enchantment, Integer> mapEntry : primary.entrySet() ) {
            cost += mapEntry.getValue() * plugin.getConfig().getInt( mapEntry.getKey().getName().toUpperCase() + "_COST" );
        }
        for( Map.Entry<Enchantment, Integer> mapEntry : secondary.entrySet() ) {
            cost += mapEntry.getValue() * plugin.getConfig().getInt( mapEntry.getKey().getName().toUpperCase() + "_COST" );
        }
        return cost;
    }

    private String getCommonName(Enchantment name) {
        switch( name.getName().toUpperCase() ) {
            case "ARROW_DAMAGE":                return "Power";
            case "ARROW_FIRE":                  return "Flame";
            case "ARROW_INFINITE":              return "Infinity";
            case "BINDING_CURSE":               return "Binding";
            case "CHANNELING":                  return "Channeling";
            case "DAMAGE_ALL":                  return "Sharpness";
            case "DAMAGE_ARTHROPODS":           return "Bane of Arthropods";
            case "DAMAGE_UNDEAD":               return "Smite";
            case "DEPTH_STRIDER":               return "Depth Strider";
            case "DIG_SPEED":                   return "Efficiency";
            case "DURABILITY":                  return "Unbreaking";
            case "FIRE_ASPECT":                 return "Fire Aspect";
            case "FROST_WALKER":                return "Frost Walker";
            case "IMPALING":                    return "Impaling";
            case "KNOCKBACK":                   return "Knockback";
            case "LOOT_BONUS_BLOCKS":           return "Fortune";
            case "LOOT_BONUS_MOBS":             return "Looting";
            case "LOYALTY":                     return "Loyalty";
            case "LUCK":                        return "Luck of the Sea";
            case "LURE":                        return "Lure";
            case "MENDING":                     return "Mending";
            case "MULTISHOT":                   return "Multishot";
            case "OXYGEN":                      return "Respiration";
            case "PIERCING":                    return "Piercing";
            case "PROTECTION_ENVIRONMENTAL":    return "Protection";
            case "PROTECTION_EXPLOSIONS":       return "Blast Protection";
            case "PROTECTION_FALL":             return "Feather Falling";
            case "PROTECTION_PROJECTIVE":       return "Projectile Protection";
            case "QUICK_CHARGE":                return "Quick Charge";
            case "RIPTIDE":                     return "Riptide";
            case "SILK_TOUCH":                  return "Silk Touch";
            case "SOUL_SPEED":                  return "Soul Speed";
            case "SWEEPING_EDGE":               return "Sweeping Edge";
            case "SWIFT_SNEAK":                 return "Swift Sneak";
            case "THORNS":                      return "Thorns";
            case "VANISHING_CURSE":             return "Curse of Vanishing";
            case "WATER_WORKER":                return "Aqua Affinity";
            default:                            return "Unknown";
        }
    }

    private Map<Enchantment, Integer> sortEnchants(Map<Enchantment, Integer> enchantments) {
        Map<Enchantment, Integer> result = new HashMap<>();
        //prepare for easier sorting, using arrays
        int position = 0;
        Enchantment[] enchant = new Enchantment[enchantments.size()]; String[] names = new String[enchantments.size()]; Integer[] values = new Integer[enchantments.size()];
        for( Map.Entry<Enchantment, Integer> mapEntry : enchantments.entrySet() ) {
            names[position] = getCommonName( mapEntry.getKey() );
            enchant[position] = mapEntry.getKey();
            values[position++] = mapEntry.getValue();
        }
        //bubble sort
        int minimum_index; String name_minimum;
        for(int i = 0; i < names.length-1; i++ ) {
            name_minimum = names[i];
            minimum_index = i;
            for(int j = i+1; j < names.length; j++) {
                if( 0 < name_minimum.compareTo( names[j] ) ) {
                    name_minimum = names[j];
                    minimum_index = j;
                }
            }
            if(minimum_index == i) { continue; }
            //swap
            String string_temp = names[minimum_index]; Integer integer_temp = values[minimum_index]; Enchantment enchant_temp = enchant[minimum_index];
            names[minimum_index] = names[i]; values[minimum_index] = values[i]; enchant[minimum_index] = enchant[i];
            names[i] = string_temp; values[i] = integer_temp; enchant[i] = enchant_temp;
        }
        //construct return
        for(int i = 0; i < enchantments.size(); i++) {
            result.put( enchant[i], values[i] );
        }
        return result;
    }
}
