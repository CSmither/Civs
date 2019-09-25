package org.redcastlemedia.multitallented.civs.menus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.redcastlemedia.multitallented.civs.Civs;
import org.redcastlemedia.multitallented.civs.civilians.Civilian;
import org.redcastlemedia.multitallented.civs.civilians.CivilianManager;
import org.redcastlemedia.multitallented.civs.menus.alliance.AllianceMenu;
import org.redcastlemedia.multitallented.civs.menus.common.LanguageMenu;
import org.redcastlemedia.multitallented.civs.menus.common.MainMenu;
import org.redcastlemedia.multitallented.civs.menus.regions.BlueprintsMenu;
import org.redcastlemedia.multitallented.civs.menus.regions.RegionListMenu;
import org.redcastlemedia.multitallented.civs.menus.towns.SelectTownMenu;

import lombok.Getter;

public class MenuManager implements Listener {
    private static MenuManager instance = null;
    private static HashMap<UUID, Map<String, Object>> data = new HashMap<>();
    private static HashMap<UUID, ArrayList<MenuHistoryState>> history = new HashMap<>();
    private static HashMap<UUID, String> openMenus = new HashMap<>();
    private static HashMap<String, CustomMenu> menus = new HashMap<>();

    @Getter
    private MenuIcon backButton;
    @Getter
    private MenuIcon prevButton;
    @Getter
    private MenuIcon nextButton;

    public MenuManager() {
        if (Civs.getInstance() != null) {
            Bukkit.getPluginManager().registerEvents(this, Civs.getInstance());
        }
        instance = this;
    }
    public static MenuManager getInstance() {
        if (instance == null) {
            new MenuManager();
        }
        return instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!openMenus.containsKey(uuid)) {
            return;
        }
        Civilian civilian = CivilianManager.getInstance().getCivilian(uuid);
        menus.get(openMenus.get(uuid)).onCloseMenu(civilian, event.getInventory());
        openMenus.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        UUID uuid = event.getWhoClicked().getUniqueId();
        if (!openMenus.containsKey(uuid)) {
            return;
        }
        Civilian civilian = CivilianManager.getInstance().getCivilian(uuid);
        if (event.getCurrentItem() != null) {
            if (backButton.createCVItem(civilian.getLocale())
                    .equivalentItem(event.getCurrentItem(), true, true)) {
                popLastMenu(civilian.getUuid());
                MenuHistoryState menuHistoryState = popLastMenu(civilian.getUuid());
                openMenuFromHistory((Player) event.getWhoClicked(), menuHistoryState.getMenuName(), menuHistoryState.getData());
                event.setCancelled(true);
                return;
            } else if (prevButton.createCVItem(civilian.getLocale())
                    .equivalentItem(event.getCurrentItem(), true, true)) {
                int page = (Integer) getData(civilian.getUuid(), "page");
                putData(civilian.getUuid(), "page", page < 1 ? 0 : page - 1);
                refreshMenu(civilian);
                event.setCancelled(true);
                return;
            } else if (backButton.createCVItem(civilian.getLocale())
                    .equivalentItem(event.getCurrentItem(), true, true)) {
                int page = (Integer) getData(civilian.getUuid(), "page");
                int maxPage = (Integer) getData(civilian.getUuid(), "max-page");
                putData(civilian.getUuid(), "page", page >= maxPage ? maxPage : page + 1);
                refreshMenu(civilian);
                event.setCancelled(true);
                return;
            }
        }

