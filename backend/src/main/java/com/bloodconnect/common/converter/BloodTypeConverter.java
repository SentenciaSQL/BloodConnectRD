package com.bloodconnect.common.converter;

import com.bloodconnect.common.enums.BloodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BloodTypeConverter implements AttributeConverter<BloodType, String> {

    @Override
    public String convertToDatabaseColumn(BloodType attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public BloodType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BloodType.fromLabel(dbData);
    }
}
