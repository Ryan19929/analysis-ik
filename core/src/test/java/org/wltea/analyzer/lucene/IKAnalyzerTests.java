package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.Test;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.TestUtils;
import org.wltea.analyzer.dic.Dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IKAnalyzerTests {

    /**
     * 单char汉字+一个Surrogate Pair
     */
    @Test
    public void tokenizeCase1_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E");
        assert values.length == 2;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
    }

    /**
     * 单char汉字+一个Surrogate Pair+单char汉字
     */
    @Test
    public void tokenizeCase2_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E凤");
        assert values.length == 3;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("凤");
    }

    /**
     * 单char汉字和多Surrogate Pair混合
     */
    @Test
    public void tokenizeCase3_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E剃\uDB84\uDC97");
        assert values.length == 4;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("剃");
        assert values[3].equals("\uDB84\uDC97");
    }

    /**
     * 单char汉字和多个连续Surrogate Pair混合
     */
    @Test
    public void tokenizeCase4_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E\uDB84\uDC97");
        assert values.length == 3;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("\uDB84\uDC97");
    }

    /**
     * 单char汉字和多个连续Surrogate Pair加词库中的词
     */
    @Test
    public void tokenizeCase5_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        String[] values = tokenize(cfg, "菩\uDB84\uDD2E龟龙麟凤凤");
        assert values.length == 4;
        assert values[0].equals("菩");
        assert values[1].equals("\uDB84\uDD2E");
        assert values[2].equals("龟龙麟凤");
        assert values[3].equals("凤");
    }

    @Test
    public void testIKMatchHugeFromFile() {

        Configuration cfg = TestUtils.createFakeConfigurationSub(true);
        IKAnalyzer analyzer = new IKAnalyzer(cfg);
        String fileName = "/Users/black/code/banalysis-ik/core/src/main/java/org/wltea/analyzer/core/chinese-perf.txt";
        cfg.getConfDir();
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.printf("文件不存在: %s%n", fileName);
            return;
        }

        // 获取文件大小
        long bytes = file.length();
        System.out.printf("读取测试文件，包含 %d 字节。%n", bytes);

        try {
            // 创建Reader读取文件
            Reader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8);

            // 初始化IK分词器


            // 记录开始时间
            long start = System.currentTimeMillis();

            // 执行分词
            TokenStream ts = analyzer.tokenStream("contents", reader);
            ts.reset();
            CharTermAttribute term = ts.getAttribute(CharTermAttribute.class);

            int count = 0;
            while (ts.incrementToken()) {
                count++;
            }

            // 记录结束时间
            long end = System.currentTimeMillis();
            long time = end - start;

            // 输出统计信息
            System.out.printf("分词耗时: %d 毫秒%n", time);
            System.out.printf("分词数量: %d%n", count);
            System.out.printf("平均每个分词耗时: %.2f 微秒%n", (time * 1000.0) / count);
            System.out.printf("处理速度: %.2f MB/s%n",
                    (bytes / 1024.0 / 1024.0) / (time / 1000.0));

            // 清理资源
            ts.close();
            analyzer.close();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 用ik_max_word分词器分词
     */
    @Test
    public void tokenize_max_word_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(false);
        List<String> values = Arrays.asList(tokenize(cfg, "中华人民共和国国歌"));
        assert values.size() >= 9;
        assert values.contains("中华人民共和国");
        assert values.contains("中华人民");
        assert values.contains("中华");
        assert values.contains("华人");
        assert values.contains("人民共和国");
        assert values.contains("人民");
        assert values.contains("共和国");
        assert values.contains("共和");
        assert values.contains("国歌");
    }

    /**
     * 用ik_smart分词器分词
     */
    @Test
    public void tokenize_smart_correctly()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(true);
        List<String> values = Arrays.asList(tokenize(cfg, "中华人民共和国国歌"));
        assert values.size() == 2;
        assert values.contains("中华人民共和国");
        assert values.contains("国歌");
    }

    /**
     * 测试中文分词流程
     */
    @Test
    public void test_cjk_tokenize()
    {
        Configuration cfg = TestUtils.createFakeConfigurationSub(true);
        List<String> values = Arrays.asList(tokenize(cfg, "Hello, 我是一个测试管道工人"));
        System.out.println(values);
    }

    static String[] tokenize(Configuration configuration, String s)
    {
        ArrayList<String> tokens = new ArrayList<>();
        try (IKAnalyzer ikAnalyzer = new IKAnalyzer(configuration)) {
            TokenStream tokenStream = ikAnalyzer.tokenStream("text", s);
            tokenStream.reset();

            while(tokenStream.incrementToken())
            {
                CharTermAttribute charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
                OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
                int len = offsetAttribute.endOffset()-offsetAttribute.startOffset();
                char[] chars = new char[len];
                System.arraycopy(charTermAttribute.buffer(), 0, chars, 0, len);
                tokens.add(new String(chars));
            }
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        return  tokens.toArray(new String[0]);
    }
}
