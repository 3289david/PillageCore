package com.mingyu.pillage.economy;

import com.mingyu.pillage.data.dao.EconomyDao;

import java.util.UUID;

/** Emerald-backed economy: 1 emerald == 1 unit of balance. */
public final class EconomyManager {

    private final EconomyDao economyDao;

    public EconomyManager(EconomyDao economyDao) {
        this.economyDao = economyDao;
    }

    public long balance(UUID uuid) {
        return economyDao.balance(uuid);
    }

    public void deposit(UUID uuid, long amount) {
        economyDao.add(uuid, amount);
    }

    public boolean withdraw(UUID uuid, long amount) {
        return economyDao.subtract(uuid, amount);
    }

    public boolean pay(UUID from, UUID to, long amount) {
        if (amount <= 0) return false;
        if (!economyDao.subtract(from, amount)) return false;
        economyDao.add(to, amount);
        return true;
    }
}
