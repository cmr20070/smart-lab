




package com.smartlab.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule module = new JavaTimeModule();

            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FMT));
            module.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());

            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FMT));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FMT));

            module.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FMT));
            module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FMT));

            builder.modules(module);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.dateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            builder.timeZone(java.util.TimeZone.getTimeZone("GMT+8"));
        };
    }

    private static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                if (text.contains("T") && (text.endsWith("Z") || text.contains("+") || text.length() > 22)) {
                    return Instant.parse(text).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                if (text.contains("T")) {
                    return LocalDateTime.parse(text);
                }
                return LocalDateTime.parse(text, DATETIME_FMT);
            } catch (Exception e) {
                throw new IOException("无法解析日期时间: " + text, e);
            }
        }
    }
}