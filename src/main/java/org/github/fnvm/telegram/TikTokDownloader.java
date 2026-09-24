package org.github.fnvm.telegram;

import org.github.fnvm.data.QualityPreference;
import org.github.fnvm.telegram.action.Action;
import org.github.fnvm.telegram.action.Actions;
import org.github.fnvm.telegram.handler.*;
import org.github.fnvm.telegram.profile.UserProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.*;

public class TikTokDownloader extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(TikTokDownloader.class);

    private static final int DEFAULT_WORKER_COUNT = 4;
    private static final int DEFAULT_QUEUE_CAPACITY = 32;

    private static String botName;
    private final UserProfileManager profileManager;
    private final MessageSender messageSender;
    private final HelpHandler helpHandler;
    private final CookieHandler cookieHandler;
    private final ContentSender contentSender;
    private final DownloadHandler downloadHandler;
    private final ExecutorService[] workers;

    public TikTokDownloader(String token) {
        this(token, DEFAULT_WORKER_COUNT);
    }

    public TikTokDownloader(String token, int workerCount) {
        super(token);
        this.profileManager = new UserProfileManager();
        this.messageSender = new MessageSender(this);
        this.helpHandler = new HelpHandler(messageSender);
        this.cookieHandler = new CookieHandler(profileManager, messageSender);
        this.contentSender = new ContentSender(this, messageSender);
        this.downloadHandler = new DownloadHandler(profileManager, contentSender, messageSender);
        this.workers = createWorkers(Math.max(1, workerCount));
    }

    static void main() throws Exception {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        String name = System.getenv("TELEGRAM_BOT_NAME");

        if (token == null || name == null) {
            throw new IllegalStateException(
                    "Environment variables TELEGRAM_BOT_TOKEN or TELEGRAM_BOT_NAME are not set");
        }

        int workerCount = envInt("TELEGRAM_BOT_WORKERS", DEFAULT_WORKER_COUNT);

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botName = name;
        TikTokDownloader bot = new TikTokDownloader(token, workerCount);
        botsApi.registerBot(bot);
        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown, "bot-shutdown"));
        log.info("Bot started successfully!");
    }

    private static ExecutorService[] createWorkers(int count) {
        ExecutorService[] pool = new ExecutorService[count];
        for (int i = 0; i < count; i++) {
            int index = i;
            pool[i] = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
                    runnable -> {
                        Thread thread = new Thread(runnable, "update-worker-" + index);
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.AbortPolicy());
        }
        return pool;
    }

    private static int envInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {}: '{}', using default {}", name, value, defaultValue);
            return defaultValue;
        }
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
        Integer messageThreadId = message.getMessageThreadId();

        Action action = Actions.parse(text);

        int workerIndex = Math.floorMod(Long.hashCode(chatId), workers.length);
        try {
            workers[workerIndex].execute(() -> {
                try {
                    handleAction(action, chatId, userId, messageThreadId);
                } catch (Exception e) {
                    log.error("Error handling action for user {}: {}", userId, e.getMessage(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Update queue is full, rejecting update for user {}", userId);
            messageSender.sendMessage(
                    chatId, "The bot is busy right now. Please try again in a moment.", messageThreadId);
        }
    }

    private void shutdown() {
        for (ExecutorService worker : workers) {
            worker.shutdownNow();
        }
    }

    private void handleAction(Action action, Long chatId, Long userId, Integer messageThreadId) {
        switch (action.type()) {
            case HELP -> helpHandler.sendHelp(chatId, messageThreadId);
            case SET_COOKIE -> cookieHandler.handleSetCookie(chatId, userId, action.payload(), messageThreadId);
            case VIEW_COOKIE -> cookieHandler.handleViewCookie(chatId, userId, messageThreadId);
            case DELETE_COOKIE -> cookieHandler.handleDeleteCookie(userId, chatId, messageThreadId);
            case GET_SD ->
                downloadHandler.handleDownload(chatId, userId, action.payload(), QualityPreference.SD, messageThreadId);
            case GET_HD ->
                downloadHandler.handleDownload(chatId, userId, action.payload(), QualityPreference.HD, messageThreadId);
            case GET_FULLHD ->
                downloadHandler.handleDownload(
                        chatId, userId, action.payload(), QualityPreference.FULLHD, messageThreadId);
            case UNSUPPORTED -> {
                if (action.originalMessage() != null
                        && !action.originalMessage().isBlank()) {
                    messageSender.sendMessage(chatId, action.originalMessage(), messageThreadId);
                }
            }
        }
    }
}
