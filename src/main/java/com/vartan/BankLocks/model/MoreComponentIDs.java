package com.vartan.BankLocks.model;

import net.runelite.api.widgets.ComponentID;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoreComponentIDs {
    /**
     * Interfaces from which you can deposit items.
     */
    public static final Set<Integer> LOCKED_INTERFACES = Set.of(
            ComponentID.BANK_INVENTORY_ITEM_CONTAINER,
            ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER,
            ComponentID.BANK_EQUIPMENT_PARENT,
            ComponentID.BANK_INVENTORY_EQUIPMENT_ITEM_CONTAINER
    );
    
    /**
     * Interfaces that don't deposit (no need to lock), but can be used to lock and unlock items.
     */
    private static final Set<Integer> ADDITIONAL_LOCKABLE_INTERFACES = Set.of(
            ComponentID.INVENTORY_CONTAINER,
            ComponentID.EQUIPMENT_INVENTORY_ITEM_CONTAINER
    );

    /**
     * Interfaces that can be used to lock/unlock items from being banked.
     */
    public static final Set<Integer> LOCKABLE_INTERFACES = Stream.concat(
            LOCKED_INTERFACES.stream(), // Locked interfaces are also lockable.
            ADDITIONAL_LOCKABLE_INTERFACES.stream()
    ).collect(Collectors.toSet());

    /** Deposit-all inventory button for the deposit box. */
    public static int DEPOSIT_BOX_DEPOSIT_INVENTORY = 12582916;

    /** Deposit-all equipment button for the deposit box. */
    public static int DEPOSIT_BOX_DEPOSIT_EQUIPMENT = 12582918;

    /**
     * Identifiers for deposit all inventory buttons.
     */
    public static final Set<Integer> DEPOSIT_INVENTORY_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_INVENTORY,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_INVENTORY
    );

    /**
     * Identifiers for deposit all equipment buttons.
     */
    public static final Set<Integer> DEPOSIT_EQUIPMENT_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_EQUIPMENT,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_EQUIPMENT
    );
}
