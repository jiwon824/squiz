#!/usr/bin/env node
/**
 * 퀴즈 SQL 파일 분석 및 검증 스크립트
 */

const fs = require('fs');
const path = require('path');

class Quiz {
  constructor(data) {
    this.lineNumber = data.lineNumber;
    this.courseId = data.courseId;
    this.sectionNumber = data.sectionNumber;
    this.questionText = data.questionText;
    this.questionType = data.questionType;
    this.options = data.options;
    this.correctAnswer = data.correctAnswer;
    this.answerKeywords = data.answerKeywords;
    this.explanation = data.explanation;
    this.difficultyLevel = data.difficultyLevel;
    this.rawSql = data.rawSql;
    this.issues = [];
    this.needsFix = false;
  }
}

class QuizAnalyzer {
  constructor(sqlFilePath) {
    this.sqlFilePath = sqlFilePath;
    this.quizzes = [];
    this.issuesByCategory = {
      answerError: [],
      explanationIssue: [],
      optionsIssue: [],
      questionIssue: []
    };
  }

  parseSqlFile() {
    console.log(`파일 읽는 중: ${this.sqlFilePath}`);

    const content = fs.readFileSync(this.sqlFilePath, 'utf-8');

    // INSERT INTO quiz_question 패턴 찾기
    const pattern = /INSERT INTO `quiz_question`[^;]+VALUES\s*\n((?:\s*\([^)]+\),?\s*\n?)+);/gms;

    let totalCount = 0;
    let match;

    while ((match = pattern.exec(content)) !== null) {
      const valuesBlock = match[1];

      // 각 행 파싱 (괄호로 묶인 부분)
      // 더 정교한 파싱을 위해 상태 기계 방식 사용
      const rows = this.parseValuesBlock(valuesBlock);

      for (const rowData of rows) {
        totalCount++;
        try {
          const quiz = this.parseQuizRow(rowData, totalCount);
          this.quizzes.push(quiz);
        } catch (e) {
          console.log(`  [경고] 라인 파싱 실패 (문제 #${totalCount}): ${e.message}`);
        }
      }
    }

