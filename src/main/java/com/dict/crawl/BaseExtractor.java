package com.dict.crawl;

import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.util.JsoupUtils;
import com.dict.bean.ParserPage;
import com.dict.souplang.Context;
import com.dict.util.GFWHelper;
import com.dict.util.OImageUploader;
import com.rometools.rome.feed.synd.SyndEntry;
import com.dict.score.LeveDis;
import com.dict.util.OImageConfig;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang.StringEscapeUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.xpath.operations.String;

/**
 * Created by liuhl on 15-8-17.
 */
@CommonsLog
public class BaseExtractor {
    Context context;
    String keywords;
    public static long MINSIZE = 384;
    public static long MINWORDCOUNT = 30;
    public ParserPage p = new ParserPage();
    private static String contentChatset = "utf-8";
    public static String ADDITIONAL_TAGNAME = "div";
//    private String
    String url;
    Document doc;
    Element content;
    SyndEntry _rssEntry;
    Page _page;
    String[] wordArray = null;
    List<ParserPage> parserPages = new ArrayList<ParserPage>();

    public static String CUENTTIME = new Timestamp(System.currentTimeMillis()).toString();

    static Set<String> normalHour = new HashSet<String>();

    static {
        normalHour.add("06");
        normalHour.add("11");
        normalHour.add("15");
        normalHour.add("20");
    }

    public ParserPage getParserPage() {
        return p;
    }

    public BaseExtractor(){

    }

    public BaseExtractor(Page page) {
        url = page.getUrl();
        this._page = page;

        if (!getDoc(page)) {
            doc = page.getDoc();//瞎猜字符编码，有时候会猜错
        }
        JsoupUtils.makeAbs(doc, url);
        p.setHost(getHost(url));
        p.setUrl(url);
    }

    /**
     * @param LazyLoad 是否加载js
     *
     * */
    public BaseExtractor(Page page, boolean LazyLoad) {
        url = page.getUrl();

        boolean getDocSucc = LazyLoad?getJsLoadedDoc():getDoc(page);

        if (!getDocSucc) {
            doc = page.getDoc();//瞎猜字符编码，有时候会猜错
        }
        JsoupUtils.makeAbs(doc, url);
        p.setHost(getHost(url));
        p.setUrl(url);
    }



