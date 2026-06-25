package fr.renater.shibboleth.esup.otp.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import net.shibboleth.shared.primitive.LoggerFactory;
import org.springframework.util.StreamUtils;

/**
 * Esup otp http client interceptor to log requests.
 */
@Order(2)
public class EsupOtpLoggingInterceptor implements ClientHttpRequestInterceptor {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EsupOtpLoggingInterceptor.class);

    private static final String EMAIL_REGEX = "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)";

    private static final List<String> HEADERS_TO_SANITIZE = Arrays.asList("Authorization", "Cookie");

    /** {@inheritDoc} */
    public @Nonnull ClientHttpResponse intercept(@Nonnull final HttpRequest request, 
            @Nonnull final byte[] body, @Nonnull final ClientHttpRequestExecution execution) throws IOException {
        logRequest(request, body);
        final ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    /**
     * Log as debug request.
     *
     * @param request http request.
     * @param body request body.
     */
    private void logRequest(@Nonnull final HttpRequest request, @Nonnull final byte[] body) {
        log.debug("==========================request begin==========================");
        log.debug("URI         : {}", sanitizeRequest(request.getURI()));
        log.debug("Method      : {}", request.getMethod());
        log.debug("Headers     : {}", sanitizeHeaders(request.getHeaders()));
        if (body.length > 1) {
            final InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8);
            try(BufferedReader br = new BufferedReader(isr)) {
                final String requestBody = br.lines().collect(Collectors.joining("\n"));
                log.debug("Request body: {}", requestBody);
            } catch (final IOException e) {
                log.debug("IOException : Cannot read request body");
            }
        }
        log.debug("==========================request end============================");
    }

    /**
     * Log as debug response.
     *
     * @param response from esup-otp-api server.
     */
    private void logResponse(@Nonnull final ClientHttpResponse response) {
        log.debug("==========================response begin==========================");
        try {
            log.debug("Status code  : {}", response.getStatusCode());
            log.debug("Status text  : {}", response.getStatusText());
            log.debug("Headers      : {}", response.getHeaders());
            log.debug("Response body: {}", StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            log.debug("IOException : Cannot read response");
        }
        log.debug("==========================response end============================");
    }

    /**
     * Sanitize request URI, if request contain email address.
     *
     * @param request URI.
     * @return URI decoded and sanitized.
     */
    private String sanitizeRequest(URI request) {
        String decodedRequest = URLDecoder.decode(request.toString(), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(decodedRequest);

        StringBuilder sanitizedUrl = new StringBuilder();
        while (matcher.find()) {
            String firstPart = sanitize(matcher.group(1));
            String endPart = sanitize(matcher.group(2));

            String sanitized = firstPart + "@" + endPart;

            matcher.appendReplacement(sanitizedUrl, sanitized);
        }
        matcher.appendTail(sanitizedUrl);

        return sanitizedUrl.toString();
    }

    /**
     * Sanitize headers.
     *
     * @param headers request or response headers.
     * @return sanitized headers as String
     */
    private String sanitizeHeaders(HttpHeaders headers) {
        return headers.headerSet().stream()
                .map(entry ->
                    entry.getKey() + ":" + getValues(entry.getValue(), HEADERS_TO_SANITIZE.contains(entry.getKey()))
                )
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String getValues(List<String> values, boolean sanitize) {
        if(sanitize) {
            return values.size() == 1 ?
                    "\"" + sanitize(values.get(0)) + "\"" :
                    values.stream().map(s -> "\"" + sanitize(s) + "\"").collect(Collectors.joining(", "));
        } else {
            return values.size() == 1 ?
                    "\"" + values.get(0) + "\"" :
                    values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
        }
    }

    private String sanitize(String value) {
        if(value.length() <= 4) {
            return "****";
        }
        return value.charAt(0) +
                value.substring(1, value.length() - 2).replaceAll("[^ ]", "*") +
                value.charAt(value.length() - 1);
    }

}
