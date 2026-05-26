package com.gate.mockexam.dto;

import java.util.UUID;

public record StudentDetailsDto(
    UUID id,
    String fullName,
    String email
) {}
