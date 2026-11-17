package aresaml.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the React frontend to call this API from the browser without being
 * blocked by CORS.
 *
 * The allowed origins come from the "app.cors.origins" property (comma-separated).
 * It defaults to the local Vite/CRA dev ports, so local development just works.
 * Once the frontend is deployed, set app.cors.origins (e.g. as an env variable on
 * Render) to include your live frontend URL, for example:
 *   app.cors.origins=http://localhost:5173,https://aresaml-dashboard.vercel.app
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