    console.log(`총 ${this.quizzes.length}개 문제 파싱 완료`);
  }

  parseValuesBlock(valuesBlock) {
    const rows = [];
    let currentRow = '';
    let inString = false;
    let escapeNext = false;
    let parenDepth = 0;

    for (let i = 0; i < valuesBlock.length; i++) {
      const char = valuesBlock[i];

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

      if (char === "'" && !escapeNext) {
        // 작은따옴표 이스케이프 처리 ('')
        if (valuesBlock[i + 1] === "'") {
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
          parenDepth++;
        } else if (char === ')') {
          parenDepth--;
          if (parenDepth === 0) {
            currentRow += char;
            rows.push(currentRow.trim());
            currentRow = '';
            continue;
          }
        }
      }

      currentRow += char;
    }

    return rows;
  }

  parseQuizRow(rowData, lineNum) {
    // SQL에서 ''는 '로 이스케이프됨
    const unescapeSql = (s) => s.replace(/''/g, "'").replace(/\\n/g, "\n");

    // 정규식으로 필드 추출 (더 신중하게)
    const match = rowData.match(/\(\s*(\d+),\s*(\d+),\s*(\d+),\s*'((?:[^']|'')*)',\s*'([^']*)',\s*'(\[(?:[^\]]|\](?!'))*\])',\s*'([^']*)',\s*'(\[(?:[^\]]|\](?!'))*\])',\s*'((?:[^']|'')*)',\s*'([^']*)'\s*\)/);

    if (!match) {
      throw new Error('행 파싱 실패');
    }

    const courseId = parseInt(match[2]);
    const sectionNumber = parseInt(match[3]);
    const questionText = unescapeSql(match[4]);
    const questionType = match[5];

    let options = [];
    try {
      const optionsStr = match[6].replace(/''/g, "'");
      options = JSON.parse(optionsStr);
    } catch (e) {
      console.log(`  [경고] options JSON 파싱 실패 (문제 #${lineNum})`);
    }

    const correctAnswer = match[7];

    let answerKeywords = [];
    try {
      const keywordsStr = match[8].replace(/''/g, "'");
      answerKeywords = JSON.parse(keywordsStr);
    } catch (e) {
      console.log(`  [경고] answer_keywords JSON 파싱 실패 (문제 #${lineNum})`);
    }

    const explanation = unescapeSql(match[9]);
    const difficultyLevel = match[10];

    return new Quiz({
      lineNumber: lineNum,
      courseId,
      sectionNumber,
      questionText,
      questionType,
      options,
      correctAnswer,
      answerKeywords,
      explanation,
      difficultyLevel,
      rawSql: rowData
    });
  }

  analyzeQuizzes() {
    console.log('\n=== 퀴즈 분석 시작 ===');

    this.quizzes.forEach((quiz, idx) => {
      // 1차 검토: 구조 검증
      this.checkStructure(quiz, idx + 1);

      // 2차 검토: 정답 및 해설 정확성
      this.checkAnswerAccuracy(quiz, idx + 1);

      // 3차 검토: 선택지 및 문제 품질
      this.checkQuality(quiz, idx + 1);

      // 진행 상황 출력
      if ((idx + 1) % 1000 === 0) {
        console.log(`  분석 진행: ${idx + 1}/${this.quizzes.length} 문제`);
      }
    });

    console.log(`\n분석 완료: 총 ${this.quizzes.length}개 문제`);
    this.printSummary();
  }

  checkStructure(quiz, idx) {
    // MULTIPLE_CHOICE인데 options가 없는 경우
    if (quiz.questionType === 'MULTIPLE_CHOICE') {
      if (!quiz.options || quiz.options.length < 2) {
        quiz.issues.push(`[구조] 객관식인데 선택지가 ${quiz.options?.length || 0}개`);
        quiz.needsFix = true;
        this.issuesByCategory.answerError.push(quiz);
      }

      // 정답이 선택지에 없는 경우
      if (quiz.correctAnswer && !quiz.options.includes(quiz.correctAnswer)) {
        quiz.issues.push(`[정답오류] 정답 '${quiz.correctAnswer}'가 선택지에 없음`);
        quiz.needsFix = true;
        this.issuesByCategory.answerError.push(quiz);
      }
    }

    // 빈 해설
    if (!quiz.explanation || quiz.explanation.trim().length < 10) {
      quiz.issues.push('[해설] 해설이 너무 짧거나 없음');
      this.issuesByCategory.explanationIssue.push(quiz);
    }
  }

  checkAnswerAccuracy(quiz, idx) {
    // 해설에 "정답:"이 있는데 실제 정답과 다른 경우 감지
    if (quiz.explanation.includes('정답:') || quiz.explanation.includes('Answer:')) {
      const explanationLower = quiz.explanation.toLowerCase();
      const correctLower = quiz.correctAnswer.toLowerCase();

      if (correctLower && !explanationLower.includes(correctLower)) {
        if (quiz.questionType === 'MULTIPLE_CHOICE') {
          const answerIndex = quiz.options.findIndex(opt => opt.toLowerCase() === correctLower);
          if (answerIndex !== -1) {
            if (!quiz.explanation.includes(String(answerIndex + 1))) {
              quiz.issues.push(`[해설] 해설에 정답 '${quiz.correctAnswer}' 언급 없음`);
              this.issuesByCategory.explanationIssue.push(quiz);
            }
          }
        }
      }
    }

    // 정답이 비어있는 경우
    if (!quiz.correctAnswer || quiz.correctAnswer.trim() === '') {
      quiz.issues.push('[정답오류] 정답이 비어있음');
      quiz.needsFix = true;
      this.issuesByCategory.answerError.push(quiz);
    }
  }

  checkQuality(quiz, idx) {
    // 선택지 중복 검사
    if (quiz.questionType === 'MULTIPLE_CHOICE' && quiz.options) {
      const uniqueOptions = new Set(quiz.options);
      if (uniqueOptions.size < quiz.options.length) {
        quiz.issues.push('[선택지] 중복된 선택지 존재');
        this.issuesByCategory.optionsIssue.push(quiz);
      }
    }

    // 문제 텍스트가 너무 짧은 경우
    if (quiz.questionText.trim().length < 10) {
      quiz.issues.push('[문제] 문제 텍스트가 너무 짧음');
      this.issuesByCategory.questionIssue.push(quiz);
    }
  }

  printSummary() {
    const totalIssues = this.quizzes.filter(q => q.issues.length > 0).length;
    const needsFix = this.quizzes.filter(q => q.needsFix).length;

    console.log('\n=== 분석 요약 ===');
    console.log(`총 문제 수: ${this.quizzes.length}`);
    console.log(`이슈가 있는 문제: ${totalIssues}`);
    console.log(`수정 필요 문제: ${needsFix}`);
    console.log('\n카테고리별:');
    console.log(`  - 정답 오류: ${this.issuesByCategory.answerError.length}`);
    console.log(`  - 해설 문제: ${this.issuesByCategory.explanationIssue.length}`);
    console.log(`  - 선택지 문제: ${this.issuesByCategory.optionsIssue.length}`);
    console.log(`  - 문제 내용: ${this.issuesByCategory.questionIssue.length}`);
  }

  generateReport(outputPath) {
    console.log(`\n리포트 생성 중: ${outputPath}`);

    const totalIssues = this.quizzes.filter(q => q.issues.length > 0).length;
    const needsFix = this.quizzes.filter(q => q.needsFix).length;

    let report = '# 퀴즈 문제 분석 리포트\n\n';

    // 통계
    report += '## 분석 통계\n';
    report += `- 총 검토 문제: ${this.quizzes.length}개\n`;
    report += `- 이슈 발견 문제: ${totalIssues}개\n`;
    report += `- 수정 필요 문제: ${needsFix}개\n\n`;

    report += '### 카테고리별 통계\n';
    report += `- 정답 오류: ${this.issuesByCategory.answerError.length}개\n`;
    report += `- 해설 문제: ${this.issuesByCategory.explanationIssue.length}개\n`;
    report += `- 선택지 문제: ${this.issuesByCategory.optionsIssue.length}개\n`;
    report += `- 문제 내용: ${this.issuesByCategory.questionIssue.length}개\n\n`;

    // 심각한 문제만 상세 출력
    report += '## 수정 필요 문제 상세\n\n';

    const criticalQuizzes = this.quizzes.filter(q => q.needsFix).slice(0, 100);

    criticalQuizzes.forEach(quiz => {
      report += `### 문제 #${quiz.lineNumber}\n`;
      report += `**코스**: ${quiz.courseId}, **섹션**: ${quiz.sectionNumber}\n`;
      report += `**문제**: ${quiz.questionText.substring(0, 100)}...\n\n`;

      if (quiz.questionType === 'MULTIPLE_CHOICE') {
        report += `**선택지**: ${JSON.stringify(quiz.options, null, 2)}\n`;
      }

      report += `**정답**: ${quiz.correctAnswer}\n`;
      report += `**해설**: ${quiz.explanation.substring(0, 200)}...\n\n`;

      report += '**발견된 이슈**:\n';
      quiz.issues.forEach(issue => {
        report += `- ${issue}\n`;
      });
      report += '\n---\n\n';
    });

    if (this.quizzes.filter(q => q.needsFix).length > 100) {
      report += `\n... 외 ${this.quizzes.filter(q => q.needsFix).length - 100}개 문제 생략\n`;
    }

    fs.writeFileSync(outputPath, report, 'utf-8');
    console.log('리포트 생성 완료');
  }
}

// 실행
const sqlFile = path.join(__dirname, 'quiz_course_generated_data(sol).sql');
const reportFile = path.join(__dirname, 'quiz_analysis_report.md');

const analyzer = new QuizAnalyzer(sqlFile);
analyzer.parseSqlFile();
analyzer.analyzeQuizzes();
analyzer.generateReport(reportFile);

console.log('\n분석 완료!');
