词汇数据形式约定
  {
    id: "purple",
    word: "purple",
    phonetic: "'pә:pl",
    definition: "n. a purple color or pigment\nn. of imperial status\nv. become purple\nv. color purple",
    briefTranslation: "紫色",
    translation: "n. 紫色, 帝位\na. 紫色的, 帝王的, 华而不实的\nv. (使)成紫色",
    collins: "2",
    oxford: "1",
    tag: "zk gk cet4 cet6 ky toefl",
    bnc: "5326",
    frq: "3894",
    exchange: "s:purples/d:purpled/i:purpling/p:purpled/3:purples/r:purpler/t:purplest",
    uk_audio: "https://dictionary.cambridge.org/media/english/uk_pron/u/ukp/ukpur/ukpurit004.mp3",
    us_audio: "https://dictionary.cambridge.org/media/english/us_pron/p/pur/purpl/purple.mp3",
    chineseOptions: ["紫色", "恶名昭著的", "旅行计划", "重新安排时间"],
    englishOptions: ["purple", "flagrant", "itinerary", "reschedule"]
  }

  其中id 应为主键，后面视数据库而定很有可能变为数字。前面的字段都需要有（从原数据删除2个字段："pos"，"en_definition"，增加一个字段briefTranslation 即从中文释义中选择一个）。后2个字段分别是中英文选项，各包含一个正确答案，其余为干扰项，需要生成。