package com.bloodconnect.common.converter;

import com.bloodconnect.common.enums.BloodType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class BloodTypeWebConverter implements Converter<String, BloodType> {

    @Override
    public BloodType convert(String source) {
        return BloodType.fromLabel(source);
    }
}
