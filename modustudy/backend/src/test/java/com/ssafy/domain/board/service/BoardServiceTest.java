package com.ssafy.domain.board.service;

import com.ssafy.common.exception.BusinessException;
import com.ssafy.domain.board.dto.request.BoardCommentCreateRequest;
import com.ssafy.domain.board.dto.request.BoardPostCreateRequest;
import com.ssafy.domain.board.dto.response.BoardCommentResponse;
import com.ssafy.domain.board.dto.response.BoardPostDetailResponse;
import com.ssafy.domain.board.dto.response.BoardRecruitingStudyResponse;
import com.ssafy.domain.board.entity.BoardComment;
import com.ssafy.domain.board.repository.BoardCommentRepository;
import com.ssafy.domain.board.repository.BoardPostRepository;
import com.ssafy.domain.notification.entity.Notification;
import com.ssafy.domain.notification.repository.NotificationRepository;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.MemberRole;
import com.ssafy.domain.study.entity.MemberStatus;
import com.ssafy.domain.study.entity.MeetingType;
import com.ssafy.domain.study.entity.Status;
import com.ssafy.domain.study.entity.Study;
import com.ssafy.domain.study.entity.StudyMember;
import com.ssafy.domain.study.entity.StudyType;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.StudyMemberRepository;
import com.ssafy.domain.study.repository.StudyRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import com.ssafy.domain.user.entity.Role;
import com.ssafy.domain.user.entity.User;
import com.ssafy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardPostRepository boardPostRepository;

    @Autowired
    private BoardCommentRepository boardCommentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private FormatRepository formatRepository;

    private User leader;
    private User member;
    private Study recruitingStudy;
    private Study pendingStudy;
    private Study scheduledStudy;
    private Study otherStudy;

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

        recruitingStudy = createStudy("Recruiting Study", topic, format, Status.RECRUITING);
        pendingStudy = createStudy("Pending Study", topic, format, Status.PENDING);
        scheduledStudy = createStudy("Scheduled Study", topic, format, Status.SCHEDULED);
        otherStudy = createStudy("Other Study", topic, format, Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("Recruiting list filters by status")
    void getRecruitingStudies_filtersStatuses() {
        List<Long> studyIds = boardService.getRecruitingStudies(leader.getId())
                .stream()
                .map(BoardRecruitingStudyResponse::getId)
                .toList();

        assertThat(studyIds).contains(recruitingStudy.getId(), pendingStudy.getId(), scheduledStudy.getId());
        assertThat(studyIds).doesNotContain(otherStudy.getId());
    }

    @Test
    @DisplayName("Add comment creates notification except for author")
    void addComment_createsNotification() {
        BoardPostDetailResponse created = boardService.createPost(
                leader.getId(),
                new BoardPostCreateRequest("Recruiting post title", "Recruiting post content", "backend", MeetingType.ONLINE, 6)
        );

        boardService.addComment(member.getId(), created.getId(), new BoardCommentCreateRequest(null, "Comment content"));

        boardPostRepository.flush();
        notificationRepository.flush();

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(leader.getId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent()).contains(created.getTitle());
        assertThat(boardPostRepository.findById(created.getId()).orElseThrow().getCommentCount()).isEqualTo(1);

        boardService.addComment(leader.getId(), created.getId(), new BoardCommentCreateRequest(null, "Owner comment"));
        notificationRepository.flush();

        List<Notification> afterOwnerComment = notificationRepository.findByUserIdOrderByCreatedAtDesc(leader.getId());
        assertThat(afterOwnerComment).hasSize(1);
    }

    @Test
    @DisplayName("Delete comment marks deleted and decreases count")
    void deleteComment_marksDeleted_andDecreasesCount() {
        BoardPostDetailResponse created = boardService.createPost(
                leader.getId(),
                new BoardPostCreateRequest("Recruiting post title", "Recruiting post content", "backend", MeetingType.ONLINE, 6)
        );

        BoardCommentResponse comment = boardService.addComment(
                member.getId(),
                created.getId(),
                new BoardCommentCreateRequest(null, "Comment content")
        );

        boardService.deleteComment(member.getId(), comment.getId());
        boardPostRepository.flush();
        boardCommentRepository.flush();

        BoardComment deleted = boardCommentRepository.findById(comment.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(boardPostRepository.findById(created.getId()).orElseThrow().getCommentCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Delete comment requires author")
    void deleteComment_requiresAuthor() {
        BoardPostDetailResponse created = boardService.createPost(
                leader.getId(),
                new BoardPostCreateRequest("Recruiting post title", "Recruiting post content", "backend", MeetingType.ONLINE, 6)
        );

        BoardCommentResponse comment = boardService.addComment(
                member.getId(),
                created.getId(),
                new BoardCommentCreateRequest(null, "Comment content")
        );

        assertThatThrownBy(() -> boardService.deleteComment(leader.getId(), comment.getId()))
                .isInstanceOf(BusinessException.class);
    }

    private Study createStudy(String name, Topic topic, Format format, Status status) {
        Study study = studyRepository.save(Study.builder()
                .leaderId(leader.getId())
                .topic(topic)
                .format(format)
                .name(name)
                .description("Test study description")
                .maxMembers(6)
                .studyType(StudyType.PLANNED)
                .status(status)
                .recruitStartDate(LocalDate.now().minusDays(7))
                .recruitEndDate(LocalDate.now().plusDays(7))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        studyRepository.flush();

        studyMemberRepository.save(StudyMember.builder()
                .studyId(study.getId())
                .userId(leader.getId())
                .role(MemberRole.LEADER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .build());
        studyMemberRepository.save(StudyMember.builder()
                .studyId(study.getId())
                .userId(member.getId())
                .role(MemberRole.MEMBER)
                .status(MemberStatus.APPROVED)
                .isProbation(false)
                .build());
        studyMemberRepository.flush();

        return study;
    }
}
