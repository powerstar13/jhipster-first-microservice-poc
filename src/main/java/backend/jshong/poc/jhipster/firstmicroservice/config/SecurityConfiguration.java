package backend.jshong.poc.jhipster.firstmicroservice.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import backend.jshong.poc.jhipster.firstmicroservice.security.AuthoritiesConstants;
import backend.jshong.poc.jhipster.firstmicroservice.security.SecurityUtils;
import backend.jshong.poc.jhipster.firstmicroservice.security.oauth2.AudienceValidator;
import backend.jshong.poc.jhipster.firstmicroservice.web.filter.SpaWebFilter;
import java.util.Collection;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@Slf4j
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .addFilterAfter(new SpaWebFilter(), BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers(mvc.pattern("/index.html"), mvc.pattern("/*.js"), mvc.pattern("/*.txt"), mvc.pattern("/*.json"), mvc.pattern("/*.map"), mvc.pattern("/*.css")).permitAll()
                    .requestMatchers(mvc.pattern("/*.ico"), mvc.pattern("/*.png"), mvc.pattern("/*.svg"), mvc.pattern("/*.webapp")).permitAll()
                    .requestMatchers(mvc.pattern("/app/**")).permitAll()
                    .requestMatchers(mvc.pattern("/i18n/**")).permitAll()
                    .requestMatchers(mvc.pattern("/content/**")).permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                    .requestMatchers(mvc.pattern("/api/authenticate")).permitAll()
                    .requestMatchers(mvc.pattern("/api/auth-info")).permitAll()
                    .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/api/**")).access(this::isInternalRequest)
                    .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/management/health")).permitAll()
                    .requestMatchers(mvc.pattern("/management/health/**")).permitAll()
                    .requestMatchers(mvc.pattern("/management/info")).permitAll()
                    .requestMatchers(mvc.pattern("/management/prometheus")).permitAll()
                    .requestMatchers(mvc.pattern("/management/**")).hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())))
            .oauth2Client(withDefaults());
        return http.build();
    }

    private AuthorizationDecision isInternalRequest(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext requestAuthorizationContext) {
        String remoteAddr = requestAuthorizationContext.getRequest().getRemoteAddr();
        log.info("Remote address: {}", remoteAddr);
        boolean isInternal = remoteAddr.startsWith("192.168.") || remoteAddr.startsWith("10.") || remoteAddr.startsWith("127.0.") || remoteAddr.equals("0:0:0:0:0:0:0:1");
        return new AuthorizationDecision(isInternal);
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new Converter<Jwt, Collection<GrantedAuthority>>() {
                @Override
                public Collection<GrantedAuthority> convert(Jwt jwt) {
                    return SecurityUtils.extractAuthorityFromClaims(jwt.getClaims());
                }
            }
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
