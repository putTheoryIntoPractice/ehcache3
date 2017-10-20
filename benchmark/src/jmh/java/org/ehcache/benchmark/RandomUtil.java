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

import java.util.Random;
import java.util.stream.Collectors;

public final class RandomUtil {

  private static final String ALPHA_SPACE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz";

  private static final Random random = new Random();

  private RandomUtil() {}

  public static String randomString(int min, int max) {
    int length = min + random.nextInt(max - min);
    return random
      .ints(length,0, ALPHA_SPACE.length())
      .mapToObj(ALPHA_SPACE::charAt)
      .map(Object::toString)
      .collect(Collectors.joining());
  }

  public static int nextInt(int bound) {
    return random.nextInt(bound);
  }

  public static boolean nextBoolean() {
    return random.nextBoolean();
  }

  public static byte[] randomBytes(int min, int max) {
    int length = min + random.nextInt(max - min);
    byte[] result = new byte[length];
    random.nextBytes(result);
    return result;
  }

  public static Long nextLong(long upperBound) {
    return random.nextLong();
  }
}
