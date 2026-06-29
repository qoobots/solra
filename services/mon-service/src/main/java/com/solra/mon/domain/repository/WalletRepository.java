package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.VirtualWallet;
import java.util.Optional;

/**
 * 钱包仓储接口。
 */
public interface WalletRepository {

    Optional<VirtualWallet> findByUserId(String userId);

    VirtualWallet save(VirtualWallet wallet);
}
