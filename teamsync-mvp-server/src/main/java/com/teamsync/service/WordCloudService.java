package com.teamsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamsync.entity.Achievement;
import com.teamsync.mapper.AchievementMapper;
import com.teamsync.vo.WordCloudVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WordCloudService {

    private final AchievementMapper achievementMapper;

    // Common stop words
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一",
            "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
            "没有", "看", "好", "自己", "这", "他", "她", "它", "们", "那", "些",
            "为", "所", "以", "能", "对", "多", "让", "可以", "这个", "吗", "吧",
            "哦", "啊", "嗯", "哈", "呀", "嘛", "所以", "但是", "因为", "如果",
            "虽然", "而且", "然后", "还是", "只是", "就是", "不是",
            "今天", "明天", "昨天", "上午", "下午", "晚上", "时候"
    );

    public WordCloudService(AchievementMapper achievementMapper) {
        this.achievementMapper = achievementMapper;
    }

    public List<WordCloudVO> getWordCloud(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null) {
            wrapper.ge(Achievement::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(Achievement::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }

        List<Achievement> achievements = achievementMapper.selectList(wrapper);

        // In-memory word frequency counting
        Map<String, Integer> wordFreq = new HashMap<>();
        for (Achievement achievement : achievements) {
            if (achievement.getContent() != null && !achievement.getContent().isEmpty()) {
                List<String> words = simpleTokenize(achievement.getContent());
                for (String word : words) {
                    if (word.length() < 2) continue;
                    wordFreq.merge(word, 1, Integer::sum);
                }
            }
        }

        // Filter stop words and sort by frequency
        return wordFreq.entrySet().stream()
                .filter(e -> !STOP_WORDS.contains(e.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(50)
                .map(e -> new WordCloudVO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Simple Chinese tokenizer based on character bigrams and common patterns.
     * For MVP, this is a lightweight approach without external NLP libraries.
     */
    private List<String> simpleTokenize(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.isEmpty()) return words;

        // Remove punctuation and numbers
        String clean = text.replaceAll("[\\p{Punct}\\p{Digit}，。、；：？！「」【】《》（）—…\\s]", " ");

        // Split by whitespace first
        String[] parts = clean.split("\\s+");

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // For Chinese text, extract 2-4 character segments
            if (isChinese(part)) {
                // Bigram approach
                for (int i = 0; i < part.length() - 1; i++) {
                    String bigram = part.substring(i, i + 2);
                    if (!STOP_WORDS.contains(bigram)) {
                        words.add(bigram);
                    }
                    // Also try trigrams
                    if (i < part.length() - 2) {
                        String trigram = part.substring(i, i + 3);
                        if (!STOP_WORDS.contains(trigram)) {
                            words.add(trigram);
                        }
                    }
                }
            } else {
                // For non-Chinese, split by common delimiters
                words.add(part.toLowerCase());
            }
        }

        return words;
    }

    private boolean isChinese(String text) {
        return text.chars().anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
    }
}