    public boolean getJsLoadedDoc(){

        Capabilities caps = new DesiredCapabilities();
        ((DesiredCapabilities) caps).setJavascriptEnabled(true);
        ((DesiredCapabilities) caps).setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent",
                "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19" +
                        " (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19");
//        ((DesiredCapabilities) caps).setCapability("takesScreenshot", true);
        ((DesiredCapabilities) caps).setCapability(
                PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
//                "D:\\Crawl\\phantomjs-2.1.1-windows\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe"
                "/global/exec/zangyq/phantomjs-2.1.1-linux-x86_64/bin/phantomjs"
        );
        WebDriver   driver = new PhantomJSDriver(caps);

//        WebDriver driver = new ChromeDriver();
//        driver.setJavascriptEnabled(true);
        driver.manage().timeouts().pageLoadTimeout(2, TimeUnit.MINUTES);
        driver.get(_page.getUrl());

        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String html = driver.getPageSource();
        driver.quit();
//        if (doc == null)
        this.doc = Jsoup.parse(html, url);
        _page.setDoc(this.doc);
        return true;
    }

    public boolean getDoc(Page page) {

        //获取reponse中的charset
        byte[] contentByte = page.getContent();
        String contentType = page.getResponse().getContentType();
        if (contentType != null && contentType.toLowerCase().contains("charset")) {
            String contentTypeLow = contentType.toLowerCase();
            if (!contentTypeLow.contains("utf-8")) {//不是utf8 需要取出来
                int index = contentTypeLow.indexOf("charset=");
                if (index >= 0)
                    contentChatset = contentTypeLow.substring(index + 8);
            }
            try {
                String html = new String(contentByte, contentChatset);
                if (doc == null) this.doc = Jsoup.parse(html, url);
                page.setDoc(this.doc);
                return true;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        log.error("getDoc(page) faile, false");
        return false;
    }

    public BaseExtractor(String url) {
        try {
            Connection conn = Jsoup.connect(url);
            conn.timeout(1000 * 60 * 3);
            this.doc = conn.get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        this.url = url;
        JsoupUtils.makeAbs(doc, url);
        p.setHost(getHost(url));
        p.setUrl(url);
    }


    public void insertWith(JdbcTemplate jdbcTemplate){
        int updates = jdbcTemplate.update("insert ignore into parser_page (title, type, label, level, style" +
                        ", host, url, time, description, content, wordCount, uniqueWordCount, version, mainimage" +
                        ", moreinfo) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                p.getTitle(), p.getType(), p.getLabel(), p.getLevel(), p.getStyle()
                , p.getHost(), p.getUrl(), p.getTime(), p.getDescription(), p.getContent(), p.getWordCount()
                , p.getUniqueWordCount(), p.getVersion(), p.getMainimage(), p.getMoreinfo());
        if (updates == 1) {
            System.out.println("parser_page插入成功");
            int id = jdbcTemplate.queryForInt("SELECT id FROM parser_page WHERE url = ?", p.getUrl());

            updates = jdbcTemplate.update("insert ignore into org_content (id, content) values (?,?)",
                    id, doc.html());
            System.out.println("org_content插入成功");
        } else {
            System.out.println("失败插入mysql");
        }
    }

    public boolean extractor() {
        if (init())
            return extractorTime() && extractorTitle() && extractorType()
                    && extractorAndUploadImg() && extractorDescription()
                    && extractorContent() && extractorKeywords() && extractorTags(keywords, p.getLabel())
                    && contentWordCount() && addAdditionalTag();
        else {
            log.error("init failed");
            return false;
        }
    }

    public boolean init() {
        return false;
    }

    public boolean extractorTitle() {
        return false;
    }

    public boolean extractorType() {
        return false;
    }

    public boolean extractorTime() {
        return false;
    }

    public boolean extractorKeywords() {
        log.debug("*****extractorKeywords*****");
        Element keywordsElement = (Element) context.output.get("keywords");
        if (keywordsElement == null)
            return true;
        keywords = keywordsElement.attr("content");
        if (keywords == null || "".equals(keywords)) {
            return true;
        }
        if (!keywords.contains(",")) {
            keywords = "".equals(keywords) ? "" : keywords.replaceAll(" ", ",");
        }
        return true;
    }

    public boolean isPaging() {
        return false;
    }

    public boolean extractorAndUploadImg() {
        return extractorAndUploadImg("", "");
    }

    public boolean extractorAndUploadImg(String host, String port) {
        log.debug("*****extractorAndUploadImg*****");
        if (content == null || p == null) {
            log.error("content or p null, return false");
            return false;
        }
       /* if (host.equals(port)) return true;*/
        String mainImage = null;
        int width = 0;
        try {
            Elements imgs = content.select("img");
            for (Element img : imgs) {
                String imageUrl = img.attr("src");
                img.removeAttr("width");
                img.removeAttr("WIDTH");
                img.removeAttr("height");
                img.removeAttr("HEIGHT");
//                img.attr("style", "width:100%;");
                OImageUploader uploader = new OImageUploader();
                if (!"".equals(host) && !"".equals(port))
                    uploader.setProxy(host, port);
                long id = 0;
                try {
                    id = uploader.deal(imageUrl);
                }catch (Exception e){
//                    img.attr("src", imageUrl);
                    log.error("use org img url, continue");
                    continue;
                }
                URL newUrl = new OImageConfig().getImageSrc(id, "dict-consult");
                int twidth = uploader.getWidth();
                if (twidth >= 300)
                    img.attr("style", "width:100%;");
                img.attr("src", newUrl.toString());
                if (mainImage == null) {
                    width = uploader.getWidth();
                    mainImage = newUrl.toString();
                }
            }


        } catch (Exception e) {
            p.setStyle("no-image");
        }
        p.setMainimage(mainImage);
        if (width == 0) {
            p.setStyle("no-image");
        } else if (width > 300) {
            p.setStyle("large-image");
        } else {
            p.setStyle("no-image");
        }
        return true;
    }

    public boolean extractorDescription() {
        return true;
    }

    public boolean extractorContent() {
        return extractorContent(false);
    }


    public void HideVideo(String videoSelector) {
        Elements videos = content.select("videoSelector").select("iframe");
        for(Element e: videos){
            String videoUrl = e.attr("src");
            String className = e.className();
            Tag imgTag = Tag.valueOf("p");
//                img.appendChild(imgTag);
            Element newImg = new Element(imgTag, "");
            newImg.attr("class", "iframe");
            newImg.attr("src", videoUrl);
            newImg.attr("style", "width:100%; heigh:100%");
            e.appendChild(newImg);
        }
    }

    public static int contentWordCount(Element content){

        if(content == null){
            log.error("content null while word count");
            return  0;
        }
        String text = content.text();
//        Pattern wordRegx = Pattern.compile("\\w");
//        System.out.println(wordRegx.matcher(text).group());
//        int count = wordRegx.matcher(text).groupCount();
        int count = text.split("[^a-zA-Z']+").length;

        return count;
    }

    public static int getUniqueCount(String content){
        String[] words = getWordArray(content);

        Set<String> uniqueSet = new HashSet<String>();
        for(String word: words){
            uniqueSet.add(word);
        }
        return uniqueSet.size();
    }

    public static int getUniqueCount(Element content){
        String[] words = getWordArray(content);

        Set<String> uniqueSet = new HashSet<String>();
        for(String word: words){
            uniqueSet.add(word);
        }
        return uniqueSet.size();
    }

    public static String[] getWordArray(String content){

        Document soupContent = Jsoup.parse(content);
        content = soupContent.body().text();
        if(content == null){
            log.error("content null while word count");
            return  new String[0];
        }

        String[] t = content.split("[^a-zA-Z'’‘]+");
        return t;
    }

    public static String[] getWordArray(Element contentE){
        String content = contentE.text();
        if(content == null){
            log.error("content null while word count");
            return  new String[0];
        }

        String[] t = content.split("[^a-zA-Z'’‘]+");
        return t;
    }

    public boolean contentWordCount(){

        String[] words = getWordArray(p.getContent());
        int wordCount = words.length;
        if (wordCount < MINWORDCOUNT){
            log.error("wordCount too small, false, url: " + url);
            return false;
        }
        p.setWordCount(words.length);

        Set<String> uniqueSet = new HashSet<String>();
        for(String word: words){
            uniqueSet.add(word);
        }

        p.setUniqueWordCount(uniqueSet.size());

        return true;
    }

    public static int contentWordCount(String content){
        if(content == null || content.length() < 1){
            log.error("content null while word count");
            return  0;
        }
        Element contentE = Jsoup.parse(content);

        return contentWordCount(contentE);
    }

    public boolean extractorContent(boolean paging) {
        log.debug("*****extractorContent*****");
        if (content == null || p == null || (!paging && content.text().length() < MINSIZE)) {
            log.error("extractorContent failed return false");
            return false;
        }
        Elements hypLinks = content.select("a");
        for (Element a : hypLinks) {
            a.unwrap();
//            System.out.println(a);
        }

        removeComments(content);

        String contentHtml = content.html();

        contentHtml = StringEscapeUtils.unescapeHtml(contentHtml);//替换转义字符

        contentHtml = contentHtml.replaceAll("(?i)(<SCRIPT)[\\s\\S]*?((</SCRIPT>)|(/>))", "");//去除script
        contentHtml = contentHtml.replaceAll("(?i)(<NOSCRIPT)[\\s\\S]*?((</NOSCRIPT>)|(/>))", "");//去除NOSCRIPT
        contentHtml = contentHtml.replaceAll("(?i)(<STYLE)[\\s\\S]*?((</STYLE>)|(/>))", "");//去除style
        contentHtml = contentHtml.replaceAll("<(?!img|br|p[ >]|/p).*?>", "");//去除所有标签，只剩img,br,p
        contentHtml = contentHtml.replaceAll("\\\\s*|\\t|\\r|\\n", "");//去除换行符制表符/r,/n,/t /n
//        contentHtml = contentHtml.replaceAll("(\\n[\\s]*?)+", "\n");
// 多个换行符 保留一个----意义不大，本来也显示不出来，还是加<p>达到换行效果


        if (contentHtml.length() < 384) {
            log.error("content after extracted too short, false");
            return false;//太短
        }

        p.setContent(contentHtml);
        if (!paging && isPaging()) {
            mergePage(p);
        }
        log.debug("*****extractorContent  success*****");
        return true;
    }

    public boolean addAdditionalTag(){
        String content = p.getContent();
        content = addAdditionalTag(content);
        p.setContent(content);
        return true;

    }
    public static String addAdditionalTag(String content){
        Document soupContent = Jsoup.parse(content);
        return addAdditionalTag(soupContent.body());

    }

    public static String addAdditionalTag(Element content){
//        Elements childrenNodes = content.childNodes();//textNodes();//children();
        List<Node> childrenNodes = content.childNodes();//textNodes();//children();
        for(Node node: childrenNodes){
            if(!node.nodeName().equals("div"))
                node.wrap("<" + ADDITIONAL_TAGNAME +"></" + ADDITIONAL_TAGNAME +">");
        }
//        childrenNodes.wrap("&lt;" + ADDITIONAL_TAGNAME +"&gt;&lt;/" + ADDITIONAL_TAGNAME +"&gt;");
        return content.html();
    }

    public void replaceFrame() {
        Elements videos = content.select("iframe");
        for (Element e : videos) {
            String videoUrl = e.attr("src");
            if (GFWHelper.isBlocked(videoUrl))
                continue;
            String className = e.className();
            Tag imgTag = Tag.valueOf("p");
//                img.appendChild(imgTag);
            Element newImg = new Element(imgTag, "");
            newImg.attr("class", "iframe");
            newImg.attr("src", videoUrl);
            newImg.attr("style", "width:100%; heigh:100%");
            newImg.attr("heigh", "200");
            e.appendChild(newImg);
        }
    }

    public boolean extracteAvgLength(){
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");    // 七种Annotators
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);    // 依次处理

//        String text = "This is a test.";               // 输入文本

        Annotation document = new Annotation(p.getContent());    // 利用text创建一个空的Annotation
        pipeline.annotate(document);                   // 对text执行所有的Annotators（七种）

        // 下面的sentences 中包含了所有分析结果，遍历即可获知结果。
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        double avgSentLength = 0.0;
        int  totleSentLength = 0;
        Set<String> uniqueWord = new HashSet<String>();
        for(CoreMap coreMap: sentences){
            List<CoreLabel> tokens = coreMap.get(CoreAnnotations.TokensAnnotation.class);
            for(CoreLabel cl: tokens){
                uniqueWord.add(cl.get(CoreAnnotations.TextAnnotation.class));
            }
            int sentLength = tokens.size();
            totleSentLength += sentLength;
        }
        avgSentLength = 1.0 * totleSentLength / sentences.size();

        int totleCharCount = 0;
        double avgWordLenth = 0.0;
        for(String word: uniqueWord){
            totleCharCount += word.length();
        }
        avgWordLenth = (1.0 * totleCharCount) / uniqueWord.size();

        p.setAvgSentLength(avgSentLength);
        p.setWordCount(totleSentLength);
        p.setUniqueWordCount(uniqueWord.size());
        p.setAvgWordLength(avgWordLenth);

        return true;
    }

    public static void main(String[] args){
        System.out.println(CUENTTIME);
        String testContent = "Lightweights Ivan Redkach (19-1-1, 15 KOs) and Luis Cruz (22-4-1, 16 KOs) " +
                "fought to a split draw in a bout where both scored knockdowns Tuesday night at Sands " +
                "Bethlehem Events Center in Bethlehem, PA. Here’s what ";

        new BaseExtractor().contentWordCount(testContent);
//        new BaseExtractor().resumeFrame(testContent);
    }

    public void HideSomeHypLink(Element _content) {
        Elements hypLinks = _content.select("a");
        for (Element a : hypLinks) {
//            if(a.text().toLowerCase().matches("(.*[^\\\\w])?(more|here)+(s|ment|\\\\'s|ies|es|ing|ship|ion|e)?([^\\\\w].*)?"))
            if(a.text().toLowerCase().contains("more") || a.text().toLowerCase().contains("here")) {
                continue;
            }
            a.unwrap();
//            System.out.println(a);
        }
    }

    public String resumeFrame(String orgContent) {
//        Document extractedContent = Jsoup.parseBodyFragment(orgContent);
        Document extractedContent = Jsoup.parse(orgContent);
//        for(String className: classNames){
        Elements videoClassNames = extractedContent.select(".iframe");
        for (Element e : videoClassNames) {
            String videoUrl = e.attr("src");
            Tag imgTag = Tag.valueOf("iframe");
//                img.appendChild(imgTag);
            Element newImg = new Element(imgTag, "");
            newImg.attr("src", videoUrl);
            newImg.attr("style", "width:100%");
//                newImg.a
            e.appendChild(newImg);
            e.unwrap();
        }
        HideSomeHypLink(extractedContent);//逻辑有点乱，先这样吧，有机会再重构
        return extractedContent.body().html();
    }

    public static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size(); ) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }

    public void mergePage(ParserPage p) {
    }

    public boolean extractorTags(String... keywords) {
        log.debug("*****extractorTags*****");
        if (content == null) {
            log.error("*****extractorTags  failed***** url:" + url);
            return false;
        }
        try {
            String contentStr = content.text();
            LeveDis leveDis = LeveDis.getInstance("");
            String tags = leveDis.tag(contentStr, 5);
            if (keywords != null) {
                for (String key : keywords) {
                    if ("".equals(key) || key == null) {
                        continue;
                    }
                    if (!"".equals(tags) && !tags.contains(key)) {
                        tags = key + "," + tags;
                    } else {
                        tags = key;
                    }
                }
            }
            p.setLabel(tags);
            int level = leveDis.compFileLevel(leveDis.compLevel(contentStr));
            p.setLevel(String.valueOf(level));
            log.debug("*****extractorTags  success*****");
            return true;
        } catch (Exception e) {
            log.error("*****extractorTags  failed***** url:" + url);
            return false;
        }
    }

    public static String getHost(String url) {
        if (url == null || url.trim().equals("")) {
            return "";
        }
        String host = "";
        Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
        Matcher matcher = p.matcher(url);
        if (matcher.find()) {
            host = matcher.group();
        }
        return host;
    }

    public List<ParserPage> getParserPageList() {
        return parserPages;
    }
}