        boolean shouldCancel = menus.get(openMenus.get(uuid)).doActionAndCancel(civilian, event.getCursor(), event.getCurrentItem());
        if (shouldCancel) {
            event.setCancelled(true);
        }
    }

    public void loadMenuConfigs() {
        File menuFolder = new File(Civs.getInstance().getDataFolder(), "menus");
        if (menuFolder.exists()) {
            menuFolder.mkdir();
        }
        File menuFile = new File(menuFolder, "default.yml");
        if (!menuFile.exists()) {
            Civs.logger.severe(Civs.getPrefix() + "Unable to find menu default.yml");
            return;
        }
        try {
            FileConfiguration config = new YamlConfiguration();
            config.load(menuFile);
            backButton = new MenuIcon("back",
                    config.getString("back.icon", "REDSTONE_BLOCK"),
                    config.getString("back.name", "back-button"),
                    config.getString("back.desc", ""));
            prevButton = new MenuIcon("prev",
                    config.getString("prev.icon", "REDSTONE"),
                    config.getString("prev.name", "prev-button"),
                    config.getString("prev.desc", ""));
            prevButton = new MenuIcon("next",
                    config.getString("next.icon", "EMERALD"),
                    config.getString("next.name", "next-button"),
                    config.getString("next.desc", ""));
        } catch (Exception e) {
            Civs.logger.severe(Civs.getPrefix() + "Unable to load menu default.yml");
            e.printStackTrace();
            return;
        }

        {
            AllianceMenu allianceMenu = new AllianceMenu();
            loadConfig(allianceMenu);
            menus.put(allianceMenu.getFileName(), allianceMenu);
        }
        {
            SelectTownMenu selectTownMenu = new SelectTownMenu();
            loadConfig(selectTownMenu);
            menus.put(selectTownMenu.getFileName(), selectTownMenu);
        }
        {
            MainMenu mainMenu = new MainMenu();
            loadConfig(mainMenu);
            menus.put(mainMenu.getFileName(), mainMenu);
        }
        {
            LanguageMenu languageMenu = new LanguageMenu();
            loadConfig(languageMenu);
            menus.put(languageMenu.getFileName(), languageMenu);
        }
        {
            RegionListMenu regionListMenu = new RegionListMenu();
            loadConfig(regionListMenu);
            menus.put(regionListMenu.getFileName(), regionListMenu);
        }
        {
            BlueprintsMenu blueprintsMenu = new BlueprintsMenu();
            loadConfig(blueprintsMenu);
            menus.put(blueprintsMenu.getFileName(), blueprintsMenu);
        }
    }

    private void loadConfig(CustomMenu customMenu) {
        File menuFolder = new File(Civs.getInstance().getDataFolder(), "menus");
        if (menuFolder.exists()) {
            menuFolder.mkdir();
        }
        File menuFile = new File(menuFolder, customMenu.getFileName() + ".yml");
        if (!menuFile.exists()) {
            Civs.logger.severe(Civs.getPrefix() + "Unable to load menu " + customMenu.getFileName());
            return;
        }

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(menuFile);
        } catch (Exception e) {
            Civs.logger.severe(Civs.getPrefix() + "Unable to load menu " + customMenu.getFileName());
        }
        int size = -1;
        int newSize = config.getInt("size", 36);
        size = getInventorySize(newSize);
        String name = config.getString("name", "Unnamed");
        HashMap<Integer, MenuIcon> items = new HashMap<>();
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            MenuIcon menuIcon = new MenuIcon(key, config.getConfigurationSection("items." + key));
            if (menuIcon.getIndex().isEmpty() ||
                    menuIcon.getIndex().get(0) < 0) {
                continue;
            }
            for (Integer i : menuIcon.getIndex()) {
                items.put(i, menuIcon);
            }
        }
        customMenu.loadConfig(items, size, name);
    }

    public void openMenuFromHistory(Player player, String menuName, Map<String, Object> data) {
        if (!menus.containsKey(menuName)) {
            return;
        }
        openMenus.put(player.getUniqueId(), menuName);
        Civilian civilian = CivilianManager.getInstance().getCivilian(player.getUniqueId());
        player.openInventory(menus.get(menuName).createMenuFromHistory(civilian, data));
        if (!history.containsKey(player.getUniqueId())) {
            history.put(player.getUniqueId(), new ArrayList<>());
        }
        MenuHistoryState menuHistoryState = new MenuHistoryState(menuName, getAllData(player.getUniqueId()));
        history.get(player.getUniqueId()).add(menuHistoryState);
    }

    public void openMenu(Player player, String menuName, Map<String, String> params) {
        if (!menus.containsKey(menuName)) {
            return;
        }
        openMenus.put(player.getUniqueId(), menuName);
        Civilian civilian = CivilianManager.getInstance().getCivilian(player.getUniqueId());
        player.openInventory(menus.get(menuName).createMenu(civilian, params));
        if (!history.containsKey(player.getUniqueId())) {
            history.put(player.getUniqueId(), new ArrayList<>());
        }
        MenuHistoryState menuHistoryState = new MenuHistoryState(menuName, getAllData(player.getUniqueId()));
        history.get(player.getUniqueId()).add(menuHistoryState);
    }
    public void refreshMenu(Civilian civilian) {
        if (!openMenus.containsKey(civilian.getUuid())) {
            return;
        }
        Player player = Bukkit.getPlayer(civilian.getUuid());
        player.openInventory(menus.get(openMenus.get(civilian.getUuid())).createMenu(civilian));
    }

    public static int getInventorySize(int count) {
        int size = 9;
        if (count > size) {
            size = count + 9 - (count % 9);
            if (count % 9 == 0) {
                size -= 9;
            }
        }
        return Math.min(size, 54);
    }
    public static void addHistory(UUID uuid, CustomMenu customMenu, Map<String, Object> data) {
        if (!history.containsKey(uuid)) {
            history.put(uuid, new ArrayList<>());
        }
        MenuHistoryState menuHistoryState = new MenuHistoryState(customMenu.getFileName(), data);
        history.get(uuid).add(menuHistoryState);
    }
    public static MenuHistoryState popLastMenu(UUID uuid) {
        if (!history.containsKey(uuid) ||
                history.get(uuid).isEmpty()) {
            MenuHistoryState menuHistoryState = new MenuHistoryState("main", new HashMap<>());
            return menuHistoryState;
        }
        MenuHistoryState menuHistoryState = history.get(uuid).get(history.get(uuid).size() - 1);
        history.get(uuid).remove(history.get(uuid).size() - 1);
        return menuHistoryState;
    }
    public static void clearHistory(UUID uuid) {
        history.remove(uuid);
    }
    public static Map<String, Object> getAllData(UUID uuid) {
        return data.get(uuid);
    }
    public static Object getData(UUID uuid, String key) {
        Map<String, Object> dataMap = data.get(uuid);
        if (dataMap == null) {
            return null;
        }
        return dataMap.get(key);
    }
    public static void putData(UUID uuid, String key, Object value) {
        data.get(uuid).put(key, value);
    }
    public static void setNewData(UUID uuid, Map<String, Object> newData) {
        data.put(uuid, newData);
    }
    public static void clearData(UUID uuid) {
        data.remove(uuid);
    }
}
