package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.inventory.ItemStack;

public class StringUtil {
    public static String itemStackToString(ItemStack item) {
        if (item == null) {
            return "null";
        }

        var sfItem = SlimefunItem.getByItem(item);

        if (sfItem != null) {
            return String.format(
                    "ItemStack [sfId=%s (Addon=%s)] (type=%s, amount=%d)",
                    sfItem.getId(), sfItem.getAddon().getName(), item.getType(), item.getAmount());
        } else {
            return String.format("ItemStack (type=%s, amount=%d)", item.getType(), item.getAmount());
        }
    }
}
