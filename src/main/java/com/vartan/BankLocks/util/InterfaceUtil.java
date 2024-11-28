package com.vartan.BankLocks.util;

import com.vartan.BankLocks.model.MoreComponentIDs;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class InterfaceUtil {
    public static boolean anyInterfaceContainsWidget(Collection<Integer> interfaceIds, Widget widget, Client client) {
        Set<Widget> interfaceWidgets = interfaceIds.stream().map(client::getWidget).collect(Collectors.toSet());
        while(widget != null) {
            if (interfaceWidgets.contains(widget)) {
                return true;
            }
            widget = widget.getParent();
        }
        return false;
    }

    /**
     * Gets the item ID from an interface widget, or from its second child.
     *
     * In most cases, the item ID is directly the widget returned by MenuOptionClicked. However, in the case of
     * the equipment tab in the bank, the item ID is actually part of the widget's child at index 1.
     */
    public static int getItemIdOrChildItemId(Widget widget) {
        if (widget == null) {
            return ItemUtil.INVALID_ITEM_ID;
        }
        int itemId = widget.getItemId();

        if (ItemUtil.isValidItemId(itemId)) {
            return itemId;
        }
        Widget[] children = widget.getChildren();
        if (children != null && children.length > 1) {
            // Equipment tab has the item ID as the 2nd child.
            return children[1].getItemId();
        }
        return ItemUtil.INVALID_ITEM_ID;
    }

    /**
     * Checks whether a widget is a child of a locked widget.
     */
    public static boolean isInLockedInterface(Widget widget, Client client) {
        return InterfaceUtil.anyInterfaceContainsWidget(MoreComponentIDs.LOCKED_INTERFACES, widget, client);
    }

    /**
     * Checks whether a widget is in a lockable interface.
     */
    public static boolean isInLockableInterface(Widget widget, Client client) {
        return InterfaceUtil.anyInterfaceContainsWidget(MoreComponentIDs.LOCKABLE_INTERFACES, widget, client);
    }
}
