package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.VirtualWallet;
import com.solra.mon.domain.repository.WalletRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 钱包仓储内存实现。
 */
public class InMemoryWalletRepository implements WalletRepository {

    private final Map<String, VirtualWallet> store = new ConcurrentHashMap<>();

    @Override
    public Optional<VirtualWallet> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public VirtualWallet save(VirtualWallet wallet) {
        store.put(wallet.getUserId(), wallet);
        return wallet;
    }
}
