package com.englishlearningcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vocabulary")
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(length = 500)
    private String phonetic;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String translation;

    @Column(name = "brief_translation", length = 100)
    private String briefTranslation;

    @Column(length = 10)
    private String collins;

    @Column(length = 10)
    private String oxford;

    @Column(length = 200)
    private String tag;

    @Column(length = 20)
    private String bnc;

    @Column(length = 20)
    private String frq;

    @Column(length = 500)
    private String exchange;

    @Column(name = "uk_audio", length = 500)
    private String ukAudio;

    @Column(name = "us_audio", length = 500)
    private String usAudio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chinese_options", columnDefinition = "json")
    private List<String> chineseOptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "english_options", columnDefinition = "json")
    private List<String> englishOptions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getBriefTranslation() {
        return briefTranslation;
    }

    public void setBriefTranslation(String briefTranslation) {
        this.briefTranslation = briefTranslation;
    }

    public String getCollins() {
        return collins;
    }

    public void setCollins(String collins) {
        this.collins = collins;
    }

    public String getOxford() {
        return oxford;
    }

    public void setOxford(String oxford) {
        this.oxford = oxford;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBnc() {
        return bnc;
    }

    public void setBnc(String bnc) {
        this.bnc = bnc;
    }

    public String getFrq() {
        return frq;
    }

    public void setFrq(String frq) {
        this.frq = frq;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getUkAudio() {
        return ukAudio;
    }

    public void setUkAudio(String ukAudio) {
        this.ukAudio = ukAudio;
    }

    public String getUsAudio() {
        return usAudio;
    }

    public void setUsAudio(String usAudio) {
        this.usAudio = usAudio;
    }

    public List<String> getChineseOptions() {
        return chineseOptions;
    }

    public void setChineseOptions(List<String> chineseOptions) {
        this.chineseOptions = chineseOptions;
    }

    public List<String> getEnglishOptions() {
        return englishOptions;
    }

    public void setEnglishOptions(List<String> englishOptions) {
        this.englishOptions = englishOptions;
    }
}
