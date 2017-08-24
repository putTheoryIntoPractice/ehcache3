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

package org.ehcache.clustered.common.internal.messages;

import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.util.UUID;

/**
 * MessageCodecUtils
 */
public class MessageCodecUtils {

  public static final String SERVER_STORE_NAME_FIELD = "serverStoreName";
  public static final String KEY_FIELD = "key";

  public void encodeMandatoryFields(StructEncoder<Void> encoder, EhcacheOperationMessage message) {
    encoder.enm(EhcacheMessageType.MESSAGE_TYPE_FIELD_NAME, message.getMessageType());
  }
}
