package com.solra.grw.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FaithDashboard 单元测试。
 */
class FaithDashboardTest {

    @Test
    void shouldCreateEmptyDashboard() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        assertEquals("user1", dashboard.getUserId());
        assertEquals(0.0, dashboard.getOverallFaithScore(), 0.01);
        assertTrue(dashboard.getDimensions().isEmpty());
        assertTrue(dashboard.getMilestones().isEmpty());
    }

    @Test
    void shouldSetAndRetrieveDimensions() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        FaithDashboard.DimensionMetric metric = new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED, 100, 75.0, 5.0);
        dashboard.setDimension(metric);
        assertEquals(1, dashboard.getDimensions().size());
        assertEquals(75.0, dashboard.getDimensions()
                .get(FaithDashboard.DepthDimension.TIME_INVESTED).getPercentile(), 0.01);
    }

    @Test
    void shouldAddMilestones() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        FaithDashboard.Milestone ms = new FaithDashboard.Milestone(
                "MS-001", "首次探索", "第一次", Instant.now());
        dashboard.addMilestone(ms);
        assertEquals(1, dashboard.getMilestoneCount());
    }

    @Test
    void shouldCalculateOverallFaithScore() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED, 100, 80.0, 0));
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.SPACES_EXPLORED, 50, 60.0, 0));
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.FRIENDS_MADE, 10, 40.0, 0));

        double score = dashboard.calculateOverallFaithScore();
        // avg = (80+60+40)/3 = 60, *10 = 600
        assertEquals(600.0, score, 1.0);
    }

    @Test
    void shouldFindStrongestDimension() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED, 100, 30.0, 0));
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.SPACES_EXPLORED, 50, 90.0, 0));
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.FRIENDS_MADE, 10, 50.0, 0));

        assertTrue(dashboard.getStrongestDimension().isPresent());
        assertEquals(FaithDashboard.DepthDimension.SPACES_EXPLORED,
                dashboard.getStrongestDimension().get().getDimension());
    }

    @Test
    void shouldFindWeakestDimension() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED, 100, 30.0, 0));
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.SPACES_EXPLORED, 50, 90.0, 0));

        assertTrue(dashboard.getWeakestDimension().isPresent());
        assertEquals(FaithDashboard.DepthDimension.TIME_INVESTED,
                dashboard.getWeakestDimension().get().getDimension());
    }

    @Test
    void shouldGenerateNarrative() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        dashboard.setDimension(new FaithDashboard.DimensionMetric(
                FaithDashboard.DepthDimension.TIME_INVESTED, 100, 80.0, 0));
        dashboard.addMilestone(new FaithDashboard.Milestone("MS-001", "Test", "desc", Instant.now()));
        String narrative = dashboard.generateNarrative();
        assertNotNull(narrative);
        assertFalse(narrative.isEmpty());
    }

    @Test
    void shouldCountHighlightMilestones() {
        FaithDashboard dashboard = new FaithDashboard("user1");
        FaithDashboard.Milestone ms1 = new FaithDashboard.Milestone("MS-001", "A", "a", Instant.now());
        ms1.setHighlight(true);
        FaithDashboard.Milestone ms2 = new FaithDashboard.Milestone("MS-002", "B", "b", Instant.now());
        dashboard.addMilestone(ms1);
        dashboard.addMilestone(ms2);
        assertEquals(1, dashboard.getHighlightMilestoneCount());
    }

    @Test
    void shouldHave10DepthDimensions() {
        assertEquals(10, FaithDashboard.DepthDimension.values().length);
    }

    @Test
    void shouldHaveDimensionLabels() {
        assertEquals("时间投入", FaithDashboard.DepthDimension.TIME_INVESTED.getLabel());
        assertEquals("空间足迹", FaithDashboard.DepthDimension.SPACES_EXPLORED.getLabel());
        assertEquals("对话深度", FaithDashboard.DepthDimension.CONVERSATIONS_HAD.getLabel());
    }
}
