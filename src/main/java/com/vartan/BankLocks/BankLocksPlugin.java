package com.vartan.BankLocks;

import com.google.inject.Provides;
import com.vartan.BankLocks.model.MoreComponentIDs;
import com.vartan.BankLocks.model.SoundEffects;
import com.vartan.BankLocks.util.InterfaceUtil;
import com.vartan.BankLocks.util.ItemUtil;
import com.vartan.BankLocks.util.SetUtil;
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
import java.util.HashSet;
import java.util.Set;

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
    @Inject
    private BankLocksLoader loader;

    /**
     * Set of item IDs that should not be banked.
     * This is persisted across sessions via (save|load)LockedItems methods.
     */
    private Set<Integer> lockedItemIds = new HashSet<>();

    @Provides
    BankLocksConfig provideConfig(ConfigManager configManager) {
        // Allows BankLocksConfig to be easily injected into other classes.
        return configManager.getConfig(BankLocksConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == GameState.LOGGED_IN) {
            lockedItemIds = loader.loadLockedItems();
            if (lockImage == null) {
                lockImage = itemManager.getImage(ItemID.GOLD_LOCKS);
            }
        }
    }

    /**
     * Adds "Bank-unlock" and "Bank-lock" options to items.
     */
    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.holdShiftForLockAndUnlock() && !client.isKeyPressed(KeyCode.KC_SHIFT)) {
            return;
        }
        if (!event.getOption().contains("Examine")) {
            // Exit early on "Examine" check, since it's a simple string operation.
            // Other work below may take longer depending on the interface.
            return;
        }
        // TODO: Consider unlock-all right click on deposit-all buttons.
        int itemId = InterfaceUtil.getItemIdOrChildItemId(event.getMenuEntry().getWidget());
        if (!ItemUtil.isValidItemId(itemId)
                || !InterfaceUtil.isInLockableInterface(event.getMenuEntry().getWidget(), client)) {
            return;
        }
        String menuOption = isItemLocked(itemId) ? "Bank-unlock" : "Bank-lock";
        client.createMenuEntry(-1)
                .setOption(menuOption)
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    SetUtil.toggleItem(lockedItemIds, itemId);
                    loader.saveLockedItems(lockedItemIds);
                });
    }

    @Subscribe()
    public void onMenuOptionClicked(MenuOptionClicked event) {
        Widget widget = event.getMenuEntry().getWidget();
        if (widget == null) {
            // Ignore any menu options that aren't from a widget.
            return;
        }
        String option = Text.removeTags(event.getMenuOption());
        int widgetId = widget.getId();

        if (preventDepositAll(event, widgetId)) return;

        preventDepositItem(event, option, widget);
    }

    private void preventDepositItem(MenuOptionClicked event, String option, Widget widget) {
        if (!option.startsWith("Deposit") /* depositing from most interfaces*/
                && !option.equalsIgnoreCase("Bank") /* depositing from the equipment tab in the bank. */) {
            return;
        }

        int itemId = InterfaceUtil.getItemIdOrChildItemId(event.getWidget());
        if (!ItemUtil.isValidItemId(itemId)) {
            return;
        }

        if (shouldPreventDeposit(widget, itemId)) {
            preventMenuOptionClicked(event, itemId);
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
            if (ItemUtil.isValidItemId(itemId)) {
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
            return ItemUtil.INVALID_ITEM_ID;
        }
        for (Item item : itemContainer.getItems()) {
            int itemId = item.getId();
            if (isItemLocked(itemId)) {
                return itemId;
            }
        }
        return ItemUtil.INVALID_ITEM_ID;
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
     * Returns the item ID if the widget clicked is a locked item in a locked interface.
     */
    private boolean shouldPreventDeposit(Widget widget, int itemId) {
        if (!InterfaceUtil.isInLockedInterface(widget, client)) {
            return false;
        }
        return lockedItemIds.contains(itemId);
    }

    public boolean isItemLocked(int itemId) {
        return lockedItemIds.contains(itemId);
    }
}
