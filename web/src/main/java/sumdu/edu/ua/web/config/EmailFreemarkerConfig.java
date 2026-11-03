package sumdu.edu.ua.web.config;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.util.Locale;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class EmailFreemarkerConfig {
  @Bean("emailFreemarkerConfiguration")
  public Configuration emailFreemarkerConfiguration(
      @Value("${catalog.clock-zone}") String zone) {
    Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
    cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/mail-templates/");
    cfg.setDefaultEncoding("UTF-8");
    cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
    cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setWrapUncheckedExceptions(true);
    cfg.setLocale(Locale.forLanguageTag("uk-UA"));
    cfg.setTimeZone(TimeZone.getTimeZone(zone));
    cfg.setNumberFormat("computer");
    return cfg;
  }
}
