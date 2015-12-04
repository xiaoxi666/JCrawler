package com.youdao.dict.crawl;

import cn.edu.hfut.dmic.webcollector.model.Page;
import com.google.gson.Gson;
import com.youdao.dict.bean.ParserPage;
import com.youdao.dict.souplang.SoupLang;
import com.youdao.dict.util.OImageConfig;
import com.youdao.dict.util.OImageUploader;
import com.youdao.dict.util.TypeDictHelper;
import lombok.extern.apachecommons.CommonsLog;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by liuhl on 15-8-17.
 */
@CommonsLog
public class NewsNationalGeographicExtractor extends BaseExtractor {

    public NewsNationalGeographicExtractor(Page page) {
        super(page);
    }

    public NewsNationalGeographicExtractor(String url) {
        super(url);
    }

    public boolean init() {
        log.debug("*****init*****");
        try {
            SoupLang soupLang = new SoupLang(SoupLang.class.getClassLoader().getResourceAsStream("NewsNationalGeographicRule.xml"));
            context = soupLang.extract(doc);
            content = (Element) context.output.get("content");

            Element article = (Element) context.output.get("isarticle");
            if(article == null || article.toString().contains("article")){

                content.select(".byline").remove();//作者-时间等
                content.select(".Kicker").remove();
                content.select(".titleAndDek").remove();
                content.select(".col-md-1").remove();
                content.select(".icon-menu").remove();
                content.select(".info-button").remove();
                content.select("div[id=bottom-caption-button]").remove();
                content.select(".current-item-display").remove();
                content.select(".hidden").remove();
                content.select(".photo-gallery-initial--button").remove();
                content.select(".photo-gallery-initial__header").remove();
                content.select(".gallery-modal--next-gallery__body").remove();

                content.select(".media__caption--text").select(".mobile").remove();

                content.select(".gallery-modal__header").remove();

                content.select(".carousel-control").remove();

                content.select(".gallery-modal__footer").remove();

                content.select(".gallery-modal__right-rail").remove();

                content.select(".rightRailSlot").remove();
                content.select(".OneColumn").remove();//推荐链接
                content.select(".UniversalVideo").remove();//删除视频
                for(Element e: content.select("i")){//删除follow smBody on Twitter
                    String iText = e.text();
                    if(iText.contains("Follow "))
                        e.parent().remove();
                }


                content.select(".media--small").select(".left").remove();

                content.select(".instagram-media").remove();

//                content.select(".media--small").select(".right").remove();
//                content.select(".media--small").select(".middle").remove();
                content.select(".pull-quote__author").select(".hidden-md").remove();

                //加p标签
//                content.select(".mw-headline").wrap("<p></p>");
//
//                content.select(".step").wrap("<p></p>");
//
//                content.select(".step_num").wrap("<p></p>");
//
//                Elements lis = content.select("li");
//                for(Element e: lis){
//                    if(!e.parent().tagName().equals("ul"))
//                        e.unwrap();
//                }
//                if(content.select())
//                content.select("ul").select("li").wrap("<p></p>");
//                content.select(".mw-headline").wrap("<i></i>");
                log.debug("*****init  success*****");
                return true;
            }
            log.info("*****init  failed，isn't an article***** url:" + url);
            return false;
        } catch (Exception e) {
            log.info("*****init  failed***** url:" + url);
            return false;
        }
    }


    public boolean extractorContent() {
        return extractorContent(false);
    }

    public boolean extractorContent(boolean paging) {
        log.debug("*****extractorContent*****");
        if (content == null || p == null || (!paging && content.text().length() < MINSIZE)) {
            log.debug("*****extractorContent failed,return false*****");
            return false;
        }
        Elements hypLinks = content.select("a");
        for(Element a: hypLinks){
            a.unwrap();
//            System.out.println(a);
        }
        content.select("img").wrap("<p></p>");

        String contentHtml = content.html();

        contentHtml = contentHtml.replaceAll("&gt;", ">").replaceAll("&lt;", "<");//替换转义字符

        contentHtml = contentHtml.replaceAll("(?i)(<SCRIPT)[\\s\\S]*?((</SCRIPT>)|(/>))", "");//去除script
        contentHtml = contentHtml.replaceAll("(?i)(<NOSCRIPT)[\\s\\S]*?((</NOSCRIPT>)|(/>))", "");//去除NOSCRIPT
        contentHtml = contentHtml.replaceAll("(?i)(<STYLE)[\\s\\S]*?((</STYLE>)|(/>))", "");//去除style
        contentHtml = contentHtml.replaceAll("<(?!img|br|li|p[ >]|/p).*?>", "");//去除所有标签，只剩img,br,p
        contentHtml = contentHtml.replaceAll("\\\\s*|\\t|\\r|\\n", "");//去除换行符制表符/r,/n,/t /n
//        contentHtml = contentHtml.replaceAll("(\\n[\\s]*?)+", "\n");//多个换行符 保留一个----意义不大，本来也显示不出来，还是加<p>达到换行效果


        p.setContent(contentHtml);
        if (!paging && isPaging()) {
            mergePage(p);
        }
        log.debug("*****extractorContent  success*****");
        return true;
    }

