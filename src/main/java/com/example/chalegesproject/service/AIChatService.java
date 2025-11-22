package com.example.chalegesproject.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIChatService {
    private final ChatClient chatClient;
    private final static String SYSTEM_INSTRUCTION = """
             אתה עוזר AI שנועד לעזור למשתמשי האתר בתחומי הכושר בריאות ותזונה בלבד.
             אם שואלים אותך על נושאים אחרים תענה בנימוס שאתה מספק תשובות רק לנושאים אלו.
            תהיה נחמד ואדיב ואפשר לשלב קצת הומור עדין .
            ותספק טיפים לאתגרים ותן עידוד על מירת כושר ותזונה
            """;

    public AIChatService(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    public String getResponse(String prompt) {
        SystemMessage systemMessage = new SystemMessage(SYSTEM_INSTRUCTION);
        UserMessage userMessage = new UserMessage(prompt);

        List<Message> messageList = List.of(systemMessage, userMessage);

        return chatClient.prompt().messages(messageList).call().content();
    }
}