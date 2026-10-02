package com.zpantry.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MultipartCharsetFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String contentType = request.getContentType();
        
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data") && contentType.toLowerCase().contains("charset=")) {
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getContentType() {
                    return super.getContentType().replaceAll("(?i);\\s*charset=[^;]+", "");
                }

                @Override
                public String getHeader(String name) {
                    if ("content-type".equalsIgnoreCase(name)) {
                        return getContentType();
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("content-type".equalsIgnoreCase(name)) {
                        return Collections.enumeration(List.of(getContentType()));
                    }
                    return super.getHeaders(name);
                }
            };
        }
        
        filterChain.doFilter(request, response);
    }
}
