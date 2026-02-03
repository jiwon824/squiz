package com.ssafy.domain.board.repository;

import com.ssafy.domain.board.entity.BoardCategory;
import com.ssafy.domain.board.entity.BoardComment;
import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardCommentRepositoryTest {

    @Autowired
    private BoardCommentRepository boardCommentRepository;

    @Autowired
    private BoardPostRepository boardPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private User leader;
    private User member;
    private BoardPost post;

    @BeforeEach
    void setUp() {
        Topic topic = topicRepository.save(Topic.builder()
                .name("Java")
                .sortOrder(1)
                .build());
        topicRepository.flush();

        Format format = formatRepository.save(Format.builder()
                .name("Code Review")
                .sortOrder(1)
                .build());
        formatRepository.flush();

        leader = userRepository.save(User.builder()
                .userId("leader")
                .email("leader@test.com")
                .nickname("leader")
                .name("Leader")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        member = userRepository.save(User.builder()
                .userId("member")
                .email("member@test.com")
                .nickname("member")
                .name("Member")
                .role(Role.USER)
                .isActive(true)
                .isOnline(false)
                .isSearchable(true)
                .totalExp(0)
                .currentPoints(0)
                .currentLevel(1)
                .levelName("Bronze")
                .build());
        userRepository.flush();

        Study study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .topic(topic)
                .format(format)
                .name("Board Study")
                .description("게시판 댓글 테스트")
                .maxMembers(6)
                .studyType(StudyType.PLANNED)
                .status(Status.RECRUITING)
                .recruitStartDate(LocalDate.now().minusDays(3))
                .recruitEndDate(LocalDate.now().plusDays(3))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        studyRepository.flush();

        post = boardPostRepository.save(new BoardPost(
                leader, study, BoardCategory.FREE, "모집글", "내용"));
        boardPostRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("삭제되지 않은 댓글만 등록순으로 조회한다")
    void findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc() throws Exception {
        BoardComment first = boardCommentRepository.save(new BoardComment(post, leader, null, "첫 댓글"));
        boardCommentRepository.flush();
        Thread.sleep(5);
        BoardComment deleted = boardCommentRepository.save(new BoardComment(post, member, null, "삭제 댓글"));
        deleted.delete();
        boardCommentRepository.flush();
        Thread.sleep(5);
        BoardComment third = boardCommentRepository.save(new BoardComment(post, member, null, "세 번째 댓글"));
        boardCommentRepository.flush();
        entityManager.clear();

        List<BoardComment> comments = boardCommentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(post.getId());

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getId()).isEqualTo(first.getId());
        assertThat(comments.get(1).getId()).isEqualTo(third.getId());
    }
}
