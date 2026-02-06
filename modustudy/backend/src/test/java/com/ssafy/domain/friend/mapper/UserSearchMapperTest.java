package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.dto.response.UserSearchResponse;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("UserSearchMapper 통합 테스트")
class UserSearchMapperTest {

    @Autowired
    private UserSearchMapper userSearchMapper;

    @Autowired
    private UserRepository userRepository;

    private User searchingUser;
    private User targetUser1;
    private User targetUser2;
    private User hiddenUser;

    @BeforeEach
    void setUp() {
        // 검색하는 사용자
        searchingUser = userRepository.save(User.builder()
                .userId("searcher")
                .email("searcher@test.com")
                .nickname("검색자")
                .name("Searcher")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        // 검색 대상 사용자 1
        targetUser1 = userRepository.save(User.builder()
                .userId("target1")
                .email("target1@test.com")
                .nickname("알고리즘고수")
                .name("Target 1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(100)
                .currentPoints(50)
                .currentLevel(2)
                .levelName("Silver")
                .build());

        // 검색 대상 사용자 2
        targetUser2 = userRepository.save(User.builder()
                .userId("target2")
                .email("target2@test.com")
                .nickname("알고리즘초보")
                .name("Target 2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        // 검색 비허용 사용자
        hiddenUser = userRepository.save(User.builder()
                .userId("hidden")
                .email("hidden@test.com")
                .nickname("알고리즘숨김")
                .name("Hidden")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(false)  // 검색 비허용
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 기본 검색")
    void searchByNickname_basicSearch() {
        // given
        String keyword = "알고리즘";

        // when
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                Collections.emptyList()
        );

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.nickname().contains("알고리즘"));
        // 검색 비허용 사용자는 결과에 없어야 함
        assertThat(results).noneMatch(r -> r.nickname().equals("알고리즘숨김"));
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 본인 제외")
    void searchByNickname_excludesSelf() {
        // given
        String keyword = "검색";

        // when
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                Collections.emptyList()
        );

        // then - 본인은 결과에 포함되지 않아야 함
        assertThat(results).noneMatch(r -> r.userId().equals(searchingUser.getId()));
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 차단한 사용자 제외")
    void searchByNickname_excludesBlockers() {
        // given
        String keyword = "알고리즘";
        List<Long> blockerIds = List.of(targetUser1.getId());

        // when
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                blockerIds
        );

        // then - 차단한 사용자는 결과에서 제외
        assertThat(results).hasSize(1);
        assertThat(results.get(0).nickname()).isEqualTo("알고리즘초보");
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 결과 없음")
    void searchByNickname_noResults() {
        // given
        String keyword = "존재하지않는닉네임";

        // when
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                Collections.emptyList()
        );

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 온라인 상태 확인")
    void searchByNickname_checksOnlineStatus() {
        // given
        String keyword = "알고리즘";

        // when
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                Collections.emptyList()
        );

        // then
        UserSearchResponse onlineUser = results.stream()
                .filter(r -> r.nickname().equals("알고리즘고수"))
                .findFirst()
                .orElseThrow();
        UserSearchResponse offlineUser = results.stream()
                .filter(r -> r.nickname().equals("알고리즘초보"))
                .findFirst()
                .orElseThrow();

        assertThat(onlineUser.isOnline()).isTrue();
        assertThat(offlineUser.isOnline()).isFalse();
    }

    @Test
    @DisplayName("닉네임으로 사용자 검색 - 프로필 이미지 조회 (profile 테이블 JOIN)")
    void searchByNickname_fetchesProfileImage() {
        // given
        String keyword = "알고리즘";

        // when - 이 쿼리가 성공하면 profile 테이블 JOIN이 정상 동작하는 것
        List<UserSearchResponse> results = userSearchMapper.searchByNickname(
                keyword,
                searchingUser.getId(),
                Collections.emptyList()
        );

        // then - profileImage는 null일 수 있지만 쿼리 자체는 성공해야 함
        assertThat(results).isNotEmpty();
        // profile 테이블에 데이터가 없으면 null, 있으면 URL
        results.forEach(r -> {
            // profileImage 필드가 조회되는지 확인 (null 허용)
            assertThat(r).hasFieldOrProperty("profileImage");
        });
    }
}
