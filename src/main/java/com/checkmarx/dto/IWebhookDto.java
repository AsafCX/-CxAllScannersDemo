package com.checkmarx.dto;

public interface IWebhookDto {

    String getUrl();

    boolean isActive();
    
    String getId();

    default boolean isPushOrPull(){ return true;}
}
