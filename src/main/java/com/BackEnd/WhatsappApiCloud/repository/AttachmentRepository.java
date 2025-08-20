package com.BackEnd.WhatsappApiCloud.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.BackEnd.WhatsappApiCloud.model.entity.user.AttachmentEntity;
import com.BackEnd.WhatsappApiCloud.util.enums.AttachmentStatus;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    List<AttachmentEntity> findByWhatsappPhoneAndAttachmentStatusAndTimestampBetween(
        String whatsappPhone,
        AttachmentStatus attachmentStatus,
        Instant start,
        Instant end
    );

}