    public boolean extractorTitle() {
        log.debug("*****extractorTitle*****");
//        String title = context.output.get("title").toString();
        Element elementTitle = (Element) context.output.get("title");
        if (elementTitle == null){
            log.error("extractorTitle failed skippppp");
            return false;
        }
        String title = elementTitle.attr("content");
        if (title == null || "".equals(title.trim())) {
            log.info("*****extractorTitle  failed***** url:" + url);
            return false;
        }
        title = title.replaceAll("\\\\s*|\\t|\\r|\\n", "");//去除换行符制表符/r,/n,/t
//        if (title.contains("-"))
//            p.setTitle(title.substring(0, title.lastIndexOf("-")).trim());
//        else
        p.setTitle(title.trim());
        log.debug("*****extractorTitle  success*****");
        return true;
    }

    public boolean extractorType() {
        Element elementType = (Element) context.output.get("type");
        if (elementType == null){
            log.debug("*****extractorType  null  skipp url:" + url);
            return false;

        }
        String type = elementType.attr("content");
        if(!TypeDictHelper.rightTheType(type)){
            Map<String, String> map = new HashMap<String, String>();
            map.put("orgType", type);
            String moreinfo = new Gson().toJson(map);
            p.setMoreinfo(moreinfo);
        }

        type = TypeDictHelper.getType(type, "News");


        p.setType(type.trim());

        Element elementLabel = (Element) context.output.get("label");
        if (elementLabel == null) {
            log.info("*****extractorLabel  failed, continue***** url:" + url);
            return true;
        }
        String label = elementLabel.attr("content");
//        String label = (String) context.output.get("label");
        if (label == null || "".equals(label.trim())) {
            log.info("*****extractorLabel  failed, continue***** url:" + url);
            return true;
        }
//        label = label.contains("China")?"China":label.contains("news")? "World": label;//news belong to World
        String[] keywords = label.split(", ");
        PriorityQueue<String> pq = new PriorityQueue<String>(10, new Comparator<String>(){

            @Override
            public int compare(String o1, String o2) {
                int length1 = o1.split(" ").length, length2 = o2.split(" ").length;
                return length1 - length2;
            }
        });

        for(String keyword: keywords){
            keyword = keyword.replaceAll(",", "");
            int wordCount = keyword.split(" ").length;
            if(wordCount <= 3) pq.add(keyword);
        }
        StringBuilder sb = new StringBuilder();
        while(!pq.isEmpty()){
            sb.append(pq.poll());
            if(!pq.isEmpty()) sb.append(", ");
        }
        p.setLabel(sb.toString());
        log.debug("*****extractorTitle  success*****");
        return true;
    }

