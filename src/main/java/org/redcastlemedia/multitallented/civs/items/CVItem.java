package org.redcastlemedia.multitallented.civs.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.redcastlemedia.multitallented.civs.Civs;
import org.redcastlemedia.multitallented.civs.ConfigManager;
import org.redcastlemedia.multitallented.civs.civilians.Civilian;
import org.redcastlemedia.multitallented.civs.localization.LocaleConstants;
import org.redcastlemedia.multitallented.civs.localization.LocaleManager;
import org.redcastlemedia.multitallented.civs.util.Util;

import io.lumine.mythic.lib.api.item.NBTItem;
import lombok.Getter;
import lombok.Setter;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;

/**
 *
 * @author Multi
 */
public class CVItem {
    private static String CUSTOM_ITEM_KEY = Civs.NAME + "_CUSTOM_ITEM";
    private static String BOUND_OWNER_KEY = Civs.NAME + "_OWNER";

    private Material mat;
    private int qty;
    private double chance;
    private String displayName = null;

    @Getter @Setter
    private UUID ownerBound = null;
    @Getter @Setter
    private String civItemName = null;
    @Getter @Setter
    private Integer customModelData = null;
    @Getter @Setter
    private String mmoItemType = null;

    @Setter @Getter
    private String mmoItemName = null;

    @Getter @Setter
    private String group = null;
    private List<String> lore = new ArrayList<>();

    public CVItem(Material mat, int qty, int chance, String displayName, List<String> lore) {
        this.mat = mat;
        this.qty = qty;
        this.chance = ((double) chance) / 100;
        this.displayName = displayName;
        this.lore = lore;
    }

    public CVItem(Material mat, int qty, int chance, String displayName) {
        this.mat = mat;
        this.qty = qty;
        this.chance = ((double) chance) / 100;
        this.displayName = displayName;
    }

    public CVItem(Material mat, int qty, int chance) {
        this.mat = mat;
        this.qty = qty;
        this.chance = ((double) chance) / 100;
    }

    public CVItem(Material mat, int qty) {
        this.mat = mat;
        this.qty = qty;
        this.chance = 1;
    }

    public void setChance(double chance) {
        this.chance = chance / 100;
    }

    public static CVItem createCVItemFromString(String materialString) {
        return createCVItemFromString(ConfigManager.getInstance().getDefaultLanguage(), materialString);
    }

    public static CVItem createCVItemFromString(String locale, String materialString)  {
        boolean isMMOItem = materialString.contains("mi:");
        boolean isCivItem = materialString.contains("civ:");
        if (isMMOItem) {
            materialString = materialString.replace("mi:", "");
        }

        String quantityString = "1";
        String chanceString = "100";
        String nameString = null;
        String itemType = "";
        String data = "";
        Material mat;

        String[] splitString;


        for (;;) {
            int asteriskIndex = materialString.indexOf("*");
            int percentIndex = materialString.indexOf("%");
            int nameIndex = materialString.indexOf(".");
            int dataIndex = materialString.indexOf("^");
            if (asteriskIndex != -1 && asteriskIndex > percentIndex && asteriskIndex > nameIndex && asteriskIndex > dataIndex) {
                splitString = materialString.split("\\*");
                quantityString = splitString[splitString.length - 1];
                materialString = splitString[0];
            } else if (percentIndex != -1 && percentIndex > asteriskIndex && percentIndex > nameIndex && percentIndex > dataIndex) {
                splitString = materialString.split("%");
                chanceString = splitString[splitString.length - 1];
                materialString = splitString[0];
            } else if (nameIndex != -1 && nameIndex > percentIndex && nameIndex > asteriskIndex && nameIndex > dataIndex) {
                splitString = materialString.split("\\.");
                nameString = splitString[splitString.length - 1];
                materialString = splitString[0];
            } else if (dataIndex != -1 && dataIndex > percentIndex && dataIndex > asteriskIndex && dataIndex > nameIndex) {
                splitString = materialString.split("\\^");
                data = splitString[splitString.length - 1];
                materialString = splitString[0];
            } else {
                if (isMMOItem) {
                    itemType = materialString.toUpperCase();
                    mat = Material.STONE;
                } else if (isCivItem) {
                    itemType = materialString.replace("civ:", "").toLowerCase();
                    mat = Material.STONE;
                } else {
                    mat = getMaterialFromString(materialString);
                }
                break;
            }
        }


        if (mat == null) {
            mat = Material.STONE;
            Civs.logger.severe(Civs.getPrefix() + "Unable to parse material " + materialString);
        }
        int quantity = Integer.parseInt(quantityString);
        int chance = Integer.parseInt(chanceString);

        if (isMMOItem) {
            return getMmoItemAsCvItem(nameString, itemType, mat, quantity, chance, data);
        }
        if (isCivItem) {
            return getCivItem(itemType, quantity, chance);
        }
        CVItem cvItem;
        if (nameString == null) {
            cvItem = new CVItem(mat, quantity, chance);
        } else {
            String displayName = LocaleManager.getInstance().getTranslation(locale, "item-" + nameString + LocaleConstants.NAME_SUFFIX);
            List<String> lore = new ArrayList<>();
            lore.addAll(Util.textWrap(ConfigManager.getInstance().getCivsItemPrefix() +
                    LocaleManager.getInstance().getTranslation(locale, "item-" + nameString + LocaleConstants.DESC_SUFFIX)));
            cvItem = new CVItem(mat, quantity, chance, displayName, lore);
        }
        if (data != null && !data.isEmpty()) {
            cvItem.setCustomModelData(Integer.parseInt(data));
        }
        return cvItem;
    }

