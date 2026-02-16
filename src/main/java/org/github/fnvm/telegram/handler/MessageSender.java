package org.github.fnvm.telegram.handler;

import org.github.fnvm.telegram.TikTokDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MessageSender {
  private static final Logger log = LoggerFactory.getLogger(MessageSender.class);
  private final TikTokDownloader bot;

  public MessageSender(TikTokDownloader bot) {
    this.bot = bot;
  }

  public void sendMessage(Long chatId, String text, Integer messageThreadId) {
    try {
      SendMessage message = new SendMessage();
      message.setChatId(chatId.toString());
      message.setText(text);
      if (messageThreadId != null) {
        message.setMessageThreadId(messageThreadId);
      }
      bot.execute(message);
    } catch (TelegramApiException e) {
      log.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
    }
  }

  public void sendMessage(Long chatId, String text) {
    sendMessage(chatId, text, null);
  }

  public void sendError(Long chatId, String errorText, Integer messageThreadId) {
    sendMessage(chatId, errorText, messageThreadId);
  }

  public void sendError(Long chatId, String errorText) {
    sendError(chatId, errorText, null);
  }
}
