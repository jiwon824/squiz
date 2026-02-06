package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.entity.UserBlock;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("UserBlockMapper 통합 테스트")
class UserBlockMapperTest {

    @Autowired
    private UserBlockMapper userBlockMapper;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder()
                .userId("blocktest1")
                .email("block1@test.com")
                .nickname("차단테스트1")
                .name("Block Test 1")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user2 = userRepository.save(User.builder()
                .userId("blocktest2")
                .email("block2@test.com")
                .nickname("차단테스트2")
                .name("Block Test 2")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());

        user3 = userRepository.save(User.builder()
                .userId("blocktest3")
                .email("block3@test.com")
                .nickname("차단테스트3")
                .name("Block Test 3")
                .role(Role.USER)
                .isActive(true)
                .isOnline(true)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
    }

    @Test
    @DisplayName("차단 저장 및 조회")
    void insert_and_findByBlockerAndBlocked() {
        // given
        UserBlock block = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .build();

        // when
        userBlockMapper.insert(block);

        // then
        assertThat(block.getId()).isNotNull();

        UserBlock found = userBlockMapper.findByBlockerAndBlocked(user1.getId(), user2.getId());
        assertThat(found).isNotNull();
        assertThat(found.getBlockerId()).isEqualTo(user1.getId());
        assertThat(found.getBlockedId()).isEqualTo(user2.getId());
        // profile 테이블 JOIN 확인
        assertThat(found.getBlockedNickname()).isEqualTo("차단테스트2");
    }

    @Test
    @DisplayName("차단 여부 확인")
    void existsByBlockerAndBlocked() {
        // given
        UserBlock block = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .build();
        userBlockMapper.insert(block);

        // when & then
        assertThat(userBlockMapper.existsByBlockerAndBlocked(user1.getId(), user2.getId())).isTrue();
        assertThat(userBlockMapper.existsByBlockerAndBlocked(user2.getId(), user1.getId())).isFalse();
        assertThat(userBlockMapper.existsByBlockerAndBlocked(user1.getId(), user3.getId())).isFalse();
    }

    @Test
    @DisplayName("양방향 차단 확인")
    void existsAnyBlock() {
        // given
        UserBlock block = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .build();
        userBlockMapper.insert(block);

        // when & then - 한쪽만 차단해도 양방향 확인 시 true
        assertThat(userBlockMapper.existsAnyBlock(user1.getId(), user2.getId())).isTrue();
        assertThat(userBlockMapper.existsAnyBlock(user2.getId(), user1.getId())).isTrue();
        assertThat(userBlockMapper.existsAnyBlock(user1.getId(), user3.getId())).isFalse();
    }

    @Test
    @DisplayName("내가 차단한 사용자 목록 조회")
    void findByBlockerId() {
        // given
        UserBlock block1 = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .build();
        UserBlock block2 = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user3.getId())
                .build();
        userBlockMapper.insert(block1);
        userBlockMapper.insert(block2);

        // when
        List<UserBlock> blockedList = userBlockMapper.findByBlockerId(user1.getId());

        // then
        assertThat(blockedList).hasSize(2);
        assertThat(blockedList).allMatch(b -> b.getBlockerId().equals(user1.getId()));
        // profile JOIN 확인
        assertThat(blockedList).anyMatch(b -> b.getBlockedNickname().equals("차단테스트2"));
        assertThat(blockedList).anyMatch(b -> b.getBlockedNickname().equals("차단테스트3"));
    }

    @Test
    @DisplayName("나를 차단한 사용자 ID 목록 조회")
    void findBlockerIdsByBlockedId() {
        // given - user2, user3이 user1을 차단
        UserBlock block1 = UserBlock.builder()
                .blockerId(user2.getId())
                .blockedId(user1.getId())
                .build();
        UserBlock block2 = UserBlock.builder()
                .blockerId(user3.getId())
                .blockedId(user1.getId())
                .build();
        userBlockMapper.insert(block1);
        userBlockMapper.insert(block2);

        // when
        List<Long> blockerIds = userBlockMapper.findBlockerIdsByBlockedId(user1.getId());

        // then
        assertThat(blockerIds).hasSize(2);
        assertThat(blockerIds).contains(user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("차단 삭제")
    void delete() {
        // given
        UserBlock block = UserBlock.builder()
                .blockerId(user1.getId())
                .blockedId(user2.getId())
                .build();
        userBlockMapper.insert(block);
        Long id = block.getId();

        // when
        userBlockMapper.delete(id);

        // then
        assertThat(userBlockMapper.existsByBlockerAndBlocked(user1.getId(), user2.getId())).isFalse();
    }
}
