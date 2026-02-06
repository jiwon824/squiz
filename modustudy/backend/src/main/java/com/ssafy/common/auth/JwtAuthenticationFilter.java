package com.ssafy.common.auth;

import com.ssafy.common.util.JwtTokenUtil;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.service.UserService;  // ← import 수정!
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenUtil.validateToken(token)) {
            String userId = jwtTokenUtil.getUserId(token);
            Optional<User> userOpt = userService.getUserById(Long.parseLong(userId));  // ← 수정!

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                SsafyUserDetails userDetails = new SsafyUserDetails(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtTokenUtil.HEADER_STRING);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtTokenUtil.TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
