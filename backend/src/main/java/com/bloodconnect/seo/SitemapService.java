package com.bloodconnect.seo;

import com.bloodconnect.bloodrequest.entity.BloodRequest;
import com.bloodconnect.bloodrequest.repository.BloodRequestRepository;
import com.bloodconnect.common.enums.RequestStatus;
import com.bloodconnect.config.BloodConnectProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SitemapService {

    private static final List<RequestStatus> INDEXABLE_STATUSES =
            List.of(RequestStatus.OPEN, RequestStatus.IN_PROGRESS);
    private static final int REQUEST_LIMIT = 1000;

    private static final List<StaticPage> STATIC_PAGES = List.of(
            new StaticPage("/", "daily", "1.0"),
            new StaticPage("/solicitudes", "daily", "0.9"),
            new StaticPage("/donantes", "weekly", "0.8"),
            new StaticPage("/centros", "weekly", "0.7"),
            new StaticPage("/como-donar", "monthly", "0.8"),
            new StaticPage("/compatibilidad", "monthly", "0.8"),
            new StaticPage("/preguntas-frecuentes", "monthly", "0.6"),
            new StaticPage("/eliminacion-de-cuenta", "yearly", "0.3")
    );

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodConnectProperties properties;

    @Transactional(readOnly = true)
    public String buildXml(HttpServletRequest request) {
        String origin = resolveOrigin(request);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (StaticPage page : STATIC_PAGES) {
            appendUrl(xml, origin, page.path(), page.changefreq(), page.priority(), null);
        }
        List<BloodRequest> requests = bloodRequestRepository.findByStatusIn(
                INDEXABLE_STATUSES,
                PageRequest.of(0, REQUEST_LIMIT, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        ZoneId zone = ZoneId.of(properties.timezone());
        for (BloodRequest bloodRequest : requests) {
            String path = PublicUrlSlug.requestPath(
                    bloodRequest.getBloodType(),
                    bloodRequest.getMunicipality().getName(),
                    bloodRequest.getProvince().getName(),
                    bloodRequest.getId()
            );
            String lastmod = bloodRequest.getUpdatedAt() == null
                    ? null
                    : bloodRequest.getUpdatedAt().atZone(zone).toLocalDate().toString();
            appendUrl(xml, origin, path, "daily", "0.8", lastmod);
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void appendUrl(
            StringBuilder xml,
            String origin,
            String path,
            String changefreq,
            String priority,
            String lastmod
    ) {
        String loc = origin + ("/".equals(path) ? "/" : path);
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escape(loc)).append("</loc>\n");
        if (lastmod != null && !lastmod.isBlank()) {
            xml.append("    <lastmod>").append(escape(lastmod)).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String resolveOrigin(HttpServletRequest request) {
        String configured = properties.publicSiteUrl();
        if (configured != null && !configured.isBlank()) {
            return trimSlash(configured);
        }
        String forwardedSite = request.getHeader("X-Forwarded-Site-Url");
        if (forwardedSite != null && !forwardedSite.isBlank()) {
            return trimSlash(forwardedSite);
        }
        String proto = firstHeader(request, "X-Forwarded-Proto", request.getScheme());
        String host = firstHeader(request, "X-Forwarded-Host", request.getServerName());
        if (host.contains(":")) {
            return proto + "://" + host;
        }
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(proto) && port == 80) || ("https".equals(proto) && port == 443);
        if (defaultPort || port <= 0) {
            return proto + "://" + host;
        }
        return proto + "://" + host + ":" + port;
    }

    private static String firstHeader(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.split(",")[0].trim();
    }

    private static String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record StaticPage(String path, String changefreq, String priority) {
    }
}
