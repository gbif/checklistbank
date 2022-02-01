/*
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
package org.gbif.checklistbank.cli.stubs;

import org.gbif.common.messaging.api.Message;
import org.gbif.common.messaging.api.MessagePublisher;

import java.io.IOException;

public class MessagePublisherStub implements MessagePublisher {

  @Override
  public void send(Message message) throws IOException {}

  @Override
  public void send(Message message, boolean persistent) throws IOException {}

  @Override
  public void send(Message message, String exchange) throws IOException {}

  @Override
  public void send(Object message, String exchange, String routingKey) throws IOException {}

  @Override
  public void send(Object message, String exchange, String routingKey, boolean persistent)
    throws IOException {}

  @Override
  public void replyToQueue(Object o, boolean b, String s, String s1) throws IOException {

  }

  @Override
  public <T> T sendAndReceive(Message message, String s, boolean b, String s1)
    throws IOException, InterruptedException {
    return null;
  }

  @Override
  public <T> T sendAndReceive(Object o, String s, String s1, boolean b, String s2)
    throws IOException, InterruptedException {
    return null;
  }

  @Override
  public void close() {}
}
