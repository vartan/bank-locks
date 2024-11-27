package com.vartan.BankLocks;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

import javax.inject.Inject;
import java.awt.*;

public class BankLocksOverlay extends WidgetItemOverlay {
    private final BankLocksPlugin plugin;
    private final BankLocksConfig config;

    @Inject
    private BankLocksOverlay(BankLocksPlugin plugin, BankLocksConfig config) {
        super();
        this.plugin = plugin;
        this.config = config;
        showOnInventory();
        showOnEquipment();
        // TODO: Figure out how to get this working on bank equipment inventory.
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem) {
        graphics.setFont(FontManager.getRunescapeSmallFont());

        if (!plugin.getLockedItemIds().contains(itemId)) {
            return;
        }
        Rectangle location = widgetItem.getCanvasBounds();
        int width = (int) location.getWidth();
        int height = (int) location.getHeight();
        int x = (int) location.getX();
        int y = (int) location.getY();
        float opacity = (float) Math.max(0, Math.min(1, config.lockOpacity()));
        if (plugin.lockImage != null && opacity > 0f) {
            // Draw transparent lock over the item.
            AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            graphics.setComposite(composite);
            graphics.drawImage(plugin.lockImage, x + width / 2, y + height / 2, width / 2, height / 2, null);
        }
    }
}
