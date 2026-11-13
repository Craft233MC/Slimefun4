package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} is responsible for all events centered around a {@link SlimefunBackpack}.
 * This also includes the {@link Cooler}
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @author NihilistBrew
 * @author AtomicScience
 * @author VoidAngel
 * @author John000708
 * @see SlimefunBackpack
 * @see PlayerBackpack
 */
@Slf4j
public class BackpackListener implements Listener {
    // Stores the player uuid maps to the opening backpack uuid
    private final Set<UUID> openingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> backpacks = new ConcurrentHashMap<>();
    private final Map<UUID, SlimefunBackpack> backpackInstances = new ConcurrentHashMap<>();

    public void register(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder(false) instanceof PlayerBackpack backpack) {
            openingPlayers.remove(p.getUniqueId());
            backpacks.remove(p.getUniqueId());
            backpackInstances.remove(p.getUniqueId());
            // The changedSlot computation and refreshSnapshot is moved to the
            // ProfileDataController#saveBackpackInventory
            Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInventory(backpack);
            SoundEffect.BACKPACK_CLOSE_SOUND.playFor(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearPlayerState(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        clearPlayerState(e.getPlayer().getUniqueId());
    }

    private void clearPlayerState(UUID uuid) {
        openingPlayers.remove(uuid);
        backpacks.remove(uuid);
        backpackInstances.remove(uuid);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (!openingPlayers.isEmpty() && openingPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (backpacks.containsKey(e.getPlayer().getUniqueId())) {
            ItemStack item = e.getItemDrop().getItemStack();
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem instanceof SlimefunBackpack) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSwap(PlayerSwapHandItemsEvent e) {
        if (!openingPlayers.isEmpty() && openingPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        var player = e.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }

        ItemStack item = player.getInventory().getItemInOffHand();
        if (item == null || item.getType().isAir()) {
            return;
        }

        SlimefunItem backpack = SlimefunItem.getByItem(item);
        if (backpack instanceof SlimefunBackpack) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent atEntityEvent) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(atEntityEvent.getPlayer().getUniqueId())) {
            atEntityEvent.setCancelled(true);
            return;
        }
        var player = atEntityEvent.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }
        atEntityEvent.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent atEntityEvent) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(atEntityEvent.getPlayer().getUniqueId())) {
            atEntityEvent.setCancelled(true);
            return;
        }
        var player = atEntityEvent.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }
        atEntityEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(e.getWhoClicked().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        SlimefunBackpack slimefunBackpack =
                backpackInstances.get(e.getWhoClicked().getUniqueId());
        if (slimefunBackpack != null) {
            if (e.getClick() == ClickType.NUMBER_KEY) {
                // Prevent disallowed items from being moved using number keys.
                if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                    ItemStack hotbarItem = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());

                    if (!isAllowed(slimefunBackpack, hotbarItem)) {
                        e.setCancelled(true);
                    }
                }
            } else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                    // Fixes #3265 - Don't move disallowed items using the off hand.
                    ItemStack offHandItem = e.getWhoClicked().getInventory().getItemInOffHand();

                    if (!isAllowed(slimefunBackpack, offHandItem)) {
                        e.setCancelled(true);
                    }
                } else {
                    // Fixes #3664 - Do not swap any of these backpacks to your off hand.
                    if (e.getCurrentItem() != null && SlimefunItem.getByItem(e.getCurrentItem()) == slimefunBackpack) {
                        e.setCancelled(true);
                    }
                }
            } else if (!isAllowed(slimefunBackpack, e.getCurrentItem())) {
                e.setCancelled(true);
            }
        }
    }

    private boolean isAllowed(@Nonnull SlimefunBackpack backpack, @Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        return backpack.isItemAllowed(item, SlimefunItem.getByItem(item));
    }

    @ParametersAreNonnullByDefault
    public void openBackpack(Player p, ItemStack item, SlimefunBackpack backpack) {
        if (backpack.canUse(p, true)
                && !PlayerProfile.get(p, profile -> {
                    // PlayerProfile#get 的回调可能落在数据库读取线程上，而后续逻辑全都要碰玩家。
                    // 先切回玩家所属的区域线程（Folia）/主线程（Paper）再继续。
                    if (Bukkit.isPrimaryThread()) {
                        openBackpackInternal(p, item, backpack);
                    } else {
                        Slimefun.runSyncAtEntity(() -> openBackpackInternal(p, item, backpack), 0, p);
                    }
                })) {
            Slimefun.getLocalization().sendMessage(p, "messages.opening-backpack");
        }
    }

    @ParametersAreNonnullByDefault
    private void openBackpackInternal(Player p, ItemStack item, SlimefunBackpack backpackItem) {
        if (item.getAmount() != 1) {
            Slimefun.getLocalization().sendMessage(p, "backpack.no-stack", true);
            return;
        }
        var meta = item.getItemMeta();
        // Check if the backpack owner is online
        if (!PlayerBackpack.isOwnerOnline(meta)) {
            Slimefun.getLocalization().sendMessage(p, "backpack.not-backpack-owner");
            return;
        }

        if (PlayerBackpack.getBackpackUUID(meta).isEmpty()
                && PlayerBackpack.getBackpackID(meta).isEmpty()) {
            // Create backpack
            Slimefun.getLocalization().sendMessage(p, "backpack.set-name", true);
            UUID puuid = p.getUniqueId();
            ItemStack itemCopy = item.clone();
            Slimefun.getChatCatcher().scheduleCatcher(puuid, name -> {
                Player player = Bukkit.getPlayer(puuid);
                // Don't let player quit server during the input
                if (player == null) return;
                var pInv = player.getInventory();
                // Check if the player change the amount of item
                if (item.getAmount() != 1) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.no-stack", true);
                    return;
                }
                // Check if the item is modified during the chat input
                if (!Objects.equals(itemCopy, item)) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.not-original-item", true);
                    return;
                }
                // Check if the player moves the item
                if (!item.equals(pInv.getItemInMainHand()) && !item.equals(pInv.getItemInOffHand())) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.not-original-item", true);
                    return;
                }
                // Create the backpack, and bind
                PlayerProfile.get(player, profile -> {
                    PlayerBackpack.bindItem(
                            item,
                            Slimefun.getDatabaseManager()
                                    .getProfileDataController()
                                    .createBackpack(player, name, profile.nextBackpackNum(), backpackItem.getSize()));
                });
            });
            return;
        }

        /*
         * If the current Player is already viewing a backpack (for whatever reason),
         * terminate that view.
         */
        if (backpacks.containsKey(p.getUniqueId())) {
            p.closeInventory();
        }
        UUID puuid = p.getUniqueId();
        openingPlayers.add(puuid);
        PlayerBackpack.getAsync(item)
                .exceptionally(ex -> {
                    Slimefun.logger().warning(ex.getLocalizedMessage());
                    log.error("Exception Occurred During Reading PlayerBackPack.", ex);
                    return null;
                })
                .thenAccept(bp -> {
                    // Folia: 不能用 ThreadUtils.getMainThreadExecutor()。那是 Paper 的
                    // MCUtil.MAIN_EXECUTOR，内部调用 MinecraftServer#execute()，Folia 上会直接抛
                    // UnsupportedOperationException，回调根本不会执行、openingPlayers 也会永久残留。
                    // 打开背包属于玩家实体归属的操作，必须交给 EntityScheduler（区域线程）。
                    Slimefun.runSyncAtEntity(
                            () -> {
                                openingPlayers.remove(puuid);
                                if (!p.isOnline()) {
                                    return;
                                }
                                // Check if the backpack item contains invalid backpack data
                                if (bp == null || bp.isInvalid()) {
                                    return;
                                }
                                // Check if someone else is currently viewing this backpack
                                if (backpacks.containsValue(bp.getUniqueId())
                                        || !bp.getInventory().getViewers().isEmpty()) {
                                    Slimefun.getLocalization().sendMessage(p, "backpack.already-open", true);
                                    return;
                                }
                                SoundEffect.BACKPACK_OPEN_SOUND.playAt(p.getLocation(), SoundCategory.PLAYERS);
                                bp.open(p);
                                backpacks.put(puuid, bp.getUniqueId());
                                backpackInstances.put(puuid, backpackItem);
                            },
                            0,
                            p);
                })
                .exceptionally(ex -> {
                    // 兜底：任何失败（包括实体退休导致调度失败）都不能把玩家永久锁在 openingPlayers 里
                    openingPlayers.remove(puuid);
                    Slimefun.logger().warning(ex.getLocalizedMessage());
                    log.error("Exception Occurred During Opening PlayerBackPack.", ex);
                    return null;
                });
    }
}
