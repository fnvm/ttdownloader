package org.github.fnvm.telegram;

import org.github.fnvm.scraper.Scraper;
import org.github.fnvm.scraper.UrlScrapingException;
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

import static org.github.fnvm.data.ContentType.BLANK;
import static org.github.fnvm.data.ContentType.VIDEO;

public class TikTokDownloader extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(TikTokDownloader.class);


    public TikTokDownloader(String token) {
        super(token);
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        String text = message.getText().trim();

        try {
            if (text.startsWith("https://vt.tiktok.com") || text.startsWith("https://www.tiktok.com")) {
                String[] parts = text.split("\\s+", 2);
                boolean hd = parts.length > 1 && parts[1].equalsIgnoreCase("hd");
                sendContentFromUrl(message, parts[0], hd);

            } else if (text.startsWith("/get ") || text.startsWith("/gethd ")) {
                boolean hd = text.startsWith("/gethd ");
                String[] parts = text.split("\\s+", 2);
                if (parts.length < 2) return;
                sendContentFromUrl(message, parts[1], hd);
            }

        } catch (TelegramApiException e) {
            log.error("Failed to send video to chat {}: {}", message.getChatId(), e.getMessage(), e);
            sendErrorMessage(message.getChatId(), "Не удалось отправить видео.");
        } catch (UrlScrapingException e) {
            log.error("Failed to scrape video for chat {}: {}", message.getChatId(), e.getMessage(), e);
            sendErrorMessage(message.getChatId(), "Не удалось загрузить видео.");
        }
    }

    private void sendErrorMessage(Long chatId, String errorText) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(errorText);
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send error message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendContentFromUrl(Message message, String url, boolean hd)
            throws UrlScrapingException, TelegramApiException {

        ContentResult result = Scraper.getContent(url, hd);
        if (result.type() == BLANK) return;
        if (result.type() == VIDEO) {
            List<String> listFromScraper = result.content();
            if (listFromScraper.isEmpty()) return;
            String link = listFromScraper.getFirst();
            if (link.isEmpty()) return;
            sendVideo(message, link);
        } else {
            List<String> listFromScraper = result.content();
            if (listFromScraper.isEmpty()) return;
            sendPhotos(message, listFromScraper);
        }


    }

    private void sendVideo(Message message, String link) throws TelegramApiException {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(message.getChatId().toString());
        sendVideo.setVideo(new InputFile(link));
        execute(sendVideo);
        log.info("Successfully sent video to chat {}", message.getChatId());
    }

    private void sendPhotos(Message message, List<String> photoUrls) throws TelegramApiException {
        int maxPhotosPerGroup = 10;

        for (int i = 0; i < photoUrls.size(); i += maxPhotosPerGroup) {
            int end = Math.min(i + maxPhotosPerGroup, photoUrls.size());
            List<String> batch = photoUrls.subList(i, end);

            if (batch.size() == 1) {
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(message.getChatId().toString());
                sendPhoto.setPhoto(new InputFile(batch.getFirst()));
                execute(sendPhoto);
            } else {
                List<InputMedia> mediaGroup = new ArrayList<>();
                for (String photoUrl : batch) {
                    InputMediaPhoto photo = new InputMediaPhoto();
                    photo.setMedia(photoUrl);
                    mediaGroup.add(photo);
                }

                SendMediaGroup sendMediaGroup = new SendMediaGroup();
                sendMediaGroup.setChatId(message.getChatId().toString());
                sendMediaGroup.setMedias(mediaGroup);
                execute(sendMediaGroup);
            }
        }

        log.info("Successfully sent {} photos to chat {}", photoUrls.size(), message.getChatId());
    }

    static void main() throws Exception {

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new TikTokDownloader(""));
        System.out.println("Bot started!");
    }
}