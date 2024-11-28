package com.vartan.BankLocks.util;

public class ItemUtil {
    public static int INVALID_ITEM_ID = -1;
    public static boolean isValidItemId(int itemId) {
        return itemId >= 0;
    }
}
