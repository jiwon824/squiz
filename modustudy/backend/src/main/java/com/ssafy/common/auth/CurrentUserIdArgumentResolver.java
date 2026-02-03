package com.ssafy.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUserId Long userId 파라미터를 처리하는 ArgumentResolver
 * SsafyUserDetails에서 userId를 추출하여 Long 타입으로 반환
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentUserId 어노테이션이 있고, 파라미터 타입이 Long인 경우
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // SsafyUserDetails인 경우 userId 추출
        if (principal instanceof SsafyUserDetails) {
            SsafyUserDetails userDetails = (SsafyUserDetails) principal;
            return userDetails.getUser().getId();
        }

        // String인 경우 (anonymousUser 등)
        if (principal instanceof String) {
            return null;
        }

        return null;
    }
}
