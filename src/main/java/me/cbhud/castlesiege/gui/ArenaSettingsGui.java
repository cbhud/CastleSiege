package me.cbhud.castlesiege.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cbhud.castlesiege.CastleSiege;
import me.cbhud.castlesiege.arena.Arena;
import me.cbhud.castlesiege.arena.ArenaState;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class ArenaSettingsGui {

    private final CastleSiege plugin;
    private final ArenaEditChatListener chat;
    private final String arenaId;

    public ArenaSettingsGui(CastleSiege plugin, ArenaEditChatListener chat, String arenaId) {
        this.plugin = plugin;
        this.chat = chat;
        this.arenaId = arenaId;
    }

    public void open(Player player) {
        Arena arena = plugin.getArenaManager().getArenaById(arenaId);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena not found: " + arenaId);
            return;
        }

        Gui gui = Gui.gui()
                .title(Component.text("Arena Settings: " + arenaId))
                .rows(3)
                .create();

        boolean locked = arena.getState() == ArenaState.IN_GAME;
        if (locked) {
            gui.setItem(13, ItemBuilder.from(Material.BARRIER)
                    .name(Component.text(ChatColor.RED + "Editing disabled"))
                    .lore(List.of(Component.text(ChatColor.GRAY + "Arena is IN_GAME. Stop it first.")))
                    .asGuiItem(e -> { e.setCancelled(true); })
            );
        }

        // Hardcore toggle
        gui.setItem(10, hardcoreItem(arena, locked));

        // Auto-start
        gui.setItem(11, numberEditItem("auto-start", "Auto Start (seconds)", Material.CLOCK, locked,
                e -> startChatEdit(e, ArenaEditChatListener.PendingKey.AUTO_START)));

        // Game timer
        gui.setItem(12, numberEditItem("game-timer", "Game Timer (seconds)", Material.CLOCK, locked,
                e -> startChatEdit(e, ArenaEditChatListener.PendingKey.GAME_TIMER)));

        // Min players
        gui.setItem(14, numberEditItem("min-players", "Min Players", Material.PLAYER_HEAD, locked,
                e -> startChatEdit(e, ArenaEditChatListener.PendingKey.MIN_PLAYERS)));

        // Max players
        gui.setItem(15, numberEditItem("max-players", "Max Players", Material.PLAYER_HEAD, locked,
                e -> startChatEdit(e, ArenaEditChatListener.PendingKey.MAX_PLAYERS)));

        // Optional: Spawn setters (recommended)
        gui.setItem(16, spawnSetterItem("lobby-spawn", "Set Lobby Spawn to your position", Material.EMERALD, locked));
        gui.setItem(19, spawnSetterItem("king-spawn", "Set King Spawn to your position", Material.GOLDEN_HELMET, locked));
        gui.setItem(20, spawnSetterItem("defenders-spawn", "Set Defenders Spawn to your position", Material.BLUE_BANNER, locked));
        gui.setItem(21, spawnSetterItem("attackers-spawn", "Set Attackers Spawn to your position", Material.RED_BANNER, locked));

        // Back
        gui.setItem(26, ItemBuilder.from(Material.ARROW)
                .name(Component.text(ChatColor.YELLOW + "Back"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    new ArenaEditorGui(plugin).open((Player) e.getWhoClicked());
                })
        );

        gui.open(player);
    }

    private GuiItem hardcoreItem(Arena arena, boolean locked) {
        boolean hardcore = ArenaYmlEditor.getBoolean(plugin, arenaId, "hardcore", false);

        Material mat = hardcore ? Material.LIME_DYE : Material.GRAY_DYE;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY + "Current: " + (hardcore ? "true" : "false")));
        lore.add(Component.text(ChatColor.DARK_GRAY + (locked ? "Locked while IN_GAME" : "Click to toggle")));

        return ItemBuilder.from(mat)
                .name(Component.text(ChatColor.GOLD + "Hardcore"))
                .lore(lore)
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (locked) return;

                    boolean newValue = !hardcore;
                    boolean ok = ArenaYmlEditor.setBoolean(plugin, arenaId, "hardcore", newValue);
                    if (ok) {
                        plugin.getArenaManager().reload();
                        ((Player) e.getWhoClicked()).sendMessage(ChatColor.GREEN + "Hardcore set to " + newValue);
                        open((Player) e.getWhoClicked());
                    } else {
                        ((Player) e.getWhoClicked()).sendMessage(ChatColor.RED + "Failed to save arenas.yml");
                    }
                });
    }

    private GuiItem numberEditItem(String key, String title, Material mat, boolean locked, java.util.function.Consumer<InventoryClickEvent> onClick) {
        int value = ArenaYmlEditor.getInt(plugin, arenaId, key, 0);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY + "Current: " + value));
        lore.add(Component.text(ChatColor.DARK_GRAY + (locked ? "Locked while IN_GAME" : "Click to edit (chat input)")));

        return ItemBuilder.from(mat)
                .name(Component.text(ChatColor.GOLD + title))
                .lore(lore)
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (locked) return;
                    onClick.accept(e);
                });
    }

    private void startChatEdit(InventoryClickEvent e, ArenaEditChatListener.PendingKey key) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        player.closeInventory();
        chat.begin(player, arenaId, key);    }

    private GuiItem spawnSetterItem(String key, String title, Material mat, boolean locked) {
        String raw = ArenaYmlEditor.getSpawnRaw(plugin, arenaId, key);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.GRAY + "Current:"));
        lore.add(Component.text(ChatColor.DARK_GRAY + raw));
        lore.add(Component.text(ChatColor.DARK_GRAY + (locked ? "Locked while IN_GAME" : "Click to set to your position")));

        return ItemBuilder.from(mat)
                .name(Component.text(ChatColor.GOLD + title))
                .lore(lore)
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (locked) return;
                    Player p = (Player) e.getWhoClicked();

                    boolean ok = ArenaYmlEditor.setSpawn(plugin, arenaId, key, p.getLocation());
                    if (ok) {
                        plugin.getArenaManager().reload();
                        p.sendMessage(ChatColor.GREEN + "Updated " + key + " for arena '" + arenaId + "'.");
                        open(p);
                    } else {
                        p.sendMessage(ChatColor.RED + "Failed to save arenas.yml");
                    }
                });
    }
}