package com.vartan.BankLocks;

import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class BankLocksLoader {
    @Inject
    private ConfigManager configManager;

    /** Saves the locked item ID set to config. */
    public void saveLockedItems(Set<Integer> lockedItemIds) {
        String commaSeparatedItemIds = lockedItemIds
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        configManager.setConfiguration(BankLocksConfig.CONFIG_GROUP,
                BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME,
                commaSeparatedItemIds);
    }

    /** Returns the set of item IDs saved to config, or an empty set if it doesn't exist. */
    public Set<Integer> loadLockedItems() {
        try {
            String commaSeparatedItemIds = configManager.getConfiguration(BankLocksConfig.CONFIG_GROUP,
                    BankLocksConfig.LOCKED_ITEMS_CONFIG_NAME);
            if (commaSeparatedItemIds != null) {
                return Arrays.stream(commaSeparatedItemIds.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toCollection(HashSet::new));
            }
        } catch (Exception e) {
            // If there is any exception encountered (reading config or converting to Set<Integer>),
            // behave as if there is no saved config.
        }
        return new HashSet<>();
    }
}
