package com.example.chalegesproject.dto;

public record ChatRequest(String message,String conversationId) {
    public ChatRequest{
        if(conversationId == null ||conversationId. isBlank()){
            conversationId="default-user";}
}
}