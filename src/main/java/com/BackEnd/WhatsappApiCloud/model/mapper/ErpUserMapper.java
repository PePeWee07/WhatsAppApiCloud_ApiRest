package com.BackEnd.WhatsappApiCloud.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE  // Ignora campos en UserChatEntity que no mapeemos
)
public interface ErpUserMapper {

}