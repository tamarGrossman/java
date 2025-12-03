package com.example.chalegesproject.service;

import com.example.chalegesproject.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIChatService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final static String SYSTEM_INSTRUCTION = """
             אתה עוזר AI שנועד לעזור למשתמשי האתר בתחומי הכושר בריאות ותזונה בלבד.
             אם שואלים אותך על נושאים אחרים תענה בנימוס שאתה מספק תשובות רק לנושאים אלו.
            תהיה נחמד ואדיב ואפשר לשלב קצת הומור עדין .
            ותספק טיפים לאתגרים ותן עידוד על מירת כושר ותזונה
            """;

    public AIChatService(ChatClient.Builder chatClient,ChatMemory chatMemory) {
        this.chatClient = chatClient.build();
        this.chatMemory=chatMemory;
    }


    public Flux<ChatResponse> getResponseStream(String prompt, String conversationId){
        List<org.springframework.ai.chat.messages.Message> messageList=new ArrayList<>();
        //ההודעה הראשונה - ההנחיה הראשונית
        messageList.add(new SystemMessage(SYSTEM_INSTRUCTION));
        //מוסיפים את כל ההודעות ששייכות לאותה השיחה
        messageList.addAll(chatMemory.get(conversationId));
        //השאלה הנוכחית
        UserMessage userMessage=new UserMessage(prompt);
        messageList.add(userMessage);

        Flux<String> aiResponseFlux = chatClient.prompt().messages(messageList)
                .stream()
                .content();
        // משתנה לאיסוף התגובה המלאה לצורך שמירת זיכרון
        StringBuilder finalResponseBuilder = new StringBuilder();

        //שמירת התגובה בזכרון
        //התגובה של ה-AI
        return aiResponseFlux
                .doOnNext(contentChunk -> finalResponseBuilder.append(contentChunk))
                .map(ChatResponse::new)
                .doOnComplete(() -> {
                    String fullResponse = finalResponseBuilder.toString();

                    AssistantMessage aiMessage = new AssistantMessage(fullResponse);

                    List<Message> messageListToSave =
                            List.of(userMessage, aiMessage);

                    chatMemory.add(conversationId, messageListToSave);

                    System.out.println("Memory saved for conversation: " + conversationId);
                });
}}