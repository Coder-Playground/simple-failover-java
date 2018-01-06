package com.github.phantomthief.failover.impl;

import static com.google.common.base.Predicates.alwaysFalse;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import com.github.phantomthief.failover.Failover;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * @author w.vela
 */
class WeightFailoverTest {

    @Test
    void testCommon() {
        List<String> original = Arrays.asList("1", "2", "3");
        Failover<String> failover = WeightFailover.newBuilder() //
                .checker(this::check) //
                .build(original);
        Multiset<String> result = HashMultiset.create();
        Multiset<Integer> getCount = HashMultiset.create();
        for (int i = 0; i < 500; i++) {
            List<String> available = failover.getAvailable(2);
            assertTrue(available.size() <= 2);
            getCount.add(available.size());
            available.forEach(obj -> {
                assertNotNull(obj);
                if (doSomething(obj, failover)) {
                    result.add(obj);
                }
            });
            sleepUninterruptibly(10, MILLISECONDS);
        }
        System.out.println(getCount);
        System.out.println(result);
    }

    @Test
    void testMinWeight() {
        List<String> original = Arrays.asList("1", "2", "3");
        Failover<String> failover = WeightFailover.newBuilder() //
                .checker(this::check) //
                .minWeight(1) //
                .onMinWeight(i -> System.out.println("onMin:" + i)) //
                .build(original);
        Multiset<String> result = HashMultiset.create();
        Multiset<Integer> getCount = HashMultiset.create();
        for (int i = 0; i < 500; i++) {
            List<String> available = failover.getAvailable(2);
            assertTrue(available.size() == 2);
            getCount.add(available.size());
            available.forEach(obj -> {
                assertNotNull(obj);
                if (doSomething(obj, failover)) {
                    result.add(obj);
                }
            });
            sleepUninterruptibly(10, MILLISECONDS);
        }
        System.out.println(getCount);
        System.out.println(result);
    }

    @Test
    void testDown() {
        List<String> original = Arrays.asList("1", "2", "3");
        Failover<String> failover = WeightFailover.<String> newGenericBuilder() //
                .checker(alwaysFalse()) //
                .onMinWeight(i -> System.out.println("onMin:" + i)) //
                .build(original);
        Multiset<String> result = HashMultiset.create();
        Multiset<Integer> getCount = HashMultiset.create();
        failover.down("1");
        for (int i = 0; i < 500; i++) {
            String available = failover.getOneAvailable();
            assertNotEquals(available, "1");
            sleepUninterruptibly(10, MILLISECONDS);
        }
        System.out.println(getCount);
        System.out.println(result);
    }

    @Test
    void testLarge() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 108; i++) {
            map.put("i" + i, 32518);
        }
        for (int i = 0; i < 331; i++) {
            map.put("j" + i, 2652);
        }
        WeightFailover<String> failover = WeightFailover.<String> newGenericBuilder() //
                .checker(alwaysTrue()) //
                .build(map);
        Multiset<String> counter = HashMultiset.create();
        for (int i = 0; i < 100000; i++) {
            String oneAvailable = failover.getOneAvailable();
            counter.add(oneAvailable.substring(0, 1));
        }
        System.out.println(counter);
    }

    private boolean check(String test) {
        System.out.println("test:" + test);
        return true;
    }

    private boolean doSomething(String obj, Failover<String> failover) {
        boolean result = ThreadLocalRandom.current().nextInt(10) > Integer.parseInt(obj);
        if (result) {
            failover.success(obj);
        } else {
            failover.fail(obj);
        }
        return result;
    }
}
