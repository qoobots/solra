package com.solra.grw.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Evangelist 单元测试。
 */
class EvangelistTest {

    @Test
    void shouldCreateWithPendingStatus() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        assertEquals(Evangelist.ApplicationStatus.PENDING, ev.getStatus());
        assertEquals(Evangelist.EvangelistTier.APPRENTICE, ev.getTier());
        assertEquals(0, ev.getFollowersCount());
        assertEquals(0, ev.getTotalVisits());
    }

    @Test
    void shouldApproveCorrectly() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.approve("admin1", "Approved");
        assertEquals(Evangelist.ApplicationStatus.APPROVED, ev.getStatus());
        assertEquals("admin1", ev.getReviewerId());
        assertNotNull(ev.getReviewedAt());
    }

    @Test
    void shouldRejectCorrectly() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.reject("admin1", "Not qualified");
        assertEquals(Evangelist.ApplicationStatus.REJECTED, ev.getStatus());
    }

    @Test
    void shouldSuspendCorrectly() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.approve("admin1", "ok");
        ev.suspend("Violation");
        assertEquals(Evangelist.ApplicationStatus.SUSPENDED, ev.getStatus());
    }

    @Test
    void shouldRevokeCorrectly() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.approve("admin1", "ok");
        ev.revoke("Inactive");
        assertEquals(Evangelist.ApplicationStatus.REVOKED, ev.getStatus());
    }

    @Test
    void shouldBeEligibleWithEnoughFollowersAndVisits() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.updateFollowers(200);
        ev.updateVisits(600);
        assertTrue(ev.isEligible());
    }

    @Test
    void shouldNotBeEligibleWithoutEnoughFollowers() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.updateFollowers(50);
        ev.updateVisits(600);
        assertFalse(ev.isEligible());
    }

    @Test
    void shouldRecalculateTierOnStatsUpdate() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.updateFollowers(100);
        ev.updateVisits(500);
        assertEquals(Evangelist.EvangelistTier.APPRENTICE, ev.getTier());

        ev.updateFollowers(600);
        ev.updateVisits(2500);
        assertEquals(Evangelist.EvangelistTier.JOURNEYMAN, ev.getTier());

        ev.updateFollowers(1200);
        ev.updateVisits(6000);
        assertEquals(Evangelist.EvangelistTier.MASTER, ev.getTier());
    }

    @Test
    void shouldUpdateContributionScore() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        ev.incrementSpacesCreated();
        assertTrue(ev.getContributionScore() > 0);
        ev.incrementSharesGenerated();
        assertTrue(ev.getContributionScore() > 0);
    }

    @Test
    void shouldHaveFourTiers() {
        assertEquals(4, Evangelist.EvangelistTier.values().length);
    }

    @Test
    void shouldBeActiveWhenApproved() {
        Evangelist ev = new Evangelist("EV-001", "user1", "TestEv", "bio");
        assertFalse(ev.isActive());
        ev.approve("admin1", "ok");
        assertTrue(ev.isActive());
    }

    @Test
    void shouldClassifyTierFromStats() {
        assertEquals(Evangelist.EvangelistTier.APPRENTICE,
                Evangelist.EvangelistTier.fromStats(100, 500));
        assertEquals(Evangelist.EvangelistTier.JOURNEYMAN,
                Evangelist.EvangelistTier.fromStats(500, 2000));
        assertEquals(Evangelist.EvangelistTier.MASTER,
                Evangelist.EvangelistTier.fromStats(1000, 5000));
        assertEquals(Evangelist.EvangelistTier.LEGEND,
                Evangelist.EvangelistTier.fromStats(5000, 10000));
    }
}
