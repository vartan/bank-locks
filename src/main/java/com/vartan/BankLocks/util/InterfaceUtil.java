package com.vartan.BankLocks.util;

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
}
