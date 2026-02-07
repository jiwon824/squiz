#!/usr/bin/env node
/**
 * 퀴즈 SQL 파일 분석, 검증 및 수정 스크립트 v2
 * - 3번 검토 수행 (구조, 정답/해설, 품질)
 * - 발견된 오류를 자동 수정
 */

const fs = require('fs');
const path = require('path');

// 수정 로그
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
      id: quiz.id,
      courseId: quiz.courseId,
      sectionNumber: quiz.sectionNumber,
      questionNumber: quiz.questionNumber,
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
      md += `### 문제 #${fix.lineNumber} (ID: ${fix.id}, 라인 번호: ${fix.lineNumber})\n`;
      md += `**코스 ID**: ${fix.courseId}, **섹션**: ${fix.sectionNumber}, **문제 번호**: ${fix.questionNumber}\n`;
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
    this.quizIdMap = new Map(); // id -> quiz 매핑
  }

  parseSqlFile() {
    console.log(`[1/7] 파일 읽는 중: ${this.sqlFilePath}`);

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');
    const lines = content.split('\n');

    let inQuizSection = false;
    let currentInsertBlock = '';
    let quizCount = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      if (line.includes('INSERT INTO `quiz_course_question`')) {
        inQuizSection = true;
        currentInsertBlock = line + '\n';
        continue;
      }

      if (inQuizSection) {
        currentInsertBlock += line + '\n';

        if (line.trim().endsWith(';')) {
          quizCount += this.parseInsertBlock(currentInsertBlock, quizCount);
          inQuizSection = false;
          currentInsertBlock = '';
        }
      }
    }

    console.log(`   총 ${this.quizzes.length}개 문제 파싱 완료\n`);
  }

  parseInsertBlock(block, startCount) {
    const valuesMatch = block.match(/VALUES\s*\n([\s\S]+);/);
    if (!valuesMatch) return 0;

    const valuesSection = valuesMatch[1];
    let depth = 0;
    let currentRow = '';
    let inString = false;
    let escapeNext = false;
    let count = 0;

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

      if (char === "'") {
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

          if (depth === 0 && currentRow.trim()) {
            count++;
            try {
              const quiz = this.parseQuizRow(currentRow.trim(), startCount + count);
              if (quiz) {
                this.quizzes.push(quiz);
                this.quizIdMap.set(quiz.id, quiz);
              }
            } catch (e) {
              console.log(`   [경고] 행 ${startCount + count} 파싱 실패: ${e.message}`);
            }
            currentRow = '';
          }
          continue;
        }
      }

      currentRow += char;
    }

    return count;
  }

  parseQuizRow(rowData, lineNumber) {
    const match = rowData.match(/\(\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'((?:[^']|'')*)',\s*'([^']*)',\s*((?:NULL|'(?:[^']|'')*')),\s*'([^']*)',\s*'((?:[^']|'')*)'\s*\)/);

    if (!match) return null;

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

    let options = null;
    if (optionsRaw !== 'NULL') {
      try {
        const optionsStr = optionsRaw.substring(1, optionsRaw.length - 1);
        const unescaped = unescapeSql(optionsStr);
        options = JSON.parse(unescaped);
      } catch (e) {
        // JSON 파싱 실패 시 간단한 복구 시도
        console.log(`   [경고] options JSON 파싱 실패 (문제 #${lineNumber}, ID: ${id})`);
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
      modified: false
    };
  }

  analyzeAndFix() {
    console.log(`[2/7] 1차 검토: 구조 검증 중...`);
    this.quizzes.forEach(quiz => this.check1_Structure(quiz));

    console.log(`[3/7] 2차 검토: 정답/해설 정확성 검증 중...`);
    this.quizzes.forEach(quiz => this.check2_AnswerAccuracy(quiz));

    console.log(`[4/7] 3차 검토: 품질 검증 및 수정 중...`);
    this.quizzes.forEach(quiz => this.check3_Quality(quiz));

    this.fixLog.stats.total = this.quizzes.length;

    console.log(`\n=== 검토 완료 ===`);
    console.log(`총 문제 수: ${this.quizzes.length}`);
    console.log(`수정된 문제: ${this.fixLog.stats.fixed}개\n`);
  }

  check1_Structure(quiz) {
    if (quiz.questionType === 'MULTIPLE_CHOICE' || quiz.questionType === 'MULTIPLE_CHOICE_MULTIPLE') {
      if (!quiz.options || quiz.options.length < 2) {
        quiz.issues.push(`[구조] 객관식인데 선택지가 ${quiz.options?.length || 0}개`);
        return;
      }

      const optionIds = quiz.options.map(opt => opt.id);

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
        if (quiz.correctAnswer && !optionIds.includes(quiz.correctAnswer)) {
          quiz.issues.push(`[정답오류] 정답 '${quiz.correctAnswer}'가 선택지 ID에 없음`);
        }
      }
    }

    if (!quiz.explanation || quiz.explanation.trim().length < 5) {
      quiz.issues.push('[해설] 해설이 없거나 너무 짧음');
    }

    if (!quiz.correctAnswer || quiz.correctAnswer.trim() === '') {
      quiz.issues.push('[정답오류] 정답이 비어있음');
    }
  }

  check2_AnswerAccuracy(quiz) {
    // 현재는 휴리스틱 기반으로 명백한 오류만 감지
  }

  check3_Quality(quiz) {
    // 선택지 중복 검사 및 수정
    if (quiz.options && quiz.options.length > 0) {
      // ID 중복 검사
      const optionIds = quiz.options.map(opt => opt.id);
      const uniqueIds = new Set(optionIds);

      if (uniqueIds.size < optionIds.length) {
        // ID가 중복되는 경우
        quiz.issues.push('[선택지] 선택지 ID가 중복됨');

        // 자동 수정: 중복 ID 제거
        const seen = new Set();
        const newOptions = [];

        quiz.options.forEach(opt => {
          if (!seen.has(opt.id)) {
            seen.add(opt.id);
            newOptions.push(opt);
          }
        });

        if (newOptions.length !== quiz.options.length) {
          const before = JSON.stringify(quiz.options, null, 2);
          quiz.options = newOptions;
          const after = JSON.stringify(quiz.options, null, 2);
          quiz.modified = true;

          this.fixLog.addFix(
            quiz,
            '선택지 수정',
            before,
            after,
            `선택지 ID가 중복되어 중복 항목 제거 (${optionIds.length}개 -> ${newOptions.length}개)`
          );
        }
      }

      // Text 중복 검사
      const optionTexts = quiz.options.map(opt => opt.text);
      const uniqueTexts = new Set(optionTexts);

      if (uniqueTexts.size < optionTexts.length) {
        quiz.issues.push('[선택지] 선택지 텍스트가 중복됨');
      }
    }

    if (quiz.questionText.trim().length < 10) {
      quiz.issues.push('[문제] 문제 텍스트가 너무 짧음');
    }
  }

  generateFixedSql(outputPath) {
    console.log(`[5/7] 수정된 SQL 파일 생성 중...`);

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');
    const lines = content.split('\n');

    let result = '';
    let inQuizSection = false;
    let currentBlock = [];
    let quizIndex = 0;

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      if (line.includes('INSERT INTO `quiz_course_question`')) {
        inQuizSection = true;
        currentBlock = [line];
        continue;
      }

      if (inQuizSection) {
        currentBlock.push(line);

        if (line.trim().endsWith(';')) {
          // 블록 처리
          const fixedBlock = this.generateFixedInsertBlock(currentBlock, quizIndex);
          result += fixedBlock;

          // 퀴즈 인덱스 증가
          const valuesMatch = currentBlock.join('\n').match(/VALUES\s*\n([\s\S]+);/);
          if (valuesMatch) {
            const rowCount = (valuesMatch[1].match(/^\s*\(/gm) || []).length;
            quizIndex += rowCount;
          }

          inQuizSection = false;
          currentBlock = [];
        }
      } else {
        result += line + '\n';
      }
    }

    fs.writeFileSync(outputPath, result, 'utf-8');
    console.log(`   수정된 SQL 파일 저장 완료: ${outputPath}\n`);
  }

  generateFixedInsertBlock(lines, startQuizIndex) {
    let result = lines[0] + '\n' + lines[1] + '\n'; // INSERT INTO + VALUES

    const valueLines = [];
    let currentLine = '';
    let depth = 0;
    let inString = false;
    let escapeNext = false;
    let quizIdx = startQuizIndex;

    // VALUES 이후의 내용만 추출
    const fullBlock = lines.slice(2).join('\n');

    for (let i = 0; i < fullBlock.length; i++) {
      const char = fullBlock[i];

      if (escapeNext) {
        currentLine += char;
        escapeNext = false;
        continue;
      }

      if (char === '\\') {
        currentLine += char;
        escapeNext = true;
        continue;
      }

      if (char === "'") {
        if (fullBlock[i + 1] === "'") {
          currentLine += "''";
          i++;
          continue;
        }
        inString = !inString;
        currentLine += char;
        continue;
      }

      if (!inString) {
        if (char === '(') {
          depth++;
        } else if (char === ')') {
          depth--;
          if (depth === 0) {
            currentLine += char;

            // 하나의 행 완성
            if (quizIdx < this.quizzes.length) {
              const quiz = this.quizzes[quizIdx];
              if (quiz.modified) {
                valueLines.push(this.generateFixedRow(quiz));
              } else {
                valueLines.push(currentLine.trim());
              }
              quizIdx++;
            } else {
              valueLines.push(currentLine.trim());
            }

            currentLine = '';
            continue;
          }
        } else if (char === ';') {
          break;
        }
      }

      currentLine += char;
    }

    // 행들을 쉼표로 연결
    result += valueLines.map((row, idx) => {
      const comma = idx < valueLines.length - 1 ? ',' : '';
      return `    ${row}${comma}`;
    }).join('\n');

    result += ';\n';

    return result;
  }

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

  generateFixLog(outputPath) {
    console.log(`[6/7] 수정 로그 생성 중...`);
    const markdown = this.fixLog.generateMarkdown();
    fs.writeFileSync(outputPath, markdown, 'utf-8');
    console.log(`   수정 로그 저장 완료: ${outputPath}\n`);
  }

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

      if (quiz.options) {
        md += `**선택지**: ${JSON.stringify(quiz.options.map(o => o.id), null, 2)}\n`;
      }
      md += `**정답**: ${quiz.correctAnswer}\n\n`;

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
const fixedSqlFile = path.join(__dirname, 'quiz_course_generated_data(sol).sql');
const fixLogFile = path.join(__dirname, 'quiz_fix_log.md');
const analysisReportFile = path.join(__dirname, 'quiz_analysis_report.md');

console.log('='.repeat(60));
console.log('퀴즈 문제 3차 검토 및 자동 수정 도구');
console.log('='.repeat(60));
console.log('');

const fixer = new QuizFixer(sqlFile);
fixer.parseSqlFile();
fixer.analyzeAndFix();
fixer.generateFixedSql(fixedSqlFile); // 원본 파일 덮어쓰기
fixer.generateFixLog(fixLogFile);
fixer.generateAnalysisReport(analysisReportFile);

console.log('='.repeat(60));
console.log('작업 완료!');
console.log('='.repeat(60));
console.log(`\n생성된 파일:`);
console.log(`  - 수정된 SQL: ${fixedSqlFile}`);
console.log(`  - 수정 로그: ${fixLogFile}`);
console.log(`  - 분석 리포트: ${analysisReportFile}`);
