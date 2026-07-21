# 9. 算法视图 — FSRS-6 间隔复习调度器

## 9.1 概述

词汇复习采用 FSRS-6 算法。用户作答后自评（Again=1/Hard=2/Good=3/Easy=4），系统根据 21 参数 DSR 模型更新每题的记忆难度 D 和稳定性 S，计算下次复习时间。

## 9.2 数据结构

CardState（`user_word_progress` 表）：difficulty[1,10]、stability≥0.1（天）、state(New/Review)、due。

## 9.3 算法流程

用户提交 rating → 查旧 CardState → 判断：
- New → D₀=w₄-e^(w₅(G-1))+1, S₀=w[G-1]
- Again → 遗忘公式 S'=w₁₁·D^(-w₁₂)·((S+1)^w₁₃-1)·e^(w₁₄(1-R))
- 同日 → 短期公式 S'=S·e^(w₁₇(G-3+w₁₈))·S^(-w₁₉)
- 跨天 → 回忆公式 S'=S·(1+e^w₈(11-D)S^(-w₉)(e^(w₁₀(1-R))-1)·penalty·bonus)

→ 难度更新 D'=clamp((1-w₇)(D-w₆(G-3)(10-D)/9)+w₇·D₀(Easy),1,10)
→ 间隔 I=S/factor×(0.9^(1/-w₂₀)-1)，±5% fuzz
→ 写回 DB

## 9.4 参数

w[0..3]=0.21,1.29,2.31,8.30（首次 S₀）| w[4..5]=6.41,0.83（初始 D）| w[6..7]=3.02,0.001（难度更新）| w[8..10]=1.87,0.17,0.80（跨天稳定性）| w[11..14]=1.48,0.06,0.26,1.65（遗忘稳定性）| w[15..16]=0.60,0.87（Hard 罚/Easy 奖）| w[17..19]=0.54,0.09,0.07（同日复习）| w[20]=0.15（遗忘曲线衰减）

## 9.5 系统集成

ReviewService 维护 FSRS 单例（requestRetention=0.9）。POST /api/vocabulary/review → submitRating → fsrs.review() → save。GET /api/vocabulary/review-vocabulary → 查询 due≤now 的记录 JOIN vocabulary 返回。

## 9.6 验证

roxy 测试：wordId=1 Easy→D=1.0,S=8.3,7天后；wordId=250 Again→D=6.41,S=0.21,1天后。
