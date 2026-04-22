package com.gumtree.csagent.service.llm;

import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;

public interface LlmClient {

    LlmResponse chat(LlmRequest request);
}
