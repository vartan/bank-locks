package com.vartan.BankLocks;

import com.google.inject.Provides;
import com.vartan.BankLocks.model.MoreComponentIDs;
import com.vartan.BankLocks.model.SoundEffects;
import com.vartan.BankLocks.util.InterfaceUtil;
import com.vartan.BankLocks.util.SetUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Bank Locks"
)
public class BankLocksPlugin extends Plugin {
    /** Interfaces from which you can deposit items. */
    private static final Set<Integer> LOCKED_INTERFACES = Set.of(
            ComponentID.BANK_INVENTORY_ITEM_CONTAINER,
            ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER,
            ComponentID.BANK_EQUIPMENT_PARENT,
            ComponentID.BANK_INVENTORY_EQUIPMENT_ITEM_CONTAINER
    );
    /** Interfaces from which you can lock items from being deposited. */
    public static final Set<Integer> LOCKABLE_INTERFACES = Set.of(
            ComponentID.INVENTORY_CONTAINER,
            ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER,

            // Locked interfaces are also lockable. TODO: Compose this from 2 lists instead
            // of copying these ids over.
            ComponentID.BANK_INVENTORY_ITEM_CONTAINER,
            ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER,
            ComponentID.BANK_EQUIPMENT_PARENT,
            ComponentID.BANK_INVENTORY_EQUIPMENT_ITEM_CONTAINER
    );

    /** Identifiers for deposit all inventory buttons. */
    private static final Set<Integer> DEPOSIT_INVENTORY_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_INVENTORY,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_INVENTORY
    );

    /** Identifiers for deposit all equipment buttons. */
    private static final Set<Integer> DEPOSIT_EQUIPMENT_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_EQUIPMENT,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_EQUIPMENT
    );

    @Inject
    private ConfigManager configManager;
    @Inject
    private ItemManager itemManager;

    @Inject
    private Client client;
    @Inject
    private BankLocksConfig config;
    @Inject
    private BankLocksOverlay overlay;
    @Inject
    private OverlayManager overlayManager;

    @Getter
    private Set<Integer> lockedItemIds = new HashSet<>();

    /** Lock image used by the overlay. Must be generated from the main thread. */
    BufferedImage lockImage;

    /** Whether we should write the locked item list to a file when logging out. */
    private boolean dirtyConfig = false;

    public BankLocksPlugin() {
        super();
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        if (client.getGameState() == GameState.LOGGED_IN) {
            save();
        }
        overlayManager.remove(overlay);
    }

    private void save() {
        if (!dirtyConfig) {
            return;
        }
        String commaSeparatedItemIds = lockedItemIds
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        configManager.setConfiguration(BankLocksConfig.CONFIG_GROUP,
                BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME,
                commaSeparatedItemIds);
        dirtyConfig = false;

    }

    private void load() {
        String commaSeparatedItemIds = configManager.getConfiguration(BankLocksConfig.CONFIG_GROUP,
                BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME);
        if (commaSeparatedItemIds != null) {
            lockedItemIds = Arrays.stream(commaSeparatedItemIds.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
        }
    }

    @Subscribe()
    public void onMenuOptionClicked(MenuOptionClicked event) {
        Widget widget = event.getMenuEntry().getWidget();
        if (widget == null) {
            return;
        }
        String option = Text.removeTags(event.getMenuOption());
        int widgetId = widget.getId();

        if (preventDepositAll(event, widgetId)) return;

        preventDepositItem(event, option, widget);

    }

    private void preventDepositItem(MenuOptionClicked event, String option, Widget widget) {
        int itemId = getItemIdOrChildItemId(event.getWidget());
        if (itemId <= 1) {
            return;
        }

        if (option.startsWith("Deposit") || option.equalsIgnoreCase("Bank")) {
            if (shouldPreventDeposit(widget, itemId)) {
                preventMenuOptionClicked(event, itemId);
            }
        }
    }

    private boolean preventDepositAll(MenuOptionClicked event, int widgetId) {
        if(config.preventDepositAll()) {
            if (DEPOSIT_INVENTORY_COMPONENT_IDS.contains(widgetId)) {
                int itemId = shouldPreventDepositAll(InventoryID.INVENTORY);
                if (itemId > 0) {
                    preventMenuOptionClicked(event, itemId);
                }
                return true;
            } else if (DEPOSIT_EQUIPMENT_COMPONENT_IDS.contains(widgetId)) {
                int itemId = shouldPreventDepositAll(InventoryID.EQUIPMENT);
                if (itemId > 0) {
                    preventMenuOptionClicked(event, itemId);
                }
                return true;
            }
        }
        return false;
    }

    private int shouldPreventDepositAll(InventoryID id) {
        ItemContainer itemContainer = client.getItemContainer(id);
        if (itemContainer == null) {
            return -1;
        }
        for (Item item : itemContainer.getItems()) {
            int itemId = item.getId();
            if (lockedItemIds.contains(itemId)) {
                return itemId;
            }
        }
        return -1;
    }


    private void preventMenuOptionClicked(MenuOptionClicked event, int itemId) {
        if (config.playSoundWhenPrevented()) {
            client.playSoundEffect(SoundEffects.GE_TRADE_ERROR);
        }
        ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        String itemName = itemComposition.getName();

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                "Prevented depositing a locked item: " + itemName, null);
        event.consume();
    }

    private boolean isInLockedInterface(Widget widget) {
        return InterfaceUtil.anyInterfaceContainsWidget(LOCKED_INTERFACES, widget, client);
    }

    private boolean isInLockableInterface(Widget widget) {
        return InterfaceUtil.anyInterfaceContainsWidget(LOCKABLE_INTERFACES, widget, client);
    }

    private boolean shouldPreventDeposit(Widget widget, int itemId) {
        if (!isInLockedInterface(widget)) {
            return false;
        }
        return lockedItemIds.contains(itemId);
    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == GameState.LOGGED_IN) {
            load();
            if(lockImage == null) {
                lockImage = itemManager.getImage(25454);
            }
        } else {
            save();
        }
    }

    @Provides
    BankLocksConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankLocksConfig.class);
    }

    private int getItemIdOrChildItemId(Widget widget) {
        if (widget == null) {
            return -1;
        }
        int itemId = widget.getItemId();

        if (itemId > 0) {
            return itemId;
        }
        Widget[] children = widget.getChildren();
        if (children != null && children.length > 1) {
            // Equipment tab has the item ID as the 2nd child.
            return children[1].getItemId();
        }
        return -1;
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.holdShiftForLockAndUnlock() && !client.isKeyPressed(KeyCode.KC_SHIFT)) {
            return;
        }
        // TODO: Consider unlock-all right click on deposit-all buttons.
        int itemId = getItemIdOrChildItemId(event.getMenuEntry().getWidget());
        if (itemId < 1
                || !isInLockableInterface(event.getMenuEntry().getWidget())
                || !event.getOption().contains("Examine")) {
            return;
        }
        String menuOption = lockedItemIds.contains(itemId) ? "Bank-unlock" : "Bank-lock";
        client.createMenuEntry(-1)
                .setOption(menuOption)
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    SetUtil.toggleItem(lockedItemIds, itemId);
                    dirtyConfig = true;
                });

    }
}
