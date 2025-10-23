package org.tms.service;


import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public String generateToken(String username) {
        return "dummy-jwt-for-" + username;
    }
}