    private static CVItem getCivItem(String itemType, int quantity, int chance) {
        CivItem civItem = ItemManager.getInstance().getItemType(itemType);
        if (civItem == null) {
            return null;
        }
        CVItem cvItem = civItem.clone();
        cvItem.setCivItemName(itemType);
        cvItem.setQty(quantity);
        cvItem.setChance(chance);
        return cvItem;
    }

    @NotNull
    private static CVItem getMmoItemAsCvItem(String nameString, String itemType, Material mat, int quantity, int chance, String data) {
        if (Civs.mmoItems == null) {
            Civs.logger.severe(Civs.getPrefix() + "Unable to create MMOItem because MMOItems is disabled");
            return new CVItem(mat, quantity, chance);
        }
        Type mmoItemType = Civs.mmoItems.getTypes().get(itemType);
        if (mmoItemType == null) {
            Civs.logger.severe(Civs.getPrefix() + "MMOItem type " + itemType + " not found");
            return new CVItem(mat, quantity, chance);
        }
        if (nameString == null) {
            Civs.logger.severe(Civs.getPrefix() + "Invalid MMOItem " + itemType + " did not provide item name");
            return new CVItem(mat, quantity, chance);
        }
        int level = 0;
        if (data != null && !data.isEmpty()) {
            level = Integer.parseInt(data);
        }
        MMOItem mmoItem = MMOItems.plugin.getMMOItem(mmoItemType, nameString, level, null);
        ItemStack item = mmoItem.newBuilder().build();
        CVItem cvItem = new CVItem(item.getType(), quantity, chance, item.getItemMeta().getDisplayName(),
                item.getItemMeta().getLore());
        cvItem.mmoItemName = nameString;
        cvItem.mmoItemType = itemType;
        return cvItem;
    }

    public static boolean isCustomItem(ItemStack itemStack) {
        return itemStack != null && itemStack.hasItemMeta() &&
                itemStack.getItemMeta() != null &&
                isCustomItem(itemStack.getItemMeta());
    }

