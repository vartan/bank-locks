package com.vartan.BankLocks.util;

import java.util.Set;

public class SetUtil {
    public static <T> boolean toggleItem(Set<T> set, T item) {
        if(set.contains(item)) {
            set.remove(item);
            return false;
        }
        set.add(item);
        return true;
    }
}
