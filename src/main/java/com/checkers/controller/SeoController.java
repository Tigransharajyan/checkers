package com.checkers.controller;

import com.checkers.seo.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.StringWriter;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class SeoController {
    private final SeoService seo;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = seo.indexingEnabled()
                ? "User-agent: *\nAllow: /\nDisallow: /api/\nDisallow: /ws/\nDisallow: /h2-console\n\nSitemap: " + seo.baseUrl() + "/sitemap.xml\n"
                : "User-agent: *\nDisallow: /\n";
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() throws XMLStreamException {
        StringWriter text = new StringWriter();
        var xml = XMLOutputFactory.newFactory().createXMLStreamWriter(text);
        xml.writeStartDocument("UTF-8", "1.0");
        xml.writeStartElement("urlset");
        xml.writeDefaultNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
        xml.writeNamespace("xhtml", "http://www.w3.org/1999/xhtml");
        if (seo.indexingEnabled()) {
            for (String path : SeoService.INDEXABLE_PATHS) {
                for (String language : SeoService.LANGUAGES) {
                    xml.writeStartElement("url");
                    xml.writeStartElement("loc");
                    xml.writeCharacters(seo.localizedUrl(path, language));
                    xml.writeEndElement();
                    for (var alternate : seo.metadata(path, java.util.Locale.forLanguageTag(language)).alternates().entrySet()) {
                        xml.writeEmptyElement("xhtml", "link", "http://www.w3.org/1999/xhtml");
                        xml.writeAttribute("rel", "alternate");
                        xml.writeAttribute("hreflang", alternate.getKey());
                        xml.writeAttribute("href", alternate.getValue());
                    }
                    xml.writeEndElement();
                }
            }
        }
        xml.writeEndElement();
        xml.writeEndDocument();
        xml.close();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(text.toString());
    }
}
