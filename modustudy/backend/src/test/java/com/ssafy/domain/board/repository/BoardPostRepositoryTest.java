package com.ssafy.domain.board.repository;

import com.ssafy.domain.board.entity.BoardCategory;
import com.ssafy.domain.board.entity.BoardPost;
import com.ssafy.domain.board.entity.RecruitmentStatus;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.MeetingType;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BoardPostRepositoryTest {

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
    private Study study;

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

        study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .topic(topic)
                .format(format)
                .name("Board Study")
                .description("Board study description")
                .maxMembers(6)
                .studyType(StudyType.PLANNED)
                .status(Status.RECRUITING)
                .recruitStartDate(LocalDate.now().minusDays(3))
                .recruitEndDate(LocalDate.now().plusDays(3))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        studyRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("List latest non-deleted posts")
    void findAllByIsDeletedFalseOrderByCreatedAtDesc() throws Exception {
        BoardPost first = boardPostRepository.save(new BoardPost(
                leader, study, BoardCategory.FREE, "첫 번째 글", "내용1",
                null, MeetingType.ONLINE, 6, RecruitmentStatus.RECRUITING));
        boardPostRepository.flush();
        Thread.sleep(5);
        BoardPost second = boardPostRepository.save(new BoardPost(
                leader, study, BoardCategory.FREE, "두 번째 글", "내용2",
                null, MeetingType.ONLINE, 6, RecruitmentStatus.RECRUITING));
        boardPostRepository.flush();
        Thread.sleep(5);
        BoardPost deleted = boardPostRepository.save(new BoardPost(
                leader, study, BoardCategory.FREE, "삭제 글", "내용3",
                null, MeetingType.ONLINE, 6, RecruitmentStatus.RECRUITING));
        deleted.delete();
        boardPostRepository.flush();
        entityManager.clear();

        Page<BoardPost> page = boardPostRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(page.getContent().get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    @DisplayName("Exclude deleted post from detail query")
    void findByIdAndIsDeletedFalse() {
        BoardPost post = boardPostRepository.save(new BoardPost(
                leader, study, BoardCategory.FREE, "상세 조회", "내용",
                null, MeetingType.ONLINE, 6, RecruitmentStatus.RECRUITING));
        boardPostRepository.flush();

        assertThat(boardPostRepository.findByIdAndIsDeletedFalse(post.getId())).isPresent();

        post.delete();
        boardPostRepository.flush();

        assertThat(boardPostRepository.findByIdAndIsDeletedFalse(post.getId())).isEmpty();
    }
}
