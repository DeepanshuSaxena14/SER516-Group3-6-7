package edu.asu.ser516.taiga;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AucComputerTest {

    @Test
    void frontLoadedBurnProducesLowerAucWorkThanIdeal() {
        double[] remaining = {100.0, 60.0, 30.0, 10.0};
        var r = AucComputer.computeFromRemaining(remaining);
        assertTrue(r.aucWork() < r.aucValue());
        assertTrue(r.aucRatio() < 1.0);
    }

    @Test
    void endLoadedBurnProducesHigherAucWorkThanIdeal() {
        double[] remaining = {100.0, 95.0, 80.0, 10.0};
        var r = AucComputer.computeFromRemaining(remaining);
        assertTrue(r.aucWork() > r.aucValue());
        assertTrue(r.aucRatio() > 1.0);
    }

    @Test
    void flatBurnEqualsIdealWhenLinear() {
        double[] remaining = {100.0, 66.6667, 33.3333, 0.0};
        var r = AucComputer.computeFromRemaining(remaining);
        assertEquals(r.aucWork(), r.aucValue(), 0.01);
        assertEquals(1.0, r.aucRatio(), 0.01);
    }

    @Test
    void singleDaySprintHandledGracefully() {
        double[] remaining = {50.0};
        var r = AucComputer.computeFromRemaining(remaining);
        assertEquals(0.0, r.aucWork(), 0.001);
        assertEquals(0.0, r.aucValue(), 0.001);
        assertEquals(1.0, r.aucRatio(), 0.001);
    }
}

