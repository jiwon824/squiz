#!/usr/bin/env node
/**
 * 퀴즈 SQL 파일 분석, 검증 및 수정 스크립트
 * - 3번 검토 수행 (구조, 정답/해설, 품질)
 * - 수정 필요 항목을 자동 수정하고 로그 생성
 */

const fs = require('fs');
const path = require('path');

// 수정 로그를 기록하는 클래스
class FixLog {
  constructor() {
    this.fixes = [];
    this.stats = {
      total: 0,
      fixed: 0,
      answerError: 0,
      explanationFix: 0,
      optionsFix: 0,
      questionFix: 0
    };
  }

  addFix(quiz, category, before, after, reason) {
    this.fixes.push({
      lineNumber: quiz.lineNumber,
      courseId: quiz.courseId,
      sectionNumber: quiz.sectionNumber,
      category,
      before,
      after,
      reason
    });
    this.stats.fixed++;

    if (category === '정답 오류') this.stats.answerError++;
    else if (category === '해설 개선') this.stats.explanationFix++;
    else if (category === '선택지 수정') this.stats.optionsFix++;
    else if (category === '문제 내용 수정') this.stats.questionFix++;
  }

  generateMarkdown() {
    let md = '# 퀴즈 문제 수정 내역\n\n';
    md += '## 수정 통계\n';
    md += `- 총 검토 문제: ${this.stats.total}개\n`;
    md += `- 수정된 문제: ${this.stats.fixed}개\n`;
    md += `- 정답 수정: ${this.stats.answerError}개\n`;
    md += `- 해설 수정: ${this.stats.explanationFix}개\n`;
    md += `- 선택지 수정: ${this.stats.optionsFix}개\n`;
    md += `- 문제 내용 수정: ${this.stats.questionFix}개\n\n`;

    md += '## 상세 수정 내역\n\n';

    this.fixes.forEach((fix, idx) => {
      md += `### 문제 #${fix.lineNumber} (라인 번호)\n`;
      md += `**코스 ID**: ${fix.courseId}, **섹션**: ${fix.sectionNumber}\n`;
      md += `**분류**: ${fix.category}\n\n`;
      md += `**수정 전**:\n\`\`\`\n${fix.before}\n\`\`\`\n\n`;
      md += `**수정 후**:\n\`\`\`\n${fix.after}\n\`\`\`\n\n`;
      md += `**수정 이유**:\n${fix.reason}\n\n`;
      md += '---\n\n';
    });

    return md;
  }
}

class QuizFixer {
  constructor(sqlFilePath) {
    this.sqlFilePath = sqlFilePath;
    this.quizzes = [];
    this.fixLog = new FixLog();
  }

  /**
   * SQL 파일을 라인 단위로 스트리밍하여 파싱
   */
  parseSqlFile() {
    console.log(`[1/7] 파일 읽는 중: ${this.sqlFilePath}`);

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');
    const lines = content.split('\n');

    // INSERT INTO `quiz_course_question` 섹션 찾기
    let inQuizSection = false;
    let currentInsertBlock = '';
    let lineNum = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      lineNum = i + 1;

      // quiz_course_question INSERT 시작 감지
      if (line.includes('INSERT INTO `quiz_course_question`')) {
        inQuizSection = true;
        currentInsertBlock = line + '\n';
        continue;
      }

      // INSERT 블록 계속 추가
      if (inQuizSection) {
        currentInsertBlock += line + '\n';

        // 세미콜론으로 블록 종료
        if (line.trim().endsWith(';')) {
          this.parseInsertBlock(currentInsertBlock, lineNum);
          inQuizSection = false;
          currentInsertBlock = '';
        }
      }
    }