    public static void translateItem(Civilian civilian, ItemStack itemStack) {
        Player player = Bukkit.getPlayer(civilian.getUuid());
        if (itemStack.getItemMeta() == null) {
            return;
        }
        String nameString;
        if (Civs.getInstance() != null && itemStack.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(Civs.getInstance(), CUSTOM_ITEM_KEY), PersistentDataType.STRING)) {
            nameString = itemStack.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(Civs.getInstance(), CUSTOM_ITEM_KEY), PersistentDataType.STRING);
        } else {
            String itemDisplayName = itemStack.getItemMeta().getLore().get(1);
            nameString = ChatColor.stripColor(itemDisplayName
                    .replace(ChatColor.stripColor(ConfigManager.getInstance().getCivsItemPrefix()), "").toLowerCase());
        }

        String displayName = LocaleManager.getInstance().getTranslation(player,
                "item-" + nameString + LocaleConstants.NAME_SUFFIX);
        List<String> lore = new ArrayList<>();
        lore.addAll(Util.textWrap(civilian,
                LocaleManager.getInstance().getTranslation(player,
                        "item-" + nameString + LocaleConstants.DESC_SUFFIX)));
        itemStack.getItemMeta().setDisplayName(displayName);
        itemStack.getItemMeta().setLore(lore);
    }

    private static boolean isCustomItem(ItemMeta itemMeta) {
        if (Civs.getInstance() != null && itemMeta.getPersistentDataContainer()
                .has(new NamespacedKey(Civs.getInstance(), CUSTOM_ITEM_KEY), PersistentDataType.STRING)) {
            return true;
        }

        List<String> lore = itemMeta.getLore();
        return lore != null && !lore.isEmpty() &&
                LocaleManager.getInstance().hasTranslation(
                        ConfigManager.getInstance().getDefaultLanguage(),
                        "item-" + ChatColor.stripColor(lore.get(0)) + LocaleConstants.NAME_SUFFIX);
    }

    private static Material getMaterialFromString(String materialString) {
        return Material.valueOf(materialString.replaceAll(" ", "_").toUpperCase());
    }

    public boolean equivalentItem(ItemStack iss) {
        return equivalentItem(iss, false);
    }

    public static boolean isCivsItem(ItemStack is) {
        if (is == null || !is.hasItemMeta()) {
            return false;
        }
        ItemMeta im = is.getItemMeta();
        if (im == null || im.getDisplayName() == null) {
            return false;
        }
        if (Civs.getInstance() != null && im.getPersistentDataContainer().has(new NamespacedKey(Civs.getInstance(), Civs.NAME), PersistentDataType.STRING)) {
            return true;
        }
        if (im.getLore() == null || im.getLore().size() < 2 ||
                ItemManager.getInstance().getItemType(ChatColor.stripColor(im.getLore().get(1))) == null) {
            return false;
        }
        return true;
    }

    public CivItem getCivItem() {
        if (civItemName != null) {
            return ItemManager.getInstance().getItemType(civItemName);
        }
        if (lore.size() < 2) {
            return null;
        }
        return ItemManager.getInstance().getItemType(lore.get(1));
    }

    public static CVItem createFromItemStack(ItemStack is) {
        CVItem cvItem = null;
        if (Civs.getInstance() != null && is.getItemMeta() != null && is.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(Civs.getInstance(), Civs.NAME), PersistentDataType.STRING)) {
            String civItemName = is.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(Civs.getInstance(), Civs.NAME), PersistentDataType.STRING);
            if (civItemName != null) {
                cvItem = ItemManager.getInstance().getItemType(civItemName);
            }
        }

        if (cvItem == null) {
            if (is.hasItemMeta() && is.getItemMeta().getDisplayName() != null &&
                    !"".equals(is.getItemMeta().getDisplayName())) {
                if (is.getItemMeta().getLore() != null) {
                    cvItem = new CVItem(is.getType(), is.getAmount(), 100, is.getItemMeta().getDisplayName(), is.getItemMeta().getLore());
                } else {
                    cvItem = new CVItem(is.getType(), is.getAmount(), 100, is.getItemMeta().getDisplayName());
                }
                if (is.getItemMeta().hasCustomModelData()) {
                    cvItem.setCustomModelData(is.getItemMeta().getCustomModelData());
                }
            } else {
                cvItem = new CVItem(is.getType(), is.getAmount());
            }
        }

        if (Civs.getInstance() != null && is.getItemMeta() != null && is.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(Civs.getInstance(), BOUND_OWNER_KEY), PersistentDataType.STRING)) {
            String ownerUuidString = is.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(Civs.getInstance(), BOUND_OWNER_KEY), PersistentDataType.STRING);
            if (ownerUuidString != null) {
                cvItem.setOwnerBound(UUID.fromString(ownerUuidString));
            }
        }
        return cvItem;
    }
    public static List<CVItem> createListFromString(String input) {
        String groupName = null;
        List<CVItem> reqs = new ArrayList<>();
        ItemGroupList itemGroupList = new ItemGroupList();
        itemGroupList.findAllGroupsRecursively(input);
        if (itemGroupList.getCircularDependency() != null) {
            Civs.logger.log(Level.SEVERE, "Unable to create items due to circular item group {0}", itemGroupList.getCircularDependency());
            return reqs;
        }
        input = itemGroupList.getInput();
        groupName = itemGroupList.getMainGroup();
        for (String req : input.split(",")) {
            CVItem cvItem = createCVItemFromString(req);
            if (groupName != null) {
                cvItem.setGroup(groupName);
            }
            reqs.add(cvItem);
        }
        return reqs;
    }

    public ItemStack createItemStack() {
        if (mmoItemType != null && mmoItemName != null && Civs.mmoItems != null) {
            Type mmoType = Civs.mmoItems.getTypes().get(mmoItemType);
            MMOItem mmoItem = Civs.mmoItems.getItems().getMMOItem(mmoType, mmoItemName);
            ItemStack itemStack = mmoItem.newBuilder().build();
            if (displayName != null && itemStack.getItemMeta() != null) {
                itemStack.getItemMeta().setDisplayName(displayName);
            }
            if (!lore.isEmpty() && itemStack.getItemMeta() != null) {
                itemStack.getItemMeta().setLore(lore);
            }
            itemStack.setAmount(qty);
            return itemStack;
        }

        ItemStack is = new ItemStack(mat, qty);
        if (displayName != null || (lore != null && !lore.isEmpty()) || customModelData != null) {
            ItemMeta im = null;
            if (!is.hasItemMeta()) {
                im = Bukkit.getItemFactory().getItemMeta(is.getType());
            } else {
                im = is.getItemMeta();
            }
            if (im != null) {
                if (displayName != null) {
                    im.setDisplayName(displayName);
                }
                if (lore != null && !lore.isEmpty()) {
                    im.setLore(lore);
                }
                im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
                if (customModelData != null) {
                    is.getItemMeta().setCustomModelData(customModelData);
                }
                if (Civs.getInstance() != null && civItemName != null) {
                    im.getPersistentDataContainer().set(new NamespacedKey(Civs.getInstance(), Civs.NAME),
                            PersistentDataType.STRING, civItemName);
                }
                is.setItemMeta(im);
            } else {
                Civs.logger.log(Level.WARNING, "Unable to get item meta for {0} material {1}",
                        new Object[] {civItemName, mat});
            }
        }
        return is;
    }

    public boolean equivalentItem(ItemStack iss, boolean useDisplayName) {
        return equivalentItem(iss, useDisplayName, false);
    }

    public boolean equivalentItem(ItemStack iss, boolean useDisplayName, boolean lore) {
        if (mmoItemType != null && mmoItemName != null) {
            NBTItem nbtItem = NBTItem.get(iss);
            if (!nbtItem.hasType()) {
                return false;
            }
            if (!mmoItemType.equalsIgnoreCase(nbtItem.getString("MMOITEMS_ITEM_TYPE"))) {
                return false;
            }
            if (!mmoItemName.equalsIgnoreCase(nbtItem.getString("MMOITEMS_ITEM_ID"))) {
                return false;
            }
            return true;
        }
        if (customModelData != null && iss.getItemMeta() != null &&
                iss.getItemMeta().getCustomModelData() != customModelData) {
            return false;
        }
        if (useDisplayName) {
            boolean nullComparison = getDisplayName() == null;
            boolean hasItemMeta = iss.hasItemMeta();
            boolean issNullName = hasItemMeta && iss.getItemMeta().getDisplayName() == null;
            boolean nullName = !hasItemMeta || issNullName;

            boolean equivalentNames = (nullComparison && nullName) || ((!nullComparison && !nullName) && iss.getItemMeta().getDisplayName().equals(getDisplayName()));

            return iss.getType() == getMat() &&
                    equivalentNames;
        } else {
            return iss.getType() == getMat();
        }
    }

    public boolean equivalentCVItem(CVItem iss) {
        return equivalentCVItem(iss, false);
    }

    public boolean equivalentCVItem(CVItem iss, boolean useDisplayName) {
        if (!ObjectUtils.equals(mmoItemName, iss.getMmoItemName()) ||
                !ObjectUtils.equals(mmoItemType, iss.getMmoItemType())) {
            return false;
        }
        if (mmoItemType != null) {
            return true;
        }

        if ((iss.getCustomModelData() != null || customModelData != null) &&
                ObjectUtils.equals(iss.getCustomModelData(), customModelData)) {
            return false;
        }

        if (useDisplayName) {
            return iss.getMat() == getMat() &&
                    ((getDisplayName() == null && iss.getDisplayName() == null) || getDisplayName().equals(iss.getDisplayName()));
        } else {
            return iss.getMat() == getMat();
        }
    }

    public List<String> getLore() {
        return lore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setMat(Material mat) {
        this.mat = mat;
    }

    public Material getMat() {
        return mat;
    }
    public int getQty() {
        return qty;
    }
    public double getChance() {
        return chance;
    }
    public void setQty(int qty) {
        this.qty = qty;
    }
    public void setDisplayName(String name) {
        this.displayName = name;
    }
    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    @Override
    public CVItem clone() {
        CVItem cvItem = new CVItem(mat, qty, (int) chance, displayName, new ArrayList<>(lore));
        cvItem.customModelData = customModelData;
        cvItem.civItemName = civItemName;
        cvItem.mmoItemName = mmoItemName;
        cvItem.mmoItemType = mmoItemType;
        cvItem.setGroup(group);
        return cvItem;
    }
}
