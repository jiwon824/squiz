package com.ssafy.domain.friend.mapper;

import com.ssafy.domain.friend.entity.Friendship;
import com.ssafy.domain.friend.entity.FriendshipStatus;
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
@DisplayName("FriendshipMapper 통합 테스트")
class FriendshipMapperTest {

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        user1 = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .nickname("테스트유저1")
                .name("Test User 1")
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
                .userId("testuser2")
                .email("test2@test.com")
                .nickname("테스트유저2")
                .name("Test User 2")
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
                .userId("testuser3")
                .email("test3@test.com")
                .nickname("테스트유저3")
                .name("Test User 3")
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
    @DisplayName("친구 관계 저장 및 조회")
    void insert_and_findById() {
        // given
        Friendship friendship = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.PENDING)
                .build();

        // when
        friendshipMapper.insert(friendship);

        // then
        assertThat(friendship.getId()).isNotNull();

        Friendship found = friendshipMapper.findById(friendship.getId());
        assertThat(found).isNotNull();
        assertThat(found.getRequesterId()).isEqualTo(user1.getId());
        assertThat(found.getAddresseeId()).isEqualTo(user2.getId());
        assertThat(found.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        // 프로필 정보도 조회되는지 확인 (LEFT JOIN profile)
        assertThat(found.getRequesterNickname()).isEqualTo("테스트유저1");
        assertThat(found.getAddresseeNickname()).isEqualTo("테스트유저2");
    }

    @Test
    @DisplayName("두 사용자 간 친구 관계 조회")
    void findByUsers() {
        // given
        Friendship friendship = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.ACCEPTED)
                .build();
        friendshipMapper.insert(friendship);

        // when - 정방향
        Friendship found1 = friendshipMapper.findByUsers(user1.getId(), user2.getId());
        // when - 역방향
        Friendship found2 = friendshipMapper.findByUsers(user2.getId(), user1.getId());

        // then
        assertThat(found1).isNotNull();
        assertThat(found2).isNotNull();
        assertThat(found1.getId()).isEqualTo(found2.getId());
    }

    @Test
    @DisplayName("친구 요청 상태 업데이트")
    void updateStatus() {
        // given
        Friendship friendship = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipMapper.insert(friendship);

        // when
        friendshipMapper.updateStatus(friendship.getId(), FriendshipStatus.ACCEPTED);

        // then
        Friendship updated = friendshipMapper.findById(friendship.getId());
        assertThat(updated.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("받은 친구 요청 목록 조회")
    void findReceivedRequests() {
        // given
        Friendship request1 = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user3.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        Friendship request2 = Friendship.builder()
                .requesterId(user2.getId())
                .addresseeId(user3.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipMapper.insert(request1);
        friendshipMapper.insert(request2);

        // when
        List<Friendship> received = friendshipMapper.findReceivedRequests(user3.getId());

        // then
        assertThat(received).hasSize(2);
        assertThat(received).allMatch(f -> f.getAddresseeId().equals(user3.getId()));
        assertThat(received).allMatch(f -> f.getStatus() == FriendshipStatus.PENDING);
    }

    @Test
    @DisplayName("보낸 친구 요청 목록 조회")
    void findSentRequests() {
        // given
        Friendship request1 = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        Friendship request2 = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user3.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipMapper.insert(request1);
        friendshipMapper.insert(request2);

        // when
        List<Friendship> sent = friendshipMapper.findSentRequests(user1.getId());

        // then
        assertThat(sent).hasSize(2);
        assertThat(sent).allMatch(f -> f.getRequesterId().equals(user1.getId()));
    }

    @Test
    @DisplayName("친구 목록 조회 (ACCEPTED 상태)")
    void findFriends() {
        // given
        Friendship friend1 = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.ACCEPTED)
                .build();
        Friendship friend2 = Friendship.builder()
                .requesterId(user3.getId())
                .addresseeId(user1.getId())
                .status(FriendshipStatus.ACCEPTED)
                .build();
        Friendship pending = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user3.getId())
                .status(FriendshipStatus.PENDING)
                .build();
        friendshipMapper.insert(friend1);
        friendshipMapper.insert(friend2);
        friendshipMapper.insert(pending);

        // when
        List<Friendship> friends = friendshipMapper.findFriends(user1.getId());

        // then
        assertThat(friends).hasSize(2);
        assertThat(friends).allMatch(f -> f.getStatus() == FriendshipStatus.ACCEPTED);
    }

    @Test
    @DisplayName("친구 관계 삭제")
    void delete() {
        // given
        Friendship friendship = Friendship.builder()
                .requesterId(user1.getId())
                .addresseeId(user2.getId())
                .status(FriendshipStatus.ACCEPTED)
                .build();
        friendshipMapper.insert(friendship);
        Long id = friendship.getId();

        // when
        friendshipMapper.delete(id);

        // then
        Friendship deleted = friendshipMapper.findById(id);
        assertThat(deleted).isNull();
    }
}