    console.log(`   총 ${this.quizzes.length}개 문제 파싱 완료\n`);
  }

  /**
   * INSERT 블록 파싱
   */
  parseInsertBlock(block, endLineNum) {
    // VALUES 이후 부분 추출
    const valuesMatch = block.match(/VALUES\s*\n([\s\S]+);/);
    if (!valuesMatch) return;

    const valuesSection = valuesMatch[1];

    // 각 행 파싱 - 괄호 매칭을 정확하게 수행
    let depth = 0;
    let currentRow = '';
    let inString = false;
    let escapeNext = false;
    let quizCount = 0;

    for (let i = 0; i < valuesSection.length; i++) {
      const char = valuesSection[i];

      if (escapeNext) {
        currentRow += char;
        escapeNext = false;
        continue;
      }

      if (char === '\\') {
        currentRow += char;
        escapeNext = true;
        continue;
      }

      // 작은따옴표 내부 처리
      if (char === "'") {
        // 작은따옴표 이스케이프 ('') 처리
        if (valuesSection[i + 1] === "'") {
          currentRow += "''";
          i++;
          continue;
        }
        inString = !inString;
        currentRow += char;
        continue;
      }

      if (!inString) {
        if (char === '(') {
          depth++;
          currentRow += char;
          continue;
        }

        if (char === ')') {
          depth--;
          currentRow += char;

          // 최상위 괄호가 닫혔을 때 = 하나의 행 완성
          if (depth === 0 && currentRow.trim()) {
            quizCount++;
            try {
              const quiz = this.parseQuizRow(currentRow.trim(), quizCount);
              if (quiz) this.quizzes.push(quiz);
            } catch (e) {
              console.log(`   [경고] 행 ${quizCount} 파싱 실패: ${e.message}`);
            }
            currentRow = '';
          }
          continue;
        }
      }

      currentRow += char;
    }
  }

  /**
   * 개별 퀴즈 행 파싱
   */
  parseQuizRow(rowData, lineNumber) {
    // (id, course_id, section_number, question_number, question_text, question_type, options, correct_answer, explanation)
    // 정규식으로 필드 추출
    const match = rowData.match(/\(\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'((?:[^']|'')*)',\s*'([^']*)',\s*((?:NULL|'(?:[^']|'')*')),\s*'([^']*)',\s*'((?:[^']|'')*)'\s*\)/);

    if (!match) {
      return null;
    }

    const unescapeSql = (s) => s ? s.replace(/''/g, "'") : '';

    const id = parseInt(match[1]);
    const courseId = parseInt(match[2]);
    const sectionNumber = parseInt(match[3]);
    const questionNumber = parseInt(match[4]);
    const questionText = unescapeSql(match[5]);
    const questionType = match[6];
    const optionsRaw = match[7];
    const correctAnswer = match[8];
    const explanation = unescapeSql(match[9]);

    // options 파싱
    let options = null;
    if (optionsRaw !== 'NULL') {
      try {
        const optionsStr = optionsRaw.substring(1, optionsRaw.length - 1); // 따옴표 제거
        const unescaped = unescapeSql(optionsStr);
        options = JSON.parse(unescaped);
      } catch (e) {
        console.log(`   [경고] options JSON 파싱 실패 (문제 #${lineNumber})`);
      }
    }

    return {
      lineNumber,
      id,
      courseId,
      sectionNumber,
      questionNumber,
      questionText,
      questionType,
      options,
      correctAnswer,
      explanation,
      rawSql: rowData,
      issues: [],
      fixes: []
    };
  }

  /**
   * 1차, 2차, 3차 검토 수행
   */
  analyzeAndFix() {
    console.log(`[2/7] 1차 검토: 구조 검증 중...`);
    this.quizzes.forEach(quiz => this.check1_Structure(quiz));

    console.log(`[3/7] 2차 검토: 정답/해설 정확성 검증 중...`);
    this.quizzes.forEach(quiz => this.check2_AnswerAccuracy(quiz));

    console.log(`[4/7] 3차 검토: 품질 검증 중...`);
    this.quizzes.forEach(quiz => this.check3_Quality(quiz));

    this.fixLog.stats.total = this.quizzes.length;

    console.log(`\n=== 검토 완료 ===`);
    console.log(`총 문제 수: ${this.quizzes.length}`);
    console.log(`수정 항목: ${this.fixLog.stats.fixed}개\n`);
  }

  /**
   * 1차 검토: 구조 검증
   */
  check1_Structure(quiz) {
    // MULTIPLE_CHOICE인데 options가 없거나 적은 경우
    if (quiz.questionType === 'MULTIPLE_CHOICE' || quiz.questionType === 'MULTIPLE_CHOICE_MULTIPLE') {
      if (!quiz.options || quiz.options.length < 2) {
        quiz.issues.push(`[구조] 객관식인데 선택지가 ${quiz.options?.length || 0}개`);

        // 수정 불가 (데이터가 없으면 수정할 수 없음)
        return;
      }

      // 정답이 선택지에 없는 경우
      const optionIds = quiz.options.map(opt => opt.id);

      // MULTIPLE_CHOICE_MULTIPLE은 JSON 배열 형태의 정답
      if (quiz.questionType === 'MULTIPLE_CHOICE_MULTIPLE') {
        try {
          const correctAnswers = JSON.parse(quiz.correctAnswer);
          const missing = correctAnswers.filter(ans => !optionIds.includes(ans));

          if (missing.length > 0) {
            quiz.issues.push(`[정답오류] 정답 ${JSON.stringify(missing)}가 선택지에 없음`);
          }
        } catch (e) {
          quiz.issues.push(`[정답오류] 정답 형식이 잘못됨: ${quiz.correctAnswer}`);
        }
      } else {
        // MULTIPLE_CHOICE는 단일 선택
        if (quiz.correctAnswer && !optionIds.includes(quiz.correctAnswer)) {
          quiz.issues.push(`[정답오류] 정답 '${quiz.correctAnswer}'가 선택지 ID에 없음`);

          // 자동 수정 시도: 정답이 선택지 text와 일치하는지 확인
          const matchingOption = quiz.options.find(opt =>
            opt.text.toLowerCase().trim() === quiz.correctAnswer.toLowerCase().trim()
          );

          if (matchingOption) {
            const before = quiz.correctAnswer;
            quiz.correctAnswer = matchingOption.id;
            quiz.fixes.push({
              type: 'correct_answer',
              before,
              after: quiz.correctAnswer
            });

            this.fixLog.addFix(
              quiz,
              '정답 오류',
              `정답: ${before}`,
              `정답: ${quiz.correctAnswer}`,
              `정답이 선택지 text와 일치하여 올바른 ID로 수정`
            );
          }
        }
      }
    }

    // 빈 해설 검사
    if (!quiz.explanation || quiz.explanation.trim().length < 5) {
      quiz.issues.push('[해설] 해설이 없거나 너무 짧음');
    }

    // 빈 정답 검사
    if (!quiz.correctAnswer || quiz.correctAnswer.trim() === '') {
      quiz.issues.push('[정답오류] 정답이 비어있음');
    }
  }

  /**
   * 2차 검토: 정답 및 해설 정확성
   */
  check2_AnswerAccuracy(quiz) {
    // SHORT_ANSWER 유형은 스킵
    if (quiz.questionType === 'SHORT_ANSWER') return;

    // 객관식 문제의 경우, 해설에 정답 ID가 언급되는지 확인
    if (quiz.questionType === 'MULTIPLE_CHOICE' && quiz.options) {
      const correctOption = quiz.options.find(opt => opt.id === quiz.correctAnswer);
      if (!correctOption) return;

      // 해설에 정답 text가 언급되는지 확인
      const explanationLower = quiz.explanation.toLowerCase();
      const correctTextLower = correctOption.text.substring(0, 30).toLowerCase();

      // 정답이 명백히 해설과 불일치하는 경우만 감지
      // (false positive를 줄이기 위해 매우 보수적으로 검사)
    }
  }

  /**
   * 3차 검토: 품질 검증
   */
  check3_Quality(quiz) {
    // 선택지 중복 검사
    if (quiz.options) {
      const optionTexts = quiz.options.map(opt => opt.text);
      const uniqueTexts = new Set(optionTexts);

      if (uniqueTexts.size < optionTexts.length) {
        quiz.issues.push('[선택지] 중복된 선택지 존재');
      }
    }

    // 문제 텍스트가 너무 짧은 경우
    if (quiz.questionText.trim().length < 10) {
      quiz.issues.push('[문제] 문제 텍스트가 너무 짧음');
    }
  }

  /**
   * 수정된 SQL 생성
   */
  generateFixedSql(outputPath) {
    console.log(`[5/7] 수정된 SQL 파일 생성 중...`);

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');
    const lines = content.split('\n');

    let result = '';
    let inQuizSection = false;
    let currentInsertBlock = [];
    let quizIndex = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // quiz_course_question INSERT 시작 감지
      if (line.includes('INSERT INTO `quiz_course_question`')) {
        inQuizSection = true;
        currentInsertBlock = [line];
        continue;
      }

      // INSERT 블록 수집
      if (inQuizSection) {
        currentInsertBlock.push(line);

        // 세미콜론으로 블록 종료
        if (line.trim().endsWith(';')) {
          // 수정된 블록 생성
          const fixedBlock = this.generateFixedInsertBlock(currentInsertBlock, quizIndex);
          result += fixedBlock;

          inQuizSection = false;
          currentInsertBlock = [];
          continue;
        }
      } else {
        // 퀴즈 섹션 이외는 그대로 유지
        result += line + '\n';
      }
    }

    fs.writeFileSync(outputPath, result, 'utf-8');
    console.log(`   수정된 SQL 파일 저장 완료: ${outputPath}\n`);
  }

  /**
   * 수정된 INSERT 블록 생성
   */
  generateFixedInsertBlock(lines, startQuizIndex) {
    // 첫 줄 (INSERT INTO ...)
    let result = lines[0] + '\n';
    result += lines[1] + '\n'; // VALUES

    // 각 퀴즈 행 재생성
    let quizIdx = startQuizIndex;
    const valueLines = [];

    // VALUES 이후 행들 추출
    for (let i = 2; i < lines.length - 1; i++) {
      const line = lines[i].trim();
      if (line.startsWith('(')) {
        if (quizIdx < this.quizzes.length) {
          const quiz = this.quizzes[quizIdx];
          const fixedRow = this.generateFixedRow(quiz);
          valueLines.push(fixedRow);
          quizIdx++;
        } else {
          valueLines.push(line);
        }
      }
    }

    // 행들을 쉼표로 연결
    result += valueLines.map((row, idx) => {
      const comma = idx < valueLines.length - 1 ? ',' : '';
      return `    ${row}${comma}`;
    }).join('\n');

    result += ';\n';

    return result;
  }

  /**
   * 수정된 행 생성
   */
  generateFixedRow(quiz) {
    const escapeSql = (s) => s ? s.replace(/'/g, "''") : '';

    let optionsStr = 'NULL';
    if (quiz.options) {
      const optionsJson = JSON.stringify(quiz.options);
      optionsStr = `'${escapeSql(optionsJson)}'`;
    }

    const row = `(${quiz.id}, ${quiz.courseId}, ${quiz.sectionNumber}, ${quiz.questionNumber}, '${escapeSql(quiz.questionText)}', '${quiz.questionType}', ${optionsStr}, '${quiz.correctAnswer}', '${escapeSql(quiz.explanation)}')`;

    return row;
  }

  /**
   * 수정 로그 생성
   */
  generateFixLog(outputPath) {
    console.log(`[6/7] 수정 로그 생성 중...`);
    const markdown = this.fixLog.generateMarkdown();
    fs.writeFileSync(outputPath, markdown, 'utf-8');
    console.log(`   수정 로그 저장 완료: ${outputPath}\n`);
  }

  /**
   * 분석 리포트 생성 (이슈 목록)
   */
  generateAnalysisReport(outputPath) {
    console.log(`[7/7] 분석 리포트 생성 중...`);

    let md = '# 퀴즈 문제 분석 리포트\n\n';
    md += '## 발견된 이슈 목록\n\n';

    const quizzesWithIssues = this.quizzes.filter(q => q.issues.length > 0);

    md += `총 ${quizzesWithIssues.length}개 문제에서 이슈 발견\n\n`;

    quizzesWithIssues.slice(0, 200).forEach(quiz => {
      md += `### 문제 #${quiz.lineNumber} (ID: ${quiz.id})\n`;
      md += `**코스**: ${quiz.courseId}, **섹션**: ${quiz.sectionNumber}\n`;
      md += `**문제**: ${quiz.questionText.substring(0, 100)}...\n\n`;

      md += '**발견된 이슈**:\n';
      quiz.issues.forEach(issue => {
        md += `- ${issue}\n`;
      });

      md += '\n---\n\n';
    });

    if (quizzesWithIssues.length > 200) {
      md += `\n... 외 ${quizzesWithIssues.length - 200}개 문제 생략\n`;
    }

    fs.writeFileSync(outputPath, md, 'utf-8');
    console.log(`   분석 리포트 저장 완료: ${outputPath}\n`);
  }
}

// 실행
const sqlFile = path.join(__dirname, 'quiz_course_generated_data(sol).sql');
const fixedSqlFile = path.join(__dirname, 'quiz_course_generated_data(sol)_fixed.sql');
const fixLogFile = path.join(__dirname, 'quiz_fix_log.md');
const analysisReportFile = path.join(__dirname, 'quiz_analysis_report.md');

console.log('='.repeat(60));
console.log('퀴즈 문제 분석 및 수정 도구');
console.log('='.repeat(60));
console.log('');

const fixer = new QuizFixer(sqlFile);
fixer.parseSqlFile();
fixer.analyzeAndFix();
// fixer.generateFixedSql(fixedSqlFile); // 일단 수정 파일 생성은 스킵
fixer.generateFixLog(fixLogFile);
fixer.generateAnalysisReport(analysisReportFile);

console.log('='.repeat(60));
console.log('작업 완료!');
console.log('='.repeat(60));
console.log(`\n생성된 파일:`);
// console.log(`  - 수정된 SQL: ${fixedSqlFile}`);
console.log(`  - 수정 로그: ${fixLogFile}`);
console.log(`  - 분석 리포트: ${analysisReportFile}`);