    public boolean extractorTime() {
        log.debug("*****extractorTime*****");
        Element elementTime = (Element) context.output.get("time");
        if (elementTime == null){//business版head meta里没有时间
            log.error("can't extract Time, skip url:" + url);
            return false;

        }
        String time = elementTime.attr("content");
        if (time == null || "".equals(time.trim())) {
            log.info("*****extractorTime  failed***** url:" + url);
            return false;
        }
//2015-09-12 08:25
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.US);
        Date date;
        try {
            date = format.parse(time.trim());
        } catch (ParseException e) {
            log.info("*****extractorTime format.parse  failed***** url:" + url);
            return false;
        }
//        if (System.currentTimeMillis() - date.getTime() > Long.MAX_VALUE >> 25) {
//            log.debug("*****extractorTime  out of date*****");
//            return false;
//        }
        p.setTime(new Timestamp(date.getTime() + 13 * 60 * 60 * 1000).toString());//-0500纽约时间 +13h是 cst北京时间
        log.debug("*****extractorTime  success*****");
        return true;
    }

    public boolean extractorDescription() {
        log.debug("*****extractor Desc*****");
        Element elementTime = (Element) context.output.get("description");
        if (elementTime == null){//business版head meta里没有时间
            log.error("can't extract desc, continue + url : " + url);
            return true;
        }
        String description = elementTime.attr("content");
        if (description == null || "".equals(description.trim())) {
            log.info("*****extractor Desc  failed***** url:" + url);
            return true;
        }

        p.setDescription(description);

        return true;
    }

    public boolean isPaging() {
//        Elements div2 = doc.select("div[id=\"content-main\"]");
        Elements div2 = content.select("div[id=content-main]");
        Elements sociallinks = div2.select("div[class=social-links]");
        if(sociallinks != null) sociallinks.remove();//去除社交网络分享栏目框
        Elements div = div2.select("div[class=item-list");
        if (div == null) {
            return false;
        }
        Elements a = div.select("li");
        if (a == null || a.size() == 0) {
            return false;
        }
/*        if (url.equals(a.get(0).attr("href"))) {
            return false;
        }*/
        return true;
    }

    public boolean extractorAndUploadImg(String host, String port) {
        log.debug("*****extractorAndUploadImg*****");
        if (content == null || p == null) {
            return false;
        }

        //文章题目重复出现，去除之
//        content.select("div[id=block-onix-highlight-onix-highlight-article-header]").remove();
//        content.select("div[id=block-views-article-title-block]").remove();
//        //
//        content.select("div[id=157_ArticleControl_divShareButton]").remove();
//        if(isPaging()) return true;
       /* if (host.equals(port)) return true;*/


        Elements imgs = content.select(".delayed-image-load--photogallery-modal");
        if(imgs == null || imgs.size() < 1)
                imgs = content.select(".delayed-image-load");

        String mainImage = null;
        int width = 0;
        for (Element img : imgs) {
            try {
                String imageUrl = img.attr("data-src");
                //                if ("".equals(imageUrl) || !"".equals(img.attr("data-src-small")) || !"".equals(img.attr("itemprop"))) {
                if ("".equals(imageUrl)) {
                    imageUrl = img.attr("src");
                    if ("".equals(imageUrl)) {
                        img.remove();
                        continue;
                    }
                }
                Tag imgTag = Tag.valueOf("img");
//                img.appendChild(imgTag);
                Element newImg = new Element(imgTag, "");
                img.appendChild(newImg);
                img = newImg;
                img.attr("style", "width:100%;");

                int idx = imageUrl.indexOf("width");
                if(idx > 0){
                    imageUrl = imageUrl.substring(0, idx - 2);
                }else{
                    imageUrl.substring(0, imageUrl.indexOf("jpg") + 3);
                }

                OImageUploader uploader = new OImageUploader();
                if (!"".equals(host) && !"".equals(port))
                    uploader.setProxy(host, port);
                long id = uploader.deal(imageUrl);
                //                long id = 0;
                URL newUrl = new OImageConfig().getImageSrc(id, "dict-consult");

                img.attr("src", newUrl.toString());

                if (mainImage == null) {
                    width = uploader.getWidth();
                    mainImage = newUrl.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Element elementImg = (Element) context.output.get("mainimage");
        if (elementImg != null){
            String tmpMainImage = elementImg.attr("content");
            OImageUploader uploader = new OImageUploader();
            if (!"".equals(host) && !"".equals(port))
                uploader.setProxy(host, port);
            long id = 0;
            try {
                id = uploader.deal(tmpMainImage);

//                long id = 0;
                URL newUrl = new OImageConfig().getImageSrc(id, "dict-consult");
                width = uploader.getWidth();
                mainImage = newUrl.toString();
            } catch (Exception e1) {
//                        e1.printStackTrace();

            }
        }
//        p.setMainimage(mainImage);
        if (width == 0) {
            p.setStyle("no-image");
        } else if (width >= 300) {

            p.setMainimage(mainImage);
            p.setStyle("large-image");

        } else {
            p.setStyle("no-image");
        }

//        } catch (Exception e) {
//            p.setStyle("no-image");
//        }
        return true;

    }

    public void mergePage(ParserPage p) {
        log.debug("*****mergePage*****");
//        Elements div = doc.select("div[class=\"item-list\"]").select("li[class=\"pager-next\"]").select("a");
//        LinkedList<String> list = new LinkedList<String>();
//        for (Element a : div) {
//            String url = a.attr("href");
//            if (!list.contains(url)) {
//                list.add(url);
//            }
//        }
//        Elements div2 = content.select("div[id=content-main]");
//        Elements sociallinks = div2.select("div[class=social-links]");
//        if(sociallinks != null) sociallinks.remove();//去除社交网络分享栏目框
//        Elements div = div2.select("div[class=item-list");
//        if (div == null) {
//            return;
//        }
//        Elements a = div.select("li");

//        String nextPage = div.attr("href");
//        if(nextPage.equals("")) return;
//        CNBCExtractor extractor = new CNBCExtractor(url + "?singlepage=true");
//        extractor.init();
//        extractor.extractorAndUploadImg();
//        extractor.extractorContent(true);
//        p.setContent(extractor.getParserPage().getContent());

        log.info("*****mergePage end*****");
    }

}