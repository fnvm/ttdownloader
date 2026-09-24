package org.github.fnvm.telegram.handler;

public class HelpHandler {

    private final MessageSender sender;

    public HelpHandler(MessageSender sender) {
        this.sender = sender;
    }

    public void sendHelp(Long chatId, Integer messageThreadId) {
        String help = """
            TikTok Downloader Bot

            https://vt.tiktok.com/xxxxxxxx/
            https://vt.tiktok.com/xxxxxxxx/ hd
            https://vt.tiktok.com/xxxxxxxx/ o (fullhd)

            Or use commands:
            • /get <link>
            • /gethd <link>
            • /getfull <link> (requires session cookies)

            Other:
            • /setcookie <sessionid> — set cookies required for Full HD downloads
              Log in via a browser and extract the 'sessionid' cookie
            • /viewcookie — view current cookies
            • /deletecookie — delete cookies
            """;

        sender.sendMessage(chatId, help, messageThreadId);
    }
}
