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

public class Person {

  private static final String ALPHA_SPACE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz";

  private final Long id;
  private final String name;
  private final int age;
  private final Gender gender;
  private final String bio;
  private final byte[] picture;

  public static Person randomPerson(long id) {
    String name = RandomUtil.randomString(3, 20);
    int age = RandomUtil.nextInt(120);
    Gender gender = RandomUtil.nextBoolean() ? Gender.FEMALE : Gender.MALE;
    String bio = RandomUtil.randomString(50, 300);
    byte[] picture = RandomUtil.randomBytes(100, 200);
    return new Person(id, name, age, gender, bio, picture);
  }

  public Person(Long id, String name, int age, Gender gender, String bio, byte[] picture) {
    this.id = id;
    this.name = name;
    this.age = age;
    this.gender = gender;
    this.bio = bio;
    this.picture = picture;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  public Gender getGender() {
    return gender;
  }

  public String getBio() {
    return bio;
  }

  public byte[] getPicture() {
    return picture;
  }

}
