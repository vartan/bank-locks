package com.vartan.BankLocks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Bank Locks")
public interface BankLocksConfig extends Config {
    String CONFIG_GROUP = "bankLocks";
    String LOCKED_ITEMS_CONFIG_NAME = "lockedItems";

    @ConfigItem(
            keyName = "preventDepositAll",
            name = "Prevent deposit-all locked item",
            description = "Whether the 'Deposit inventory' and 'Deposit worn items' buttons will be " +
                    "disabled if pressing it would deposit a locked item.",
            position = 0
    )
    default boolean preventDepositAll() {
        return true;
    }

    @ConfigItem(
            keyName = "playSoundWhenPrevented",
            name = "Enable sound effect",
            description = "Whether a warning sound will play when the plugin prevents a deposit.",
            position = 1
    )
    default boolean playSoundWhenPrevented() {
        return true;
    }

    @ConfigItem(
            keyName = "holdShiftForLockAndUnlock",
            name = "Hold shift to lock/unlock",
            description = "Whether shift is required to display 'Bank-lock' and 'Bank-unlock' right click menu " +
                    "entries. When this is unchecked, they will always be shown.",
            position = 2

    )
    default boolean holdShiftForLockAndUnlock() {
        return true;
    }

    @ConfigItem(
            keyName = "lockOpacity",
            name = "Lock overlay opacity",
            description = "How visible should the lock overlay be? 1 for fully visible, 0 to turn off the lock " +
                    "overlay.",
            position = 3

    )
    default double lockOpacity() {
        return 0.8f;
    }
}
