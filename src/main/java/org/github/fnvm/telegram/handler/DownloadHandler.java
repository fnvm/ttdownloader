package org.github.fnvm.telegram.handler;

import org.github.fnvm.data.Content;
import org.github.fnvm.data.QualityPreference;
import org.github.fnvm.scraper.ContentService;
import org.github.fnvm.scraper.UrlScrapingException;
import org.github.fnvm.telegram.profile.UserProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class DownloadHandler {

    private static final Logger log = LoggerFactory.getLogger(DownloadHandler.class);

    private final UserProfileManager profileManager;
    private final ContentSender contentSender;
    private final MessageSender sender;

    public DownloadHandler(UserProfileManager profileManager, ContentSender contentSender, MessageSender sender) {
        this.profileManager = profileManager;
        this.contentSender = contentSender;
        this.sender = sender;
    }

    public void handleDownload(
            Long chatId, Long userId, String url, QualityPreference quality, Integer messageThreadId) {
        boolean fullhd = quality == QualityPreference.FULLHD;
        if (fullhd && !profileManager.hasCookies(userId)) {
            sender.sendMessage(chatId, """
                To download videos in maximum quality, you must provide a sessionid.
                Use /setcookie <sessionid>
                Warning! Excessive requests may lead to your account being banned.
                """, messageThreadId);
            return;
        }

        try {
            String cookies = (fullhd) ? profileManager.getCookies(userId).orElse("") : "";

            Content content = ContentService.getContent(url, quality, cookies);
            contentSender.sendContent(chatId, content, messageThreadId);

        } catch (UrlScrapingException e) {
            log.error("Scraping failed for user {}: {}", userId, e.getMessage(), e);
            sender.sendError(chatId, "Failed to send the video", messageThreadId);
        } catch (TelegramApiException e) {
            log.error("Telegram API error for user {}: {}", userId, e.getMessage(), e);
            sender.sendError(chatId, "Failed to perform the action", messageThreadId);
        }
    }
}
