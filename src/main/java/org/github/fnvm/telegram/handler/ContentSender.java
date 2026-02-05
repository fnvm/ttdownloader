package org.github.fnvm.telegram.handler;

import org.github.fnvm.data.Content;
import org.github.fnvm.data.PhotoContent;
import org.github.fnvm.data.VideoContent;
import org.github.fnvm.telegram.TikTokDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.*;
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

  public void sendContent(Long chatId, Content content) throws TelegramApiException {
    switch (content) {
      case VideoContent video -> sendVideo(chatId, video);
      case PhotoContent photos -> sendPhotos(chatId, photos);
      default -> {}
    }
  }

  public void sendVideo(Long chatId, VideoContent video) throws TelegramApiException {
    if (video.exceedsTelegramLimit()) {
      sender.sendMessage(
          chatId,
          String.format(
              """
              Size: (%s)
              Link: %s
              """,
              video.getFormattedSize(), video.url()));
      log.info(
          "Sent {} video ({}) to chat {}", video.actualQuality(), video.getFormattedSize(), chatId);
      return;
    }

    SendVideo sendVideo = new SendVideo();
    sendVideo.setChatId(chatId.toString());
    sendVideo.setVideo(new InputFile(video.url()));

    video.getValidTitle().ifPresent(sendVideo::setCaption);

    bot.execute(sendVideo);
    log.info(
        "Sent {} video ({}) to chat {}", video.actualQuality(), video.getFormattedSize(), chatId);
  }

  public void sendPhotos(Long chatId, PhotoContent photos) throws TelegramApiException {
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

  public void sendSinglePhoto(Long chatId, String url) throws TelegramApiException {
    SendPhoto sendPhoto = new SendPhoto();
    sendPhoto.setChatId(chatId.toString());
    sendPhoto.setPhoto(new InputFile(url));
    bot.execute(sendPhoto);
  }

  public void sendPhotoGroup(Long chatId, List<String> urls) throws TelegramApiException {
    List<InputMedia> mediaGroup = new ArrayList<>();
    for (String url : urls) {
      InputMediaPhoto photo = new InputMediaPhoto();
      photo.setMedia(url);
      mediaGroup.add(photo);
    }

    SendMediaGroup sendMediaGroup = new SendMediaGroup();
    sendMediaGroup.setChatId(chatId.toString());
    sendMediaGroup.setMedias(mediaGroup);
    bot.execute(sendMediaGroup);
  }
}
