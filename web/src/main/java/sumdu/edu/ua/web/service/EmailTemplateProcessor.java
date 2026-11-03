package sumdu.edu.ua.web.service;

import freemarker.template.Configuration;
import java.io.StringWriter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateProcessor {
  private final Configuration cfg;

  public EmailTemplateProcessor(
      @Qualifier("emailFreemarkerConfiguration") Configuration cfg) {
    this.cfg = cfg;
  }

  public String process(String templateName, Map<String, Object> model) {
    try {
      var template = cfg.getTemplate(templateName);
      var writer = new StringWriter();
      template.process(model, writer);
      return writer.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Cannot render email template " + templateName, e);
    }
  }
}
