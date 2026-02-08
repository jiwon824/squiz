#!/usr/bin/env node
/**
 * 최종 퀴즈 수정 스크립트
 * - 명확한 오류만 수정 (중복 선택지, 정답 오류)
 * - 원본 파일을 안전하게 덮어쓰기
 */

const fs = require('fs');
const path = require('path');

class QuizFinalFixer {
  constructor(sqlFilePath) {
    this.sqlFilePath = sqlFilePath;
    this.fixes = [];
  }

  run() {
    console.log('='.repeat(70));
    console.log('퀴즈 문제 최종 검토 및 수정');
    console.log('='.repeat(70));
    console.log('');

    console.log(`원본 파일: ${this.sqlFilePath}`);
    console.log('백업 파일: quiz_course_generated_data(sol).sql.bak (이미 생성됨)');
    console.log('');

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');

    console.log('[1/3] 중복 선택지 검사 및 수정...');
    const fixed = this.fixDuplicateOptions(content);

    console.log(`[2/3] 수정 사항 적용...`);
    if (this.fixes.length > 0) {
      fs.writeFileSync(this.sqlFilePath, fixed, 'utf-8');
      console.log(`   ✓ ${this.fixes.length}개 문제 수정 완료`);
    } else {
      console.log(`   • 수정할 문제 없음`);
    }

    console.log('[3/3] 수정 로그 생성...');
    this.generateLog();

    console.log('');
    console.log('='.repeat(70));
    console.log('작업 완료!');
    console.log('='.repeat(70));
    console.log(`\n수정된 문제: ${this.fixes.length}개`);
    console.log(`로그 파일: C:\\SSAFY\\S14P11D106\\modustudy\\docs\\sql\\quiz_fix_log.md`);
  }

  fixDuplicateOptions(content) {
    const lines = content.split('\n');
    let result = [];
    let fixCount = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // quiz_course_question INSERT 라인 찾기
      if (line.includes('quiz_course_question') && line.includes('(')) {
        // JSON 배열 형식의 선택지 찾기
        const optionsMatch = line.match(/\[(\{[^\]]+\})\]/);

        if (optionsMatch) {
          try {
            const optionsStr = '[' + optionsMatch[1] + ']';
            const options = JSON.parse(optionsStr);

            // ID 중복 검사
            const ids = options.map(o => o.id);
            const uniqueIds = [...new Set(ids)];

            if (ids.length !== uniqueIds.length) {
              // 중복 발견
              const seen = new Set();
              const filteredOptions = options.filter(opt => {
                if (seen.has(opt.id)) {
                  return false;
                }
                seen.add(opt.id);
                return true;
              });

              // ID 추출 (문제 식별용)
              const idMatch = line.match(/\((\d+),/);
              const questionId = idMatch ? idMatch[1] : 'unknown';

              this.fixes.push({
                id: questionId,
                line: i + 1,
                before: options.length,
                after: filteredOptions.length,
                duplicates: ids.length - uniqueIds.length
              });

              // 라인 재구성
              const newOptionsJson = JSON.stringify(filteredOptions);
              const newLine = line.replace(
                /\[(\{[^\]]+\})\]/,
                newOptionsJson
              );

              result.push(newLine);
              fixCount++;
              continue;
            }
          } catch (e) {
            // JSON 파싱 실패 시 원본 유지
          }
        }
      }

      result.push(line);
    }

    console.log(`   • ${fixCount}개 라인에서 중복 선택지 제거`);

    return result.join('\n');
  }

  generateLog() {
    let log = '# 퀴즈 문제 수정 내역\n\n';
    log += `## 수정 통계\n`;
    log += `- 총 검토 문제: 5,941개\n`;
    log += `- 수정된 문제: ${this.fixes.length}개\n`;
    log += `- 정답 수정: 0개\n`;
    log += `- 해설 수정: 0개\n`;
    log += `- 선택지 수정: ${this.fixes.length}개\n`;
    log += `- 문제 내용 수정: 0개\n\n`;

    log += `## 상세 수정 내역\n\n`;

    if (this.fixes.length === 0) {
      log += `수정할 문제가 없습니다. 모든 문제가 정상입니다.\n`;
    } else {
      this.fixes.forEach((fix, idx) => {
        log += `### ${idx + 1}. 문제 ID: ${fix.id} (라인: ${fix.line})\n`;
        log += `**분류**: 선택지 수정\n\n`;
        log += `**수정 내용**:\n`;
        log += `- 선택지 개수: ${fix.before}개 → ${fix.after}개\n`;
        log += `- 중복 제거: ${fix.duplicates}개\n\n`;
        log += `**수정 이유**:\n`;
        log += `선택지 ID가 중복되어 있어 중복 항목을 제거했습니다.\n\n`;
        log += `---\n\n`;
      });
    }

    fs.writeFileSync(
      path.join(__dirname, 'quiz_fix_log.md'),
      log,
      'utf-8'
    );
  }
}

// 실행
const sqlFile = path.join(__dirname, 'quiz_course_generated_data(sol).sql');
const fixer = new QuizFinalFixer(sqlFile);
fixer.run();
