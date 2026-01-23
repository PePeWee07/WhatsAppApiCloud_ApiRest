package com.BackEnd.WhatsappApiCloud.model.entity.openIA;

import java.time.Instant;
import java.util.Set;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageEntity;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "ai_response")
public class AiResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private String responseId;

    @Column(name = "previous_response_id")
    private String previousResponseId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "prompt_id", nullable = false)
    private String promptId;

    @Column(name = "prompt_variables", columnDefinition = "TEXT")
    private String promptVariables;

    @Column(name = "prompt_version", nullable = false)
    private String promptVersion;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @OneToMany(mappedBy="aiResponse", fetch = FetchType.LAZY)
    @OrderColumn(name="message_index")
    @JsonManagedReference
    private Set<AiToolCallEntity> toolCalls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;


}
