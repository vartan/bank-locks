package com.vartan.BankLocks.model;

import net.runelite.api.widgets.ComponentID;

import java.util.Set;

public class MoreComponentIDs {
    public static int DEPOSIT_BOX_DEPOSIT_INVENTORY = 12582916;
    public static int DEPOSIT_BOX_DEPOSIT_EQUIPMENT = 12582918;

    /** Interfaces from which you can deposit items. */
    public static final Set<Integer> LOCKED_INTERFACES = Set.of(
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
    public static final Set<Integer> DEPOSIT_INVENTORY_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_INVENTORY,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_INVENTORY
    );

    /** Identifiers for deposit all equipment buttons. */
    public static final Set<Integer> DEPOSIT_EQUIPMENT_COMPONENT_IDS = Set.of(
            ComponentID.BANK_DEPOSIT_EQUIPMENT,
            MoreComponentIDs.DEPOSIT_BOX_DEPOSIT_EQUIPMENT
    );
}
