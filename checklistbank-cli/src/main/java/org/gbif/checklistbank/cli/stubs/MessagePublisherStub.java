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

