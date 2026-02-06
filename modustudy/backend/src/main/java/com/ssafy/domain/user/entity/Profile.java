package com.ssafy.domain.user.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_image_source")
    private ProfileImageSource profileImageSource;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "social_links", columnDefinition = "JSON")
    private String socialLinks;

    @Column(columnDefinition = "JSON")
    private String tech;

    @Column(columnDefinition = "JSON")
    private String favorite;

    public enum ProfileImageSource {
        KAKAO, GOOGLE, NAVER, UPLOAD
    }
}
