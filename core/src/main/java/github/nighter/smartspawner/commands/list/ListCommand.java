package github.nighter.smartspawner.commands.list;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.language.MessageService;
import github.nighter.smartspawner.spawner.utils.SpawnerMobHeadTexture;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ListCommand {
    private final SmartSpawner plugin;
    private final SpawnerManager spawnerManager;
    private final LanguageManager languageManager;
    private final MessageService messageService;
    private final UserPreferenceCache userPreferenceCache;
    private static final int SPAWNERS_PER_PAGE = 45;

    public ListCommand(SmartSpawner plugin) {
        this.plugin = plugin;
        this.spawnerManager = plugin.getSpawnerManager();
        this.languageManager = plugin.getLanguageManager();
        this.messageService = plugin.getMessageService();
        this.userPreferenceCache = plugin.getUserPreferenceCache();
    }

    // World selection GUI logic (unchanged)
    public void openWorldSelectionGUI(Player player) {
        if (!player.hasPermission("smartspawner.list")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        // Get all loaded worlds with spawners
        List<World> worlds = Bukkit.getWorlds().stream()
                .filter(world -> spawnerManager.countSpawnersInWorld(world.getName()) > 0)
                .collect(Collectors.toList());

        // Check if there are any custom worlds with spawners
        List<World> customWorlds = worlds.stream()
                .filter(world -> !isDefaultWorld(world.getName()))
                .collect(Collectors.toList());

        boolean hasCustomWorlds = !customWorlds.isEmpty();

        // Calculate inventory size - use original 27 size if only default worlds, otherwise adapt
        int size = hasCustomWorlds ? Math.max(27, (int) Math.ceil((worlds.size() + 2) / 7.0) * 9) : 27;

        Inventory inv = Bukkit.createInventory(new WorldSelectionHolder(),
                size, languageManager.getGuiTitle("gui_title_world_selection"));

        // If we only have default worlds, use the original layout
        if (!hasCustomWorlds) {
            // Create buttons for default worlds
            ItemStack overworldButton = createWorldButtonIfWorldExists("world", Material.GRASS_BLOCK,
                    languageManager.getGuiTitle("world_buttons.overworld.name"));

            ItemStack netherButton = createWorldButtonIfWorldExists("world_nether", Material.NETHERRACK,
                    languageManager.getGuiTitle("world_buttons.nether.name"));

            ItemStack endButton = createWorldButtonIfWorldExists("world_the_end", Material.END_STONE,
                    languageManager.getGuiTitle("world_buttons.end.name"));

            // Set buttons in the original layout
            if (overworldButton != null) inv.setItem(11, overworldButton);
            if (netherButton != null) inv.setItem(13, netherButton);
            if (endButton != null) inv.setItem(15, endButton);
        }
        // If we have custom worlds, use a more flexible layout
        else {
            int slot = 10; // Start at second row, second column
            int row = 1;

            // Add default worlds first (if they exist)
            if (addWorldButtonIfExists(inv, "world", Material.GRASS_BLOCK,
                    languageManager.getGuiTitle("world_buttons.overworld.name"), slot)) {
                slot++;
            }

            if (addWorldButtonIfExists(inv, "world_nether", Material.NETHERRACK,
                    languageManager.getGuiTitle("world_buttons.nether.name"), slot)) {
                slot++;
            }

            if (addWorldButtonIfExists(inv, "world_the_end", Material.END_STONE,
                    languageManager.getGuiTitle("world_buttons.end.name"), slot)) {
                slot++;
            }

            // Add custom worlds
            for (World world : customWorlds) {
                // Move to next row if we've reached the end of this one
                if (slot % 9 == 8) {
                    row++;
                    slot = 9 * row + 1; // First slot in the next row (skipping the border)
                }

                // Stop if we've filled the inventory
                if (slot >= size) {
                    break;
                }

                // Add the world button
                Material material = getMaterialForWorldType(world.getEnvironment());
                addWorldButton(inv, world.getName(), material, formatWorldName(world.getName()), slot++);
            }
        }

        player.openInventory(inv);
    }

    private boolean isDefaultWorld(String worldName) {
        return worldName.equals("world") || worldName.equals("world_nether") || worldName.equals("world_the_end");
    }

    private ItemStack createWorldButtonIfWorldExists(String worldName, Material material, String displayName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null && spawnerManager.countSpawnersInWorld(worldName) > 0) {
            return createWorldButton(material, displayName, getWorldDescription(worldName));
        }
        return null;
    }

    private boolean addWorldButtonIfExists(Inventory inv, String worldName, Material material, String displayName, int slot) {
        World world = Bukkit.getWorld(worldName);
        if (world != null && spawnerManager.countSpawnersInWorld(worldName) > 0) {
            addWorldButton(inv, worldName, material, displayName, slot);
            return true;
        }
        return false;
    }

    private void addWorldButton(Inventory inv, String worldName, Material material, String displayName, int slot) {
        // For custom worlds, get formatted name with color based on environment
        if (!isDefaultWorld(worldName)) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                World.Environment environment = world.getEnvironment();
                String namePath;
                switch (environment) {
                    case NORMAL -> namePath = "world_buttons.custom_overworld.name";
                    case NETHER -> namePath = "world_buttons.custom_nether.name";
                    case THE_END -> namePath = "world_buttons.custom_end.name";
                    default -> namePath = "world_buttons.custom_default.name";
                }
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("world_name", displayName);
                displayName = languageManager.getGuiTitle(namePath, placeholders);
            }
        }

        ItemStack button = createWorldButton(material, displayName, getWorldDescription(worldName));
        inv.setItem(slot, button);
    }

    private Material getMaterialForWorldType(World.Environment environment) {
        return switch (environment) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.ENDER_PEARL; // For custom environments
        };
    }

    private String formatWorldName(String worldName) {
        // Convert something like "my_custom_world" to "My Custom World"
        return Arrays.stream(worldName.replace('_', ' ').split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private List<String> getWorldDescription(String worldName) {
        List<String> description = new ArrayList<>();
        int physicalSpawners = spawnerManager.countSpawnersInWorld(worldName);
        int totalWithStacks = spawnerManager.countTotalSpawnersWithStacks(worldName);

        // Use path for default worlds if available, otherwise use custom world description based on environment
        String path;
        if (worldName.equals("world")) {
            path = "world_buttons.overworld.lore";
        } else if (worldName.equals("world_nether")) {
            path = "world_buttons.nether.lore";
        } else if (worldName.equals("world_the_end")) {
            path = "world_buttons.end.lore";
        } else {
            // Get environment for custom world
            World world = Bukkit.getWorld(worldName);
            World.Environment environment = world != null ? world.getEnvironment() : World.Environment.NORMAL;

            // Select appropriate lore based on environment
            switch (environment) {
                case NORMAL -> path = "world_buttons.overworld.lore";
                case NETHER -> path = "world_buttons.nether.lore";
                case THE_END -> path = "world_buttons.end.lore";
                default -> path = "world_buttons.custom_default.lore";
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("total", String.valueOf(physicalSpawners));
        placeholders.put("total_stacked", languageManager.formatNumber(totalWithStacks));

        // Get the lore as string array and convert to List
        String[] loreArray = languageManager.getGuiItemLore(path, placeholders);
        return Arrays.asList(loreArray);
    }

    private ItemStack createWorldButton(Material material, String name, List<String> lore) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    // Basic spawner list GUI opener with default filter and sort
    public void openSpawnerListGUI(Player player, String worldName, int page) {
        // Check for saved preferences
        UserPreferenceCache.UserPreference preference = userPreferenceCache.getPreference(player.getUniqueId(), worldName);

        if (preference != null) {
            // Use saved preferences if available
            openSpawnerListGUI(player, worldName, page, preference.getFilterOption(), preference.getSortOption());
        } else {
            // Use default preferences
            openSpawnerListGUI(player, worldName, page, FilterOption.ALL, SortOption.DEFAULT);
        }
    }

    public void saveUserPreference(Player player, String worldName, FilterOption filter, SortOption sort) {
        userPreferenceCache.savePreference(player.getUniqueId(), worldName, filter, sort);
    }

    // Main spawner list GUI method with filter and sort options
    public void openSpawnerListGUI(Player player, String worldName, int page, FilterOption filter, SortOption sortType) {
        if (!player.hasPermission("smartspawner.list")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        // Get all spawners in the world
        List<SpawnerData> worldSpawners = spawnerManager.getAllSpawners().stream()
                .filter(spawner -> spawner.getSpawnerLocation().getWorld().getName().equals(worldName))
                .collect(Collectors.toList());

        // Apply filtering
        if (filter == FilterOption.ACTIVE) {
            worldSpawners = worldSpawners.stream()
                    .filter(spawner -> !spawner.getSpawnerStop())
                    .collect(Collectors.toList());
        } else if (filter == FilterOption.INACTIVE) {
            worldSpawners = worldSpawners.stream()
                    .filter(SpawnerData::getSpawnerStop)
                    .collect(Collectors.toList());
        }

        // Apply sorting
        switch (sortType) {
            case STACK_SIZE_ASC -> worldSpawners.sort(Comparator.comparingInt(SpawnerData::getStackSize));
            case STACK_SIZE_DESC -> worldSpawners.sort(Comparator.comparingInt(SpawnerData::getStackSize).reversed());
            default -> {} // Default sorting (by ID) - no additional sorting needed
        }

        int totalPages = (int) Math.ceil((double) worldSpawners.size() / SPAWNERS_PER_PAGE);
        page = Math.max(1, Math.min(page, totalPages));

        String worldTitle;
        switch (worldName) {
            case "world" -> worldTitle = languageManager.getGuiTitle("world_buttons.overworld.name");
            case "world_nether" -> worldTitle = languageManager.getGuiTitle("world_buttons.nether.name");
            case "world_the_end" -> worldTitle = languageManager.getGuiTitle("world_buttons.end.name");
            default -> {
                // For custom worlds, format the name nicely
                worldTitle = formatWorldName(worldName);
            }
        }

        Map<String, String> titlePlaceholders = new HashMap<>();
        worldTitle = ChatColor.stripColor(worldTitle);
        titlePlaceholders.put("world", worldTitle);
        titlePlaceholders.put("current", String.valueOf(page));
        titlePlaceholders.put("total", String.valueOf(totalPages));

        String title = languageManager.getGuiTitle("gui_title_spawner_list", titlePlaceholders);

        Inventory inv = Bukkit.createInventory(new SpawnerListHolder(page, totalPages, worldName, filter, sortType),
                54, title);

        // Calculate start and end indices for current page
        int startIndex = (page - 1) * SPAWNERS_PER_PAGE;
        int endIndex = Math.min(startIndex + SPAWNERS_PER_PAGE, worldSpawners.size());

        // Populate inventory with spawners
        for (int i = startIndex; i < endIndex; i++) {
            SpawnerData spawner = worldSpawners.get(i);
            inv.addItem(createSpawnerInfoItem(spawner));
        }

        // Add filter and sort controls
        addControlButtons(inv, filter, sortType);

        // Add navigation buttons
        if (page > 1) {
            inv.setItem(45, createNavigationButton(Material.SPECTRAL_ARROW, "navigation.previous_page"));
        }

        // Back button
        inv.setItem(49, createNavigationButton(Material.RED_STAINED_GLASS_PANE, "navigation.back"));

        if (page < totalPages) {
            inv.setItem(53, createNavigationButton(Material.SPECTRAL_ARROW, "navigation.next_page"));
        }

        player.openInventory(inv);
    }

    // Create the new consolidated filter and sort buttons
    private void addControlButtons(Inventory inv, FilterOption currentFilter, SortOption currentSort) {
        // Filter button (updated material and position) - moved to slot 46
        ItemStack filterButton = createEnhancedControlButton(
                Material.CAULDRON,
                "filter",
                currentFilter
        );

        // Sort button (updated material and position) - moved to slot 52
        ItemStack sortButton = createEnhancedControlButton(
                Material.HOPPER,
                "sort",
                currentSort
        );

        // Updated positions for better symmetry
        inv.setItem(48, filterButton);
        inv.setItem(50, sortButton);
    }

    private ItemStack createEnhancedControlButton(Material material, String controlType, Enum<?> currentOption) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta == null) return button;

        Map<String, String> placeholders = new HashMap<>();

        if (controlType.equals("filter")) {
            FilterOption currentFilter = (FilterOption) currentOption;

            // Set up color placeholders for all filter options
            for (FilterOption option : FilterOption.values()) {
                // Set active color for current option, white for others
                String colorKey = option.getName() + "_color";
                if (option == currentFilter) {
                    placeholders.put(colorKey, languageManager.getColorCode("filter." + option.getName() + ".color"));
                } else {
                    placeholders.put(colorKey, "&f");
                }
            }

            // Set button name
            meta.setDisplayName(languageManager.getGuiItemName("filter.button.name"));

        } else if (controlType.equals("sort")) {
            SortOption currentSort = (SortOption) currentOption;

            // Set up color placeholders for all sort options
            for (SortOption option : SortOption.values()) {
                // Set active color for current option, white for others
                String colorKey = option.getName() + "_color";
                if (option == currentSort) {
                    placeholders.put(colorKey, languageManager.getColorCode("sort." + option.getName() + ".color"));
                } else {
                    placeholders.put(colorKey, "&f");
                }
            }

            // Set button name
            meta.setDisplayName(languageManager.getGuiItemName("sort.button.name"));
        }

        // Set the lore using the appropriate button lore path and the placeholders
        String lorePath = controlType + ".button.lore";
        meta.setLore(languageManager.getGuiItemLoreAsList(lorePath, placeholders));

        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createNavigationButton(Material material, String namePath) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(languageManager.getGuiItemName(namePath));
        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createSpawnerInfoItem(SpawnerData spawner) {
        // Get the custom head for the spawner's entity type
        EntityType entityType = spawner.getEntityType();
        ItemStack spawnerItem;
        ItemMeta meta;
        if (entityType == null) {
            spawnerItem = new ItemStack(Material.SPAWNER);
            meta = spawnerItem.getItemMeta();
            if (meta == null) return spawnerItem;
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_UNBREAKABLE);
        } else {
            spawnerItem = SpawnerMobHeadTexture.getCustomHead(entityType);
            meta = spawnerItem.getItemMeta();
            if (meta == null) return spawnerItem;
        }
        Location loc = spawner.getSpawnerLocation();

        // Set display name with formatted spawner ID
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(spawner.getSpawnerId()));
        meta.setDisplayName(languageManager.getGuiItemName("spawner_item_list.name", placeholders));

        // Add entity type
        placeholders.put("entity", languageManager.getFormattedMobName(entityType));

        // Add stack size
        placeholders.put("size", String.valueOf(spawner.getStackSize()));

        // Add status
        if (spawner.getSpawnerStop()) {
            placeholders.put("status_color", "&#ff6b6b");
            placeholders.put("status_text", "Inactive");
        } else {
            placeholders.put("status_color", "&#00E689");
            placeholders.put("status_text", "Active");
        }

        // Add location
        placeholders.put("x", String.valueOf(loc.getBlockX()));
        placeholders.put("y", String.valueOf(loc.getBlockY()));
        placeholders.put("z", String.valueOf(loc.getBlockZ()));

        // Get the lore with placeholders replaced
        List<String> lore = Arrays.asList(languageManager.getGuiItemLore("spawner_item_list.lore", placeholders));

        // Set lore and apply meta
        meta.setLore(lore);
        spawnerItem.setItemMeta(meta);
        return spawnerItem;
    }

    @Getter
    public enum FilterOption {
        ALL("filter.all"),
        ACTIVE("filter.active"),
        INACTIVE("filter.inactive");

        private final String langPath;

        FilterOption(String langPath) {
            this.langPath = langPath;
        }

        public FilterOption getNextOption() {
            return switch (this) {
                case ALL -> ACTIVE;
                case ACTIVE -> INACTIVE;
                case INACTIVE -> ALL;
            };
        }

        public String getColorPath() {
            return langPath + ".color";
        }

        public String getName() {
            return name().toLowerCase();
        }
    }

    @Getter
    public enum SortOption {
        DEFAULT("sort.default"),
        STACK_SIZE_ASC("sort.stack_size_asc"),
        STACK_SIZE_DESC("sort.stack_size_desc");

        private final String langPath;

        SortOption(String langPath) {
            this.langPath = langPath;
        }

        public SortOption getNextOption() {
            return switch (this) {
                case DEFAULT -> STACK_SIZE_ASC;
                case STACK_SIZE_ASC -> STACK_SIZE_DESC;
                case STACK_SIZE_DESC -> DEFAULT;
            };
        }

        public String getColorPath() {
            return langPath + ".color";
        }

        public String getName() {
            return name().toLowerCase();
        }
    }
}