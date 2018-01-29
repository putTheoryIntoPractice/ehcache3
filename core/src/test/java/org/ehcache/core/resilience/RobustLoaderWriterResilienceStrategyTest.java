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
package org.ehcache.core.resilience;

import org.assertj.core.data.MapEntry;
import org.ehcache.core.internal.util.CollectionUtil;
import org.ehcache.core.spi.store.Store;
import org.ehcache.resilience.RethrowingStoreAccessException;
import org.ehcache.resilience.StoreAccessException;
import org.ehcache.spi.loaderwriter.BulkCacheLoadingException;
import org.ehcache.spi.loaderwriter.BulkCacheWritingException;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.ehcache.spi.loaderwriter.CacheLoadingException;
import org.ehcache.spi.loaderwriter.CacheWritingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RobustLoaderWriterResilienceStrategyTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private Store<Integer, Long> store;

  @Mock
  private CacheLoaderWriter<Integer, Long> loaderWriter;

  @InjectMocks
  private RobustLoaderWriterResilienceStrategy<Integer, Long> strategy;

  private final StoreAccessException accessException = new StoreAccessException("The exception");

  private final Exception exception = new Exception("failed");

  private final BulkCacheLoadingException bulkLoadingException = new BulkCacheLoadingException(
    CollectionUtil.map(1, exception), CollectionUtil.map(2, 2L));

  private final BulkCacheWritingException bulkWritingException = new BulkCacheWritingException(
    CollectionUtil.map(1, exception), Collections.singleton(2));

  @After
  public void noMoreInteractions() {
    verifyNoMoreInteractions(store, loaderWriter);
  }

  @Test
  public void getFailure() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);

    assertThat(strategy.getFailure(1, accessException)).isEqualTo(1L);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void getFailure_failedLoaderWriter() throws Exception {
    when(loaderWriter.load(1)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.getFailure(1, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void containsKeyFailure() throws Exception {
    assertThat(strategy.containsKeyFailure(1, accessException)).isFalse();

    verify(store).remove(1);
    verifyZeroInteractions(loaderWriter);
  }

  @Test
  public void putFailure() throws Exception {
    strategy.putFailure(1, 1L, accessException);

    verify(store).remove(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void putFailure_failedLoaderWriter() throws Exception {
    doThrow(exception).when(loaderWriter).write(1, 1L);

    assertThatThrownBy(() -> strategy.putFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void removeFailure() throws Exception {
    strategy.removeFailure(1, accessException);

    verify(store).remove(1);
    verify(loaderWriter).delete(1);
  }

  @Test
  public void removeFailure_failedLoaderWriter() throws Exception {
    doThrow(exception).when(loaderWriter).delete(1);

    assertThatThrownBy(() -> strategy.removeFailure(1, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).delete(1);
  }

  @Test
  public void clearFailure() throws Exception {
    strategy.clearFailure(accessException);

    verify(store).clear();
  }

  @Test
  public void putIfAbsentFailure_found() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);

    assertThat(strategy.putIfAbsentFailure(1, 2L, accessException)).isEqualTo(1);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void putIfAbsentFailure_notFound() throws Exception {
    when(loaderWriter.load(1)).thenReturn(null);

    assertThat(strategy.putIfAbsentFailure(1, 1L, accessException)).isNull();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void putIfAbsentFailure_loadFails() throws Exception {
    when(loaderWriter.load(1)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.putIfAbsentFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void putIfAbsentFailure_writeFails() throws Exception {
    when(loaderWriter.load(1)).thenReturn(null);
    doThrow(exception).when(loaderWriter).write(1, 1L);

    assertThatThrownBy(() -> strategy.putIfAbsentFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void removeFailure1_notFound() throws Exception {
    assertThat(strategy.removeFailure(1, 1L, accessException)).isFalse();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void removeFailure1_foundNotEquals() throws Exception {
    when(loaderWriter.load(1)).thenReturn(2L);

    assertThat(strategy.removeFailure(1, 1L, accessException)).isFalse();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void removeFailure1_foundEquals() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);

    assertThat(strategy.removeFailure(1, 1L, accessException)).isTrue();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).delete(1);
  }

  @Test
  public void removeFailure1_loadFails() throws Exception {
    when(loaderWriter.load(1)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.removeFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void removeFailure1_deleteFails() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);
    doThrow(exception).when(loaderWriter).delete(1);

    assertThatThrownBy(() -> strategy.removeFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).delete(1);
  }

  @Test
  public void replaceFailure_notFound() throws Exception {
    assertThat(strategy.replaceFailure(1, 1L, accessException)).isNull();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void replaceFailure_found() throws Exception {
    when(loaderWriter.load(1)).thenReturn(2L);

    assertThat(strategy.replaceFailure(1, 1L, accessException)).isEqualTo(2L);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void replaceFailure_loadFails() throws Exception {
    when(loaderWriter.load(1)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.replaceFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void replaceFailure_writeFails() throws Exception {
    when(loaderWriter.load(1)).thenReturn(2L);
    doThrow(exception).when(loaderWriter).write(1, 1L);

    assertThatThrownBy(() -> strategy.replaceFailure(1, 1L, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 1L);
  }

  @Test
  public void replaceFailure1_notFound() throws Exception {
    assertThat(strategy.replaceFailure(1, 1L, 2L, accessException)).isFalse();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void replaceFailure1_foundNotEquals() throws Exception {
    when(loaderWriter.load(1)).thenReturn(3L);

    assertThat(strategy.replaceFailure(1, 1L, 2L, accessException)).isFalse();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void replaceFailure1_foundEquals() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);

    assertThat(strategy.replaceFailure(1, 1L, 2L, accessException)).isTrue();

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 2L);
  }

  @Test
  public void replaceFailure1_loadFails() throws Exception {
    when(loaderWriter.load(1)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.replaceFailure(1, 1L, 2L, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
  }

  @Test
  public void replaceFailure1_writeFails() throws Exception {
    when(loaderWriter.load(1)).thenReturn(1L);
    doThrow(exception).when(loaderWriter).write(1, 2L);

    assertThatThrownBy(() -> strategy.replaceFailure(1, 1L, 2L, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(loaderWriter).load(1);
    verify(loaderWriter).write(1, 2L);
  }

  @Test
  public void getAllFailure_nothingFound() throws Exception {
    List<Integer> keys = Arrays.asList(1, 2);
    Map<Integer, Long> entries = CollectionUtil.map(1, null, 2, null);

    when(loaderWriter.loadAll(keys)).thenReturn(entries);

    assertThat(strategy.getAllFailure(keys, accessException)).isEqualTo(entries);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).loadAll(keys);
  }

  @Test
  public void getAllFailure_allFound() throws Exception {
    List<Integer> keys = Arrays.asList(1, 2);
    Map<Integer, Long> entries = CollectionUtil.map(1, 1L, 2, 2L);

    when(loaderWriter.loadAll(keys)).thenReturn(entries);

    assertThat(strategy.getAllFailure(keys, accessException)).isEqualTo(entries);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).loadAll(keys);
  }

  @Test
  public void getAllFailure_partialFound() throws Exception {
    List<Integer> keys = Arrays.asList(1, 2);
    Map<Integer, Long> entries = CollectionUtil.map(1, 1L, 2, null);

    when(loaderWriter.loadAll(keys)).thenReturn(entries);

    assertThat(strategy.getAllFailure(keys, accessException)).isEqualTo(entries);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).loadAll(keys);
  }

  @Test
  public void getAllFailure_loadFailsWithException() throws Exception {
    List<Integer> keys = Arrays.asList(1, 2);

    when(loaderWriter.loadAll(keys)).thenThrow(exception);

    assertThatThrownBy(() -> strategy.getAllFailure(keys, accessException))
      .isExactlyInstanceOf(CacheLoadingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).loadAll(keys);
  }

  @Test
  public void getAllFailure_loadFailsWithBulkException() throws Exception {
    List<Integer> keys = Arrays.asList(1, 2);

    when(loaderWriter.loadAll(keys)).thenThrow(bulkLoadingException);

    assertThatThrownBy(() -> strategy.getAllFailure(keys, accessException))
      .isSameAs(bulkLoadingException);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).loadAll(keys);
  }

  @Test
  public void putAllFailure() throws Exception {
    List<MapEntry<Integer, Long>> entryList = Arrays.asList(entry(1, 1L), entry(2, 2L));
    Map<Integer, Long> entryMap = CollectionUtil.map(1, 1L, 2, 2L);

    doNothing().when(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));

    strategy.putAllFailure(entryMap, accessException);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));
  }

  @Test
  public void putAllFailure_writeAllFailsWithException() throws Exception {
    List<MapEntry<Integer, Long>> entryList = Arrays.asList(entry(1, 1L), entry(2, 2L));
    Map<Integer, Long> entryMap = CollectionUtil.map(1, 1L, 2, 2L);

    doThrow(exception).when(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));

    assertThatThrownBy(() -> strategy.putAllFailure(entryMap, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));
  }

  @Test
  public void putAllFailure_writeAllFailsWithBulkException() throws Exception {
    List<MapEntry<Integer, Long>> entryList = Arrays.asList(entry(1, 1L), entry(2, 2L));
    Map<Integer, Long> entryMap = CollectionUtil.map(1, 1L, 2, 2L);

    doThrow(bulkWritingException).when(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));

    assertThatThrownBy(() -> strategy.putAllFailure(entryMap, accessException))
      .isSameAs(bulkWritingException);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).writeAll(argThat(containsAllMatcher(entryList)));
  }

  @Test
  public void removeAllFailure() throws Exception {
    List<Integer> entryList = Arrays.asList(1, 2);

    strategy.removeAllFailure(entryList, accessException);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).deleteAll(entryList);
  }

  @Test
  public void removeAllFailure_deleteAllFailsWithException() throws Exception {
    List<Integer> entryList = Arrays.asList(1, 2);

    doThrow(exception).when(loaderWriter).deleteAll(entryList);

    assertThatThrownBy(() -> strategy.removeAllFailure(entryList, accessException))
      .isExactlyInstanceOf(CacheWritingException.class)
      .hasCause(exception);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).deleteAll(entryList);
  }

  @Test
  public void removeAllFailure_deleteAllFailsWithBulkException() throws Exception {
    List<Integer> entryList = Arrays.asList(1, 2);

    doThrow(bulkWritingException).when(loaderWriter).deleteAll(entryList);

    assertThatThrownBy(() -> strategy.removeAllFailure(entryList, accessException))
      .isSameAs(bulkWritingException);

    verify(store).remove(1);
    verify(store).remove(2);
    verify(loaderWriter).deleteAll(entryList);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void filterException_SAE() {
    // Nothing happens
    strategy.filterException(accessException);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void filterException_RSAE() {
    assertThatExceptionOfType(RuntimeException.class)
      .isThrownBy(() -> strategy.filterException(new RethrowingStoreAccessException(new RuntimeException())));
  }

  private ArgumentMatcher<Iterable<? extends Map.Entry<? extends Integer, ? extends Long>>> containsAllMatcher(List<MapEntry<Integer, Long>> entryList) {
    return argument -> {
      boolean[] notFound = { false };
      argument.forEach(e -> {
        if (!entryList.contains(e)) {
          notFound[0] = true;
        }
      });
      return !notFound[0];
    };
  }
}
