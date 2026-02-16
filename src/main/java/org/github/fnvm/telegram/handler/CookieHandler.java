package org.github.fnvm.telegram.handler;

import org.github.fnvm.telegram.profile.UserProfileManager;

public class CookieHandler {
  private final UserProfileManager profileManager;
  private final MessageSender sender;

  public CookieHandler(UserProfileManager profileManager, MessageSender sender) {
    this.profileManager = profileManager;
    this.sender = sender;
  }

  public void handleSetCookie(Long chatId, Long userId, String cookies, Integer messageThreadId) {
    if (cookies == null || cookies.isBlank()) {
      sender.sendMessage(chatId, "Usage: /setcookie <sessionid>", messageThreadId);
      return;
    }

    profileManager.setCookies(userId, cookies);
    sender.sendMessage(chatId, "Cookies have been set", messageThreadId);
  }

  public void handleViewCookie(Long chatId, Long userId, Integer messageThreadId) {
    profileManager
        .getCookies(userId)
        .ifPresentOrElse(
            cookies -> sender.sendMessage(chatId, "🔑 Cookies:\n\n" + cookies, messageThreadId),
            () ->
                sender.sendMessage(
                    chatId,
                    """
                    Not set.
                    Use /setcookie to add it.
                    To download videos in maximum quality, you must provide a sessionid.
                    Be careful! Excessive requests may lead to your account being banned.
                    """,
                    messageThreadId));
  }

  public void handleDeleteCookie(Long userId, Long chatId, Integer messageThreadId) {
    if (profileManager.hasCookies(userId)) {
      profileManager.deleteCookies(userId);
      sender.sendMessage(chatId, "Cookies have been removed", messageThreadId);
    }
  }
}
