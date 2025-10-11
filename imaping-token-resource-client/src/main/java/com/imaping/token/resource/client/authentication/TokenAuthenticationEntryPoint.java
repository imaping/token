package com.imaping.token.resource.client.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imaping.token.api.exception.TokenAuthenticationException;
import com.imaping.token.api.exception.TokenError;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        final String loginUrl = request.getParameter("loginUrl");
        final String loginHash = request.getParameter("hash");
        if (!StringUtils.isEmpty(loginUrl)) {
            StringBuilder redirectUrlBuilder = new StringBuilder(loginUrl);
            if (!StringUtils.isEmpty(loginHash)) {
                redirectUrlBuilder.append("#").append(loginHash);
            }
            response.sendRedirect(redirectUrlBuilder.toString());
            return;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (exception instanceof TokenAuthenticationException) {
            final TokenAuthenticationException tokenAuthenticationException = (TokenAuthenticationException) exception;
            final TokenError error = tokenAuthenticationException.getError();
            response.setStatus(error.getHttpStatus().value());
            Map<String, Object> map = new HashMap<>();
            map.put("status", 0);
            map.put("message", error.getDescription());
            map.put("content", error.getErrorCode());

            response.getOutputStream().println(objectMapper.writeValueAsString(map));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> map = new HashMap<>();
            map.put("status", 0);
            map.put("message", exception.getMessage());
            response.getOutputStream().println(objectMapper.writeValueAsString(map));
        }
    }
}
