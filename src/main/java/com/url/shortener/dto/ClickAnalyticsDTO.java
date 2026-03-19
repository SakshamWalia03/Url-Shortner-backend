package com.url.shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalyticsDTO {
    private LocalDate clickDate;
    private int count;
}
