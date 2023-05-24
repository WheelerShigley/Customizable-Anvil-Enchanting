package me.solacekairos.mutuallyexclusive;

import org.bukkit.inventory.Recipe;
import java.util.Iterator;
import static org.bukkit.Bukkit.getServer;

public class Removal {

    public void removeAnvilRecipies() {
        Iterator<Recipe> item = getServer().recipeIterator();
        Recipe current;

        while( item.hasNext() ) {
            current = item.next();
            System.out.println( current.getResult().getType().toString() );
        }
    }

}
