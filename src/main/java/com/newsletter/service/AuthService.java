package com.newsletter.service;

import com.newsletter.dto.request.AuthRequest;
import com.newsletter.dto.response.ApiResponse;

public interface AuthService {
    ApiResponse.Auth register(AuthRequest.Register request);
    ApiResponse.Auth login(AuthRequest.Login request);
}
