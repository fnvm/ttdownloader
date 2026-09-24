package org.github.fnvm.telegram.handler;

import org.github.fnvm.data.Content;
import org.github.fnvm.data.PhotoContent;
import org.github.fnvm.data.VideoContent;
import org.github.fnvm.telegram.TikTokDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class ContentSender {

    private static final Logger log = LoggerFactory.getLogger(ContentSender.class);
    private static final int MAX_PHOTOS_PER_GROUP = 10;

    private final TikTokDownloader bot;
    private final MessageSender sender;

    public ContentSender(TikTokDownloader bot, MessageSender sender) {
        this.bot = bot;
        this.sender = sender;
    }

    public void sendContent(Long chatId, Content content, Integer messageThreadId) throws TelegramApiException {
        switch (content) {
            case VideoContent video -> sendVideo(chatId, video, messageThreadId);
            case PhotoContent photos -> sendPhotos(chatId, photos, messageThreadId);
            default -> {}
        }
    }

    public void sendVideo(Long chatId, VideoContent video, Integer messageThreadId) throws TelegramApiException {
        if (video.exceedsTelegramLimit()) {
            sender.sendMessage(chatId, String.format("""
                Size: (%s)
                Link: %s
                """, video.getFormattedSize(), video.url()), messageThreadId);
            log.info("Sent {} video ({}) to chat {}", video.actualQuality(), video.getFormattedSize(), chatId);
            return;
        }

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(chatId.toString());
        sendVideo.setVideo(new InputFile(video.url()));
        if (messageThreadId != null) {
            sendVideo.setMessageThreadId(messageThreadId);
        }

        video.getValidTitle().ifPresent(title -> {
            String caption = title.length() > 320 ? title.substring(0, 320) + "..." : title;
            sendVideo.setCaption(caption);
        });

        try {
            bot.execute(sendVideo);
            log.info("Sent {} video ({}) to chat {}", video.actualQuality(), video.getFormattedSize(), chatId);
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("failed to get HTTP URL content")) {
                log.warn("Failed to send video via Telegram, sending direct link instead for chat {}", chatId);
                sender.sendMessage(chatId, String.format("""
                    Video is too long
                    Link: %s
                    """, video.url()), messageThreadId);
            } else {
                throw e;
            }
        }
    }

    public void sendPhotos(Long chatId, PhotoContent photos, Integer messageThreadId) throws TelegramApiException {
        if (photos.isEmpty()) {
            return;
        }

        List<String> urls = photos.urls();

        for (int i = 0; i < urls.size(); i += MAX_PHOTOS_PER_GROUP) {
            int end = Math.min(i + MAX_PHOTOS_PER_GROUP, urls.size());
            List<String> batch = urls.subList(i, end);

            if (batch.size() == 1) {
                sendSinglePhoto(chatId, batch.getFirst(), messageThreadId);
            } else {
                sendPhotoGroup(chatId, batch, messageThreadId);
            }
        }

        log.info("Sent {} photos to chat {}", urls.size(), chatId);
    }

    public void sendSinglePhoto(Long chatId, String url, Integer messageThreadId) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(url));
        if (messageThreadId != null) {
            sendPhoto.setMessageThreadId(messageThreadId);
        }
        bot.execute(sendPhoto);
    }

    public void sendPhotoGroup(Long chatId, List<String> urls, Integer messageThreadId) throws TelegramApiException {
        List<InputMedia> mediaGroup = new ArrayList<>();
        for (String url : urls) {
            InputMediaPhoto photo = new InputMediaPhoto();
            photo.setMedia(url);
            mediaGroup.add(photo);
        }

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(chatId.toString());
        sendMediaGroup.setMedias(mediaGroup);
        if (messageThreadId != null) {
            sendMediaGroup.setMessageThreadId(messageThreadId);
        }
        bot.execute(sendMediaGroup);
    }
}
