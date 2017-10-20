/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehcache.benchmark;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

@BenchmarkMode(Mode.Throughput)
@Threads(Threads.MAX)
@Fork(2)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class OnHeapBenchmark {

  private static final int ENTRY_COUNT = 100_000;

  @Param({"100", "50", "33"})
  public int hitRate = 0;

  private Long[] ids;

  private CacheManager cacheManager;
  private Cache<Long, Person> cache;

  @State(Scope.Thread)
  public static class ThreadState {
    private final static AtomicInteger offset = new AtomicInteger(0);

    int index = offset.getAndAdd(ENTRY_COUNT / 13);
  }

  @Setup
  public void setUp() {
    cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
      .withCache("cache", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, Person.class,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100_000))
        .build())
      .build(true);
    cache = cacheManager.getCache("cache", Long.class, Person.class);

    LongStream.range(0, ENTRY_COUNT).forEach(i -> cache.put(i, Person.randomPerson(i)));

    AccessPattern accessPattern = new RandomAccessPattern((int) (ENTRY_COUNT * (100d / hitRate)));

    ids = new Long[ENTRY_COUNT];
    for (int i = 0; i < ENTRY_COUNT; i++) {
      ids[i] = accessPattern.next();
    }
  }

  @TearDown
  public void tearDown() {
    cacheManager.close();
  }

  @Benchmark
  public Person read(ThreadState threadState) {
    int index = threadState.index++ % ENTRY_COUNT;
    Long id = ids[index];
    return cache.get(id);
  }
}
