package com.checkmarx.dto;

public interface IRepoDto {
    String getId();

    String getWebhookId();

    String getName();
    
    boolean isWebHookEnabled();

    void setId(String id);

    void setWebhookId(String webhookId);

    void setWebHookEnabled(boolean webHookEnabled);
}
