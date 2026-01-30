package org.github.fnvm.telegram;

import org.github.fnvm.data.Content;
import org.github.fnvm.data.PhotoContent;
import org.github.fnvm.data.QualityPreference;
import org.github.fnvm.data.VideoContent;
import org.github.fnvm.scraper.ContentService;
import org.github.fnvm.scraper.UrlScrapingException;
import org.github.fnvm.telegram.profile.UserProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class TikTokDownloader extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(TikTokDownloader.class);
    private static final int MAX_PHOTOS_PER_GROUP = 10;

    private final UserProfileManager profileManager;

    public TikTokDownloader(String token) {
        super(token);
        this.profileManager = new UserProfileManager();
    }

    @Override
    public String getBotUsername() {
        return "";
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
            case HELP -> sendHelp(chatId);
            case SET_COOKIE -> handleSetCookie(chatId, userId, action.payload());
            case VIEW_COOKIE -> handleViewCookie(chatId, userId);
            case DELETE_COOKIE -> handleDeleteCookie(userId);
            case GET_SD -> handleDownload(chatId, userId, action.payload(), QualityPreference.SD);
            case GET_HD -> handleDownload(chatId, userId, action.payload(), QualityPreference.HD);
            case GET_FULLHD -> handleDownload(chatId, userId, action.payload(), QualityPreference.FULLHD);
            case UNSUPPORTED -> sendMessage(chatId, action.originalMessage());
        }
    }

    private void handleSetCookie(Long chatId, Long userId, String cookies) {
        if (cookies == null || cookies.isBlank()) {
            sendMessage(chatId, "Использование: /setcookie <sessionid>");
            return;
        }

        profileManager.setCookies(userId, cookies);
    }

    private void handleViewCookie(Long chatId, Long userId) {
        profileManager.getCookies(userId).ifPresentOrElse(
                cookies -> sendMessage(chatId, "🔑 Cookies:\n\n" + cookies),
                () -> sendMessage(chatId,
                        """
                        Не установлено.
                        
                        Используйте /setcookie для добавления.
                        
                        Для загрузки видео в максимальном качестве необходимо предоставить sessionid.
                        Осторожно! Может привести к бану аккаунта при злоупотреблении запросами.
                        """)
        );
    }

    private void handleDeleteCookie(Long userId) {
        if (profileManager.hasCookies(userId)) {
            profileManager.deleteCookies(userId);
        }
    }

    private void handleDownload(Long chatId, Long userId, String url, QualityPreference quality) {
        if (quality == QualityPreference.FULLHD && !profileManager.hasCookies(userId)) {
            sendMessage(chatId,
                    """
                            Для загрузки видео в максимальном качестве необходимо предоставить sessionid.
                            /setcookie <sessionid>
                          
                            Осторожно! Может привести к бану аккаунта при злоупотреблении запросами.
                          """);
            return;
        }

        try {
            String cookies = (quality == QualityPreference.FULLHD)
                    ? profileManager.getCookies(userId).orElse(null)
                    : null;

            Content content = ContentService.getContent(url, quality, cookies);
            sendContent(chatId, content);

        } catch (UrlScrapingException e) {
            log.error("Scraping failed for user {}: {}", userId, e.getMessage(), e);
            sendError(chatId, "Не удалось отправить видео");
        } catch (TelegramApiException e) {
            log.error("Telegram API error for user {}: {}", userId, e.getMessage(), e);
            sendError(chatId, "Не удалось выполнить действие");
        }
    }

    private void sendContent(Long chatId, Content content) throws TelegramApiException {
        switch (content) {
            case VideoContent video -> sendVideo(chatId, video);
            case PhotoContent photos -> sendPhotos(chatId, photos);
            default -> {}
        }
    }

    private void sendVideo(Long chatId, VideoContent video) throws TelegramApiException {
        if (video.exceedsTelegramLimit()) {
            sendMessage(chatId,
                    String.format(  """
                                    Размер видео: (%s)
                                    
                                    Прямая ссылка: %s
                                    """,
                            video.getFormattedSize(), video.url()));
            return;
        }

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(chatId.toString());
        sendVideo.setVideo(new InputFile(video.url()));

        video.getValidTitle().ifPresent(sendVideo::setCaption);

        execute(sendVideo);
        log.info("Sent {} video ({}) to chat {}",
                video.actualQuality(), video.getFormattedSize(), chatId);
    }

    private void sendPhotos(Long chatId, PhotoContent photos) throws TelegramApiException {
        if (photos.isEmpty()) {
            return;
        }

        List<String> urls = photos.urls();

        for (int i = 0; i < urls.size(); i += MAX_PHOTOS_PER_GROUP) {
            int end = Math.min(i + MAX_PHOTOS_PER_GROUP, urls.size());
            List<String> batch = urls.subList(i, end);

            if (batch.size() == 1) {
                sendSinglePhoto(chatId, batch.getFirst());
            } else {
                sendPhotoGroup(chatId, batch);
            }
        }

        log.info("Sent {} photos to chat {}", urls.size(), chatId);
    }

    private void sendSinglePhoto(Long chatId, String url) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(url));
        execute(sendPhoto);
    }

    private void sendPhotoGroup(Long chatId, List<String> urls) throws TelegramApiException {
        List<InputMedia> mediaGroup = new ArrayList<>();
        for (String url : urls) {
            InputMediaPhoto photo = new InputMediaPhoto();
            photo.setMedia(url);
            mediaGroup.add(photo);
        }

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(chatId.toString());
        sendMediaGroup.setMedias(mediaGroup);
        execute(sendMediaGroup);
    }

    private void sendHelp(Long chatId) {
        String help =   """
                        TikTok Downloader Bot
                
                        https://vt.tiktok.com/xxxxxxxx/
                        https://vt.tiktok.com/xxxxxxxx/ hd
                        https://vt.tiktok.com/xxxxxxxx/ fullhd (requires session cookies)
                
                        Or use commands:
                        • /get <link>
                        • /gethd <link>
                        • /getfull <link>
                
                        Other:
                        • /setcookie <sessionid> — set cookies required for Full HD downloads
                          Log in via a browser and extract the 'sessionid' cookie
                        • /viewcookie — view current cookies
                        • /deletecookie — delete cookies
                        """;

        sendMessage(chatId, help);
    }

    private void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendError(Long chatId, String errorText) {
        sendMessage(chatId, errorText);
    }


    static void main() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TikTokDownloader(""));
        log.info("Bot started successfully!");
    }
}