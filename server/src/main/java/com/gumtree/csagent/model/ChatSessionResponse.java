package com.gumtree.csagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("reply_text")
    private String replyText;

    private String intent;

    @JsonProperty("should_end_chat")
    private boolean shouldEndChat;

    @JsonProperty("additional_data")
    private Map<String, Object> additionalData;
}
