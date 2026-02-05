package org.github.fnvm.telegram;

import org.github.fnvm.data.QualityPreference;
import org.github.fnvm.telegram.action.Action;
import org.github.fnvm.telegram.action.Actions;
import org.github.fnvm.telegram.handler.ContentSender;
import org.github.fnvm.telegram.handler.CookieHandler;
import org.github.fnvm.telegram.handler.DownloadHandler;
import org.github.fnvm.telegram.handler.HelpHandler;
import org.github.fnvm.telegram.handler.MessageSender;
import org.github.fnvm.telegram.profile.UserProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TikTokDownloader extends TelegramLongPollingBot {
  private static final Logger log = LoggerFactory.getLogger(TikTokDownloader.class);

  private static String botName;
  private final UserProfileManager profileManager;
  private final MessageSender messageSender;
  private final HelpHandler helpHandler;
  private final CookieHandler cookieHandler;
  private final ContentSender contentSender;
  private final DownloadHandler downloadHandler;

  public TikTokDownloader(String token) {
    super(token);
    this.profileManager = new UserProfileManager();
    this.messageSender = new MessageSender(this);
    this.helpHandler = new HelpHandler(messageSender);
    this.cookieHandler = new CookieHandler(profileManager, messageSender);
    this.contentSender = new ContentSender(this, messageSender);
    this.downloadHandler = new DownloadHandler(profileManager, contentSender, messageSender);
  }

  @Override
  public String getBotUsername() {
    return botName;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return;
    }

    Message message = update.getMessage();
    String text = message.getText().trim();
    Long chatId = message.getChatId();
    Long userId = message.getFrom().getId();

    Action action = Actions.parse(text);

    try {
      handleAction(action, chatId, userId);
    } catch (Exception e) {
      log.error("Error handling action for user {}: {}", userId, e.getMessage(), e);
    }
  }

  private void handleAction(Action action, Long chatId, Long userId) {
    switch (action.type()) {
      case HELP -> helpHandler.sendHelp(chatId);
      case SET_COOKIE -> cookieHandler.handleSetCookie(chatId, userId, action.payload());
      case VIEW_COOKIE -> cookieHandler.handleViewCookie(chatId, userId);
      case DELETE_COOKIE -> cookieHandler.handleDeleteCookie(userId, chatId);
      case GET_SD ->
          downloadHandler.handleDownload(chatId, userId, action.payload(), QualityPreference.SD);
      case GET_HD ->
          downloadHandler.handleDownload(chatId, userId, action.payload(), QualityPreference.HD);
      case GET_FULLHD ->
          downloadHandler.handleDownload(
              chatId, userId, action.payload(), QualityPreference.FULLHD);
      case UNSUPPORTED -> messageSender.sendMessage(chatId, action.originalMessage());
    }
  }
  
  static void main() throws Exception {
    String token = System.getenv("TELEGRAM_BOT_TOKEN");
    String name = System.getenv("TELEGRAM_BOT_NAME");

    if (token == null || name == null) {
      throw new IllegalStateException(
          "Environment variables TELEGRAM_BOT_TOKEN or TELEGRAM_BOT_NAME are not set");
    }

    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botName = name;
    botsApi.registerBot(new TikTokDownloader(token));
    log.info("Bot started successfully!");
  }
}
