package com.vartan.BankLocks;

import com.google.inject.Provides;
import com.vartan.BankLocks.model.MoreComponentIDs;
import com.vartan.BankLocks.model.SoundEffects;
import com.vartan.BankLocks.util.InterfaceUtil;
import com.vartan.BankLocks.util.SetUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Bank Locks"
)
public class BankLocksPlugin extends Plugin {
    /**
     * Lock image used by the overlay. Must be generated from the main thread.
     */
    BufferedImage lockImage;
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

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
    }

    private void saveLockedItems() {
        String commaSeparatedItemIds = lockedItemIds
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        configManager.setConfiguration(BankLocksConfig.CONFIG_GROUP,
                BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME,
                commaSeparatedItemIds);
    }

    private void loadLockedItems() {
        String commaSeparatedItemIds = configManager.getConfiguration(BankLocksConfig.CONFIG_GROUP,
                BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME);
        if (commaSeparatedItemIds != null) {
            lockedItemIds = Arrays.stream(commaSeparatedItemIds.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
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
        int itemId = InterfaceUtil.getItemIdOrChildItemId(event.getWidget());
        if (itemId < 0) {
            return;
        }

        if (option.startsWith("Deposit") || option.equalsIgnoreCase("Bank")) {
            if (shouldPreventDeposit(widget, itemId)) {
                preventMenuOptionClicked(event, itemId);
            }
        }
    }

    /**
     * Prevents depositing all inventory items or equipment.
     */
    private boolean preventDepositAll(MenuOptionClicked event, int widgetId) {
        if (config.preventDepositAll()) {
            InventoryID inventoryID = null;
            if (MoreComponentIDs.DEPOSIT_INVENTORY_COMPONENT_IDS.contains(widgetId)) {
                inventoryID = InventoryID.INVENTORY;
            } else if (MoreComponentIDs.DEPOSIT_EQUIPMENT_COMPONENT_IDS.contains(widgetId)) {
                inventoryID = InventoryID.EQUIPMENT;
            } else {
                return false;
            }
            int itemId = shouldPreventDepositAll(inventoryID);
            if (itemId > 0) {
                preventMenuOptionClicked(event, itemId);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first locked item ID in an inventory (or equipment).
     */
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

    /**
     * Prevents the menu option from being clicked and notifies the user which item blocked it.
     */
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

    /**
     * Checks whether a widget is a child of a locked widget.
     */
    private boolean isInLockedInterface(Widget widget) {
        return InterfaceUtil.anyInterfaceContainsWidget(MoreComponentIDs.LOCKED_INTERFACES, widget, client);
    }

    /**
     * Checks whether a widget is in a lockable interface.
     */
    private boolean isInLockableInterface(Widget widget) {
        return InterfaceUtil.anyInterfaceContainsWidget(MoreComponentIDs.LOCKABLE_INTERFACES, widget, client);
    }

    /**
     * Returns the item ID if the widget clicked is a locked item in a locked interface.
     */
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
            loadLockedItems();
            if (lockImage == null) {
                lockImage = itemManager.getImage(ItemID.GOLD_LOCKS);
            }
        }
    }

    @Provides
    BankLocksConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BankLocksConfig.class);
    }

    /**
     * Adds "Bank-unlock" and "Bank-lock" options to items.
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.holdShiftForLockAndUnlock() && !client.isKeyPressed(KeyCode.KC_SHIFT)) {
            return;
        }
        // TODO: Consider unlock-all right click on deposit-all buttons.
        int itemId = InterfaceUtil.getItemIdOrChildItemId(event.getMenuEntry().getWidget());
        if (itemId < 0
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
                    saveLockedItems();
                });
    }
}
