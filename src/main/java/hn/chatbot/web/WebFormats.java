package hn.chatbot.web;

import java.net.URI;

/** 표현 전용 파생값. url 에서 만들어내므로 저장하지 않는다. */
final class WebFormats {

    private WebFormats() {
    }

    /** 화면 카드에 띄우는 출처 표시. */
    static String domainOf(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        }
        catch (IllegalArgumentException e) {
            return "";
        }
    }
}
