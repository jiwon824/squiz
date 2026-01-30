-- ============================================
-- MIXED 오디오 녹음 데이터 (전체 음성)
-- insert_test_data.sql 실행 후 추가 실행
-- ============================================

-- 7. 미팅 오디오 녹음 (MIXED 트랙 - 전체 음성)
INSERT INTO `meeting_audio_recording` (`meeting_id`, `user_id`, `track_type`, `recording_url`, `format`, `created_at`) VALUES
  (101, NULL, 'MIXED', '/uploads/meetings/101/audio/meeting_101_mixed.mp3', 'mp3', NOW()),
  (102, NULL, 'MIXED', '/uploads/meetings/102/audio/meeting_102_mixed.mp3', 'mp3', NOW()),
  (103, NULL, 'MIXED', '/uploads/meetings/103/audio/meeting_103_mixed.mp3', 'mp3', NOW()),
  (104, NULL, 'MIXED', '/uploads/meetings/104/audio/meeting_104_mixed.mp3', 'mp3', NOW()),
  (105, NULL, 'MIXED', '/uploads/meetings/105/audio/meeting_105_mixed.mp3', 'mp3', NOW()),
  (106, NULL, 'MIXED', '/uploads/meetings/106/audio/meeting_106_mixed.mp3', 'mp3', NOW()),
  (107, NULL, 'MIXED', '/uploads/meetings/107/audio/meeting_107_mixed.mp3', 'mp3', NOW()),
  (108, NULL, 'MIXED', '/uploads/meetings/108/audio/meeting_108_mixed.mp3', 'mp3', NOW()),
  (109, NULL, 'MIXED', '/uploads/meetings/109/audio/meeting_109_mixed.mp3', 'mp3', NOW()),
  (110, NULL, 'MIXED', '/uploads/meetings/110/audio/meeting_110_mixed.mp3', 'mp3', NOW());

-- STT 상태를 DONE으로 변경 (이미 transcript가 있으므로)
UPDATE `meeting` SET `stt_status` = 'DONE' WHERE `id` BETWEEN 101 AND 110;
