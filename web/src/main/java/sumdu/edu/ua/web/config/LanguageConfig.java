package sumdu.edu.ua.web.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class LanguageConfig implements WebMvcConfigurer {
  private static final List<Locale> LANGUAGES = List.of(
      Locale.forLanguageTag("uk"), Locale.ENGLISH, Locale.FRENCH);

  @Bean
  public LocaleResolver localeResolver() {
    var resolver = new CookieLocaleResolver("lang");
    resolver.setCookieMaxAge(Duration.ofDays(30));
    // Explicit cookie wins; otherwise negotiate Accept-Language, then Ukrainian.
    resolver.setDefaultLocaleFunction(request -> {
      var requested = request.getHeader("Accept-Language");
      if (requested != null) {
        try {
          var match = Locale.lookup(Locale.LanguageRange.parse(requested), LANGUAGES);
          if (match != null) return match;
        } catch (IllegalArgumentException ignored) { /* Use the default below. */ }
      }
      return LANGUAGES.get(0);
    });
    return resolver;
  }

  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    var interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    interceptor.setIgnoreInvalidLocale(true);
    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}
