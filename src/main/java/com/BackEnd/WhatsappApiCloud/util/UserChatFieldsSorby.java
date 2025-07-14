package com.BackEnd.WhatsappApiCloud.util;

import java.util.Set;

public class UserChatFieldsSorby {
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "whatsappPhone",
        "previousResponseId",
        "limitQuestions",
        "firstInteraction",
        "lastInteraction",
        "nextResetDate",
        "conversationState",
        "limitStrike",
        "block",
        "blockingReason",
        "validQuestionCount",
        "chatSessions",
        "identificacion"
    );
